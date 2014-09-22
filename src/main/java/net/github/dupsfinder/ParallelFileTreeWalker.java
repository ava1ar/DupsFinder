/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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

	private static class RecursiveWalk extends RecursiveTask<List<FileEntry>> {

		private final Path dir;

		public RecursiveWalk(Path dir) {
			this.dir = dir;
		}

		@Override
		protected List<FileEntry> compute() {
			final List<RecursiveWalk> walkers = new ArrayList<>();
			final List<FileEntry> fileEntries = new ArrayList<>();
			try {
				Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {

					@Override
					public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
						if (attrs.isDirectory()) {
							if (dir.equals(RecursiveWalk.this.dir)) {
								return FileVisitResult.CONTINUE;
							} else {
								RecursiveWalk walk = new RecursiveWalk(dir);
								walk.fork();
								walkers.add(walk);
								return FileVisitResult.SKIP_SUBTREE;
							}
						} else {
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

	public static List<FileEntry> listFiles(Path rootDir) {
		return new ForkJoinPool().invoke(new RecursiveWalk(rootDir));
	}
}
