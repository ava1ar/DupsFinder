package net.github.dupsfinder;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

import static java.nio.file.FileVisitResult.CONTINUE;

/**
 *
 * @author ava1ar
 */
public class ParallelFileTreeWalker {

	private static class RecursiveWalker extends RecursiveTask<Map<Long, Set<FileEntry>>> {

		private final Path dir;

		private RecursiveWalker(Path dir) {
			this.dir = dir;
		}

		@Override
		protected Map<Long, Set<FileEntry>> compute() {
			final List<RecursiveWalker> walkers = new ArrayList<>();
			final Map<Long, Set<FileEntry>> groupedFilesMap = new HashMap<>();
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
							FileEntry fileEntry = FileEntry.of(file);
							final long size = fileEntry.getSize();
							// size = -1 means getting file size failed, so we can skip this file
							if (size >= 0) {
								Set<FileEntry> dups = groupedFilesMap.get(size);
								if (dups == null) {
									dups = new HashSet<>();
									groupedFilesMap.put(size, dups);
								}
								dups.add(fileEntry);
							}
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
			for (RecursiveWalker walker : walkers) {
				mergeInto(groupedFilesMap, walker.join());
			}
			return groupedFilesMap;
		}
	}

	// merge map2 into map1
	private static void mergeInto(Map<Long, Set<FileEntry>> map1, Map<Long, Set<FileEntry>> map2) {
		// merge items which exist in both map1 and map2
		for (Map.Entry<Long, Set<FileEntry>> entry : map1.entrySet()) {
			Set<FileEntry> dups = map2.remove(entry.getKey());
			if (dups != null) {
				entry.getValue().addAll(dups);
			}
		}
		// add items, which exist only in map2
		map1.putAll(map2);
	}

	static Collection<Set<FileEntry>> listFiles(Path rootDir) {
		return new ForkJoinPool().invoke(new RecursiveWalker(rootDir)).values();
	}
}
