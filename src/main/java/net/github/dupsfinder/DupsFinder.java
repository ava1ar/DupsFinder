package net.github.dupsfinder;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class DupsFinder {

	public static void main(String[] args) {
		// start processing of root directory from command line parameter using current dir if not provided
		new DupsFinder().process((args.length > 0) ? args[0] : "");
	}

	/**
	 * Process root directory for duplicates
	 *
	 * @param rootDir directory to look for duplicate files
	 */
	public void process(final String rootDir) {
		final Path rootDirectory = Paths.get(rootDir);
		try {
			System.err.format("Searching for duplicates in the \"%s\" directory.\n", rootDirectory.toRealPath());
		} catch (IOException ex) {
			System.err.println("WARN " + ex);
		}
		// list of all files to be compared
		// TODO: after https://bugs.openjdk.java.net/browse/JDK-8039910 is fixed,  Files.walk stream can be used as a source
		final List<FileEntry> completeFilesList = ParallelFileTreeWalker.listFiles(rootDirectory);

		// process list of files to get the list of lists of files with identical hashsums
		List<List<FileEntry>> duplicateFilesList = completeFilesList.parallelStream()
				// group files by size
				.collect(Collectors.groupingByConcurrent(FileEntry::getSize)).values().parallelStream()
				// keep only groups with > 1 element
				.filter(p -> p.size() > 1)
				.flatMap(p -> p.parallelStream())
				// filter out files with size = -1 (this means that file size getting failed)
				.filter(p -> p.getSize() >= 0)
				// group remaining files by partial hashsum
				.collect(Collectors.groupingByConcurrent(FileEntry::getPartialHashSum)).values().parallelStream()
				// keep only groups with > 1 element
				.filter(p -> p.size() > 1)
				.flatMap(p -> p.parallelStream())
				// filter out files entries with empty partial hashsum (this means that hashsum calculation failed)
				.filter(p -> !p.getPartialHashSum().isEmpty())
				// group files by file hashsum
				.collect(Collectors.groupingByConcurrent(FileEntry::getHashSum)).values().parallelStream()
				// keep only groups with > 1 element
				.filter(p -> p.size() > 1)
				// store results to the list
				.collect(Collectors.toList());
		// calculate summary and print results
		printResults(completeFilesList.size(), duplicateFilesList);
	}

	/**
	 * Calculates statistics and prints duplicate files list and summary
	 *
	 * @param totalCheckedFilesCount total number of checked files - used in summary output
	 * @param duplicateFilesList     list of list of duplicate files
	 */
	private void printResults(int totalCheckedFilesCount, List<List<FileEntry>> duplicateFilesList) {
		// calculate statistics and prepare output
		final StringBuilder sb = new StringBuilder();
		int duplicatesCount = 0;
		long wastedSpace = 0;
		// iterate through all groups of duplicate files
		for (List<FileEntry> duplicateFilesGroup : duplicateFilesList) {
			// number of duplicate files in group
			final int groupDuplicateFilesCount = duplicateFilesGroup.size();
			long fileSize = -1;
			for (FileEntry fileEntry : duplicateFilesGroup) {
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
		// print output and summary with statistics
		System.out.print(sb);
		System.err.format("Examined %s files, found %s dups, total wasted space %s\n",
				totalCheckedFilesCount, duplicatesCount, printBytesCount(wastedSpace, false));
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
