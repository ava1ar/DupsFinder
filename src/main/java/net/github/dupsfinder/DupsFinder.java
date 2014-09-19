package net.github.dupsfinder;

import java.io.IOException;
import java.nio.file.FileVisitResult;

import static java.nio.file.FileVisitResult.CONTINUE;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
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
		try {
			final Path rootDirectory = Paths.get(rootDir);
			System.err.format("Searching for duplicates in the \"%s\" directory.\n", rootDirectory.toRealPath());

			// list of all files to be compared
			final List<FileEntry> completeFilesList = getFilesList(rootDirectory);

			// prepare duplicateFiles list
			List<List<FileEntry>> duplicateFilesList = completeFilesList.parallelStream()
					.collect(Collectors.groupingBy(FileEntry::getSize)).values().parallelStream()
					// drop groups with single element and group by file partial hashsum
					.filter(p -> p.size() > 1)
					.flatMap(p -> p.parallelStream())
					.collect(Collectors.groupingByConcurrent(FileEntry::getPartialHashSum)).values().parallelStream()
					// drop groups with single element and group by file hashsum
					.filter(p -> p.size() > 1)
					.flatMap(p -> p.parallelStream())
					// filter out files entries with enpty partial hashsum (hashsum calculation failed)
					.filter(p -> !p.getPartialHashSum().isEmpty())
					.collect(Collectors.groupingByConcurrent(FileEntry::getHashSum)).values().parallelStream()
					// drop groups with single element
					.filter(p -> p.size() > 1)
					.collect(Collectors.toList());

			// calculate statistics and prepare output
			int duplicatesCount = 0;
			long wastedSpace = 0;
			final StringBuilder sb = new StringBuilder();

			for (List<FileEntry> files : duplicateFilesList) {
				final int dupsCount = files.size();
				long size = 0;
				for (FileEntry fileEntry : files) {
					size = fileEntry.getSize();
					sb.append(fileEntry.setDupsCount(dupsCount)).append('\n');
				}
				// update counters
				duplicatesCount += dupsCount;
				wastedSpace += (dupsCount - 1) * size;
			}

			// print output and summary with statistics
			System.out.print(sb);
			System.err.format("Examined %s files, found %s dups, total wasted space %s\n",
					completeFilesList.size(), duplicatesCount, printBytesCount(wastedSpace, false));
		} catch (IOException ex) {
			System.err.println("WARN " + ex);
		}
	}

	/**
	 * Method returns list of all files to be compared, recursively walking inside specified path
	 *
	 * @param path of the root directory to start
	 * @return list of files to be compared
	 * @throws IOException
	 */
	private List<FileEntry> getFilesList(Path path) throws IOException {
		final List<FileEntry> fileEntries = new ArrayList<>();

		Files.walkFileTree(path, new SimpleFileVisitor<Path>() {

			@Override
			public FileVisitResult visitFile(final Path path, final BasicFileAttributes attrs) {
				// only working with regular files
				if (attrs.isRegularFile()) {
					try {
						fileEntries.add(FileEntry.of(path));
					} catch (IOException ex) {
						System.err.println("WARN " + ex);
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

		return fileEntries;
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
