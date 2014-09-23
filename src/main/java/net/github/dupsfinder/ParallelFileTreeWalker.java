package net.github.dupsfinder;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

import static java.nio.file.FileVisitResult.CONTINUE;

/**
 *
 * @author ava1ar
 */
public class ParallelFileTreeWalker {

	private static class RecursiveWalker extends RecursiveTask<List<FileEntry>> {

		private final Path dir;

		private RecursiveWalker(Path dir) {
			this.dir = dir;
		}

		@Override
		protected List<FileEntry> compute() {
			final List<RecursiveWalker> walkers = new ArrayList<>();
			final List<FileEntry> fileEntries = new ArrayList<>();
			try {
				Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {

					@Override
					public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
						if (dir.equals(RecursiveWalker.this.dir)) {
							return FileVisitResult.CONTINUE;
						} else {
							RecursiveWalker walker = new RecursiveWalker(dir);
							walker.fork();
							walkers.add(walker);
							return FileVisitResult.SKIP_SUBTREE;
						}
					}

					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
						if (attrs.isRegularFile()) {
							fileEntries.add(FileEntry.of(file));
						}
						return CONTINUE;
					}

					@Override
					public FileVisitResult visitFileFailed(Path file, IOException ex) {
						System.err.println("WARN " + ex);
						return CONTINUE;
					}
				});
			} catch (IOException ex) {
				System.err.println("WARN " + ex);
			}

			// join results of parallel executions
			walkers.stream().forEach((task) -> {
				fileEntries.addAll(task.join());
			});
			return fileEntries;
		}
	}

	static List<FileEntry> listFiles(Path rootDir) {
		return new ForkJoinPool().invoke(new RecursiveWalker(rootDir));
	}
}
