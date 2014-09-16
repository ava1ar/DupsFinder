package net.github.dupsfinder;

import java.io.IOException;
import java.nio.file.FileVisitResult;

import static java.nio.file.FileVisitResult.CONTINUE;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DupsFinder {

	private static final int PARTIAL_CHECKSUM_BYTES = 1024;

	public static void main(String[] args) {
		// setting root directory from command line parameter using current dir if not provided
		String rootDir = (args.length > 0) ? args[0] : "";
		// getting processors count from the system to use as threads count
		int coresCount = Runtime.getRuntime().availableProcessors();
		// start processing of rootDir in coresCount number of threads
		DupsFinder.withThreadsCount(coresCount).process(rootDir);
	}

	private final int threadsCount;
	private int filesCount = 0;

	private DupsFinder(int threadsCount) {
		this.threadsCount = threadsCount;
	}

	// static builder
	static DupsFinder withThreadsCount(int threadsCount) {
		return new DupsFinder(threadsCount);
	}

	/**
	 * Process root directory for duplicates
	 *
	 * @param rootDir directory to looks for a duplicate files
	 */
	public void process(final String rootDir) {
		try {
			final Path rootDirectory = Paths.get(rootDir);
			System.out.println("Searching for duplicates in the \"" + rootDirectory.toRealPath() + "\" directory...");

			// grouping by file size
			final Collection groupedByFileSize = groupBySize(rootDirectory);
			// grouping files with identical size by partial hashsum (SHA-1) using multiple threads
			final Collection groupedByPartialHashsum = groupByHashsum(groupedByFileSize, threadsCount, true);
			// grouping files with identical partial hashsum by complete hashsum (SHA-1) using multiple threads
			final Collection groupedByHashsum = groupByHashsum(groupedByPartialHashsum, threadsCount, false);

			// print diplicates from groupedByHashsum and total summary
			printResults(groupedByHashsum);
		} catch (IOException | HashsumCalculationException ex) {
			System.err.println("WARN " + ex);
		}
	}

	/**
	 * Method takes directory and groups all files and subdirectories from it recursively into groups with same file
	 * size
	 *
	 * @param path of the root directory to start
	 * @return collection of files groups with identical size
	 * @throws IOException
	 */
	private Collection groupBySize(Path root) throws IOException {
		final Map<Long, Set<FileEntry>> groupedFilesMap = new HashMap<>();

		// walk over given folder, grouping files with the same size
		Files.walkFileTree(root, new SimpleFileVisitor<Path>() {

			@Override
			public FileVisitResult visitFile(final Path path, final BasicFileAttributes attrs) throws IOException {
				// only working with regular files
				if (attrs.isRegularFile()) {
					filesCount++;
					final FileEntry fileEntry = FileEntry.of(path);
					final long fileSize = fileEntry.getSize();
					Set<FileEntry> dups = groupedFilesMap.get(fileSize);
					if (dups == null) {
						dups = new HashSet<>();
						groupedFilesMap.put(fileSize, dups);
					}
					dups.add(fileEntry);
				}
				return CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException ex) {
				System.err.println("WARN " + ex);
				return CONTINUE;
			}
		}
		);
		return groupedFilesMap.values();
	}

	/**
	 * Method takes collection of file groups with identical size, looks for groups with two and more files and returns
	 * collection of file groups with identical hash sum
	 *
	 * @param groupedFiles collection of files groups with identical size
	 * @param threadCount number of threads to execute method
	 * @param usePartialChecksum if true, use partial checksum instead of whole file checksum
	 * @return collection of files groups with identical checksum (partial or complete)
	 */
	private Collection groupByHashsum(Collection<Set<FileEntry>> groupedFiles, final int threadCount, final boolean usePartialChecksum) {
		final Map<String, Set<FileEntry>> groupedFilesMap = new HashMap<>();
		final ExecutorService executor = Executors.newFixedThreadPool(threadCount);

		for (Set<FileEntry> files : groupedFiles) {
			if (files.size() > 1) {
				for (final FileEntry fileEntry : files) {
					executor.execute(new Runnable() {

						@Override
						public void run() {
							try {
								final String hashSum = usePartialChecksum
										? fileEntry.getPartialHashSum(PARTIAL_CHECKSUM_BYTES)
										: fileEntry.getHashSum();
								synchronized (groupedFilesMap) {
									Set<FileEntry> dups = groupedFilesMap.get(hashSum);
									if (dups == null) {
										dups = new HashSet<>();
										groupedFilesMap.put(hashSum, dups);
									}
									dups.add(fileEntry);
								}
							} catch (HashsumCalculationException ex) {
								System.err.println("WARN " + ex);
							}
						}

					});
				}
			}
		}

		// shutdown executor and wait all threads to finish
		executor.shutdown();
		while (!executor.isTerminated()) {
			// wait for all threads to finish
		}
		return groupedFilesMap.values();
	}

	/**
	 * Method takes collection of identical files groups and prints info for groups with two or more items in it
	 *
	 * @param groupedFilesMap collection of identical files groups
	 * @throws HashsumCalculationException
	 */
	private void printResults(Collection<Set<FileEntry>> groupedFiles) throws HashsumCalculationException {
		int duplicatesCount = 0;
		long wastedSpace = 0;
		final StringBuilder sb = new StringBuilder();

		for (Set<FileEntry> files : groupedFiles) {
			final int dupsForGroupCount = files.size();
			if (dupsForGroupCount > 1) {
				long size = 0;
				for (FileEntry entry : files) {
					size = entry.getSize();
					sb.append(entry.getHashSum()).append(':')
							.append(dupsForGroupCount).append(':')
							.append(size).append(":\"")
							.append(entry.getPath()).append("\"").append('\n');
				}
				// update counters
				duplicatesCount += dupsForGroupCount;
				wastedSpace += (dupsForGroupCount - 1) * size;
			}
		}
		// add summary part
		final String summaryStr = String.format("Examined %s files, found %s dups, total wasted space %s",
				filesCount, duplicatesCount, humanReadableByteCount(wastedSpace, false));
		sb.append(summaryStr).append('\n');
		// print output
		System.out.println(sb);
	}

	/**
	 * Converts size in bytes to the human-readable form
	 *
	 * @param bytes input size in bytes
	 * @param si if true, use SI, otherwise use historical
	 * @return human-readable size value
	 */
	private String humanReadableByteCount(long bytes, boolean si) {
		final int unit = si ? 1000 : 1024;
		if (bytes < unit) {
			return bytes + " B";
		}
		final int exp = (int) (Math.log(bytes) / Math.log(unit));
		final String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
		return String.format("%.2f %sB", bytes / Math.pow(unit, exp), pre);
	}
}
