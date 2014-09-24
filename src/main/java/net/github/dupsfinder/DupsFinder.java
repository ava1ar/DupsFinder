package net.github.dupsfinder;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DupsFinder {

	public static void main(String[] args) {
		// setting root directory from command line parameter using current dir if not provided
		String rootDir = (args.length > 0) ? args[0] : "";
		// getting processors count from the system to use as threads count
		int coresCount = Runtime.getRuntime().availableProcessors();
		// start processing of rootDir in coresCount number of threads
		DupsFinder.withThreadsCount(coresCount).process(rootDir);
	}

	private final int threadsCount;
	private int filesCount;

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
		final Path rootDirectory = Paths.get(rootDir);
		try {
			System.err.format("Searching for duplicates in the \"%s\" directory.\n", rootDirectory.toRealPath());
		} catch (IOException ex) {
			System.err.println("WARN " + ex);
		}

		// grouping all files in the rootDir by file size using multiple threads
		final Collection<Set<FileEntry>> groupedBySizeFilesList = ParallelFileTreeWalker.listFiles(rootDirectory);
		// grouping files with identical size by partial hashsum (SHA-1) using multiple threads
		final Collection groupByPartialHashsum = groupByHashsum(threadsCount, false, groupedBySizeFilesList);
		// grouping files with identical partial hashsum by complete file hashsum (SHA-1) using multiple threads
		final Collection groupByCompleteHashsum = groupByHashsum(threadsCount, true, groupByPartialHashsum);
		// print diplicates from groupedByHashsum and total summary
		printResults(groupByCompleteHashsum);
	}

	/**
	 * Method takes collection of file groups with identical size, looks for groups with two and more files and returns
	 * collection of file groups with identical hash sum
	 *
	 * @param groupedFiles       collection of files groups with identical size
	 * @param threadCount        number of threads to execute method
	 * @param usePartialChecksum if true, use partial checksum instead of whole file checksum
	 * @return collection of files groups with identical checksum (partial or complete)
	 */
	private Collection groupByHashsum(final int threadCount, final boolean useCompleteFileChecksum, Collection<Set<FileEntry>> groupedFiles) {
		final Map<String, Set<FileEntry>> groupedFilesMap = new HashMap<>();
		final ExecutorService executor = Executors.newFixedThreadPool(threadCount);

		for (Set<FileEntry> files : groupedFiles) {
			final int groupSize = files.size();
			if (groupSize > 1) {
				for (final FileEntry fileEntry : files) {
					executor.execute(new Runnable() {

						@Override
						public void run() {
							final String hashSum = useCompleteFileChecksum ? fileEntry.getHashSum() : fileEntry.getPartialHashSum();
							// empty hashsum = hashsum calculation failed, so skipping such entries
							if (!hashSum.isEmpty()) {
								synchronized (groupedFilesMap) {
									Set<FileEntry> dups = groupedFilesMap.get(hashSum);
									if (dups == null) {
										dups = new HashSet<>();
										groupedFilesMap.put(hashSum, dups);
									}
									dups.add(fileEntry);
								}
							}
						}
					});
				}
			}
			// calculate total files count, but only during the first pass
			if (!useCompleteFileChecksum) {
				filesCount += groupSize;
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
	private void printResults(Collection<Set<FileEntry>> groupedFiles) {
		// calculate statistics and prepare output
		int duplicatesCount = 0;
		long wastedSpace = 0;
		final StringBuilder sb = new StringBuilder();

		// iterate through all groups of duplicate files
		for (Set<FileEntry> dupFiles : groupedFiles) {
			// number of duplicate files in group
			final int groupDuplicateFilesCount = dupFiles.size();
			if (groupDuplicateFilesCount > 1) {
				long fileSize = -1;
				for (FileEntry fileEntry : dupFiles) {
					if (fileSize == -1) {
						fileSize = fileEntry.getSize();
					}
					// append duplicates files list
					sb.append(fileEntry.setDupsCount(groupDuplicateFilesCount)).append('\n');
				}
				// update counters for duplicate files and wasted space
				duplicatesCount += groupDuplicateFilesCount;
				wastedSpace += (groupDuplicateFilesCount - 1) * fileSize;
			}
		}
		// print output and summary with statistics
		System.out.print(sb);
		System.err.format("Examined %s files, found %s dups, total wasted space %s\n",
				filesCount, duplicatesCount, printBytesCount(wastedSpace, false));
	}

	/**
	 * Converts size in bytes to the human-readable form
	 *
	 * @param bytes input size in bytes
	 * @param useSI if true, use SI (1000-based), otherwise use traditional (1024-based)
	 * @return String with human-readable size value
	 */
	private String printBytesCount(long bytes, boolean useSI) {
		final int unit = useSI ? 1000 : 1024;
		if (bytes < unit) {
			return bytes + " B";
		}
		final int exp = (int) (Math.log(bytes) / Math.log(unit));
		final String pre = (useSI ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (useSI ? "" : "i");
		return String.format("%.2f %sB", bytes / Math.pow(unit, exp), pre);
	}
}
