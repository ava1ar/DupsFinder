package net.github.dupsfinder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.RecursiveTask;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;

/**
 * Recursive Task to walk the directory tree in multiple threads
 *
 * @author ava1ar
 */
public class DirectoryProcessor extends RecursiveTask<List<FileEntry>> {

	private final Path path;

	public DirectoryProcessor(Path path) {
		this.path = path;
	}

	@Override
	protected List<FileEntry> compute() {
		final List<FileEntry> files = new ArrayList<>();
		final List<DirectoryProcessor> tasks = new ArrayList<>();
		try {
			Iterator<Path> iter = Files.newDirectoryStream(path).iterator();
			while (iter.hasNext()) {
				Path entry = iter.next();
				if (Files.isDirectory(entry, NOFOLLOW_LINKS)) {
					DirectoryProcessor task = new DirectoryProcessor(entry);
					task.fork();
					tasks.add(task);
				} else if (Files.isRegularFile(entry, NOFOLLOW_LINKS)) {
					files.add(FileEntry.of(entry));
				}
			}
		} catch (IOException ex) {
			System.err.println("WARN " + ex);
		}
		tasks.stream().forEach((task) -> {
			files.addAll(task.join());
		});
		return files;
	}
}
