package net.github.dupsfinder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Required file parameters and properties lazy getter
 *
 * @author ava1ar
 */
public class FileEntry {

	// size of part to calculate partial checksum
	private static final int PARTIAL_CHECKSUM_BYTES = 1024;

	// stored initially while isntance creation
	private final Path path;
	// calculated on demand
	private long size = -1;
	private String partialHashSum;
	private String hashSum;
	// set from external
	private int dupsCount;

	private FileEntry(Path path) {
		this.path = path;
	}

	//static builder
	static FileEntry of(Path path) {
		return new FileEntry(path);
	}

	/**
	 * @return file absolute path
	 */
	public String getPath() {
		return path.toString();
	}

	/**
	 * @return file size
	 */
	public long getSize() {
		if (size >= 0) {
			return size;
		} else {
			try {
				size = Files.size(path);
			} catch (IOException ex) {
				System.err.println("WARN " + ex);
			}
		}
		return size;
	}

	/**
	 * @return first firstBytesCount bytes of file hash sum
	 */
	public String getPartialHashSum() {
		if (partialHashSum == null) {
			partialHashSum = Hashsum.getSHA1sum(path, PARTIAL_CHECKSUM_BYTES);
			/* optimization: if partialHashSum is empty (this means generation failed)
			 or if file size <= firstBytesCount, then hashSum = partialHashSum */
			if (partialHashSum.isEmpty() || getSize() <= PARTIAL_CHECKSUM_BYTES) {
				hashSum = partialHashSum;
			}
		}
		return partialHashSum;
	}

	/**
	 * @return file hash sum
	 */
	public String getHashSum() {
		if (hashSum == null) {
			hashSum = Hashsum.getSHA1sum(path);
		}
		return hashSum;
	}

	@Override
	public String toString() {
		return new StringBuilder(getHashSum()).append(':')
				.append(getDupsCount()).append(':')
				.append(getSize()).append(":")
				.append(getPath()).toString();
	}

	public StringBuilder builder() {
		return new StringBuilder(getHashSum()).append(':')
				.append(getDupsCount()).append(':')
				.append(getSize()).append(":")
				.append(getPath());
	}

	/**
	 *
	 * @return duplicates count
	 */
	public int getDupsCount() {
		return dupsCount;
	}

	/**
	 * Set file duplicates count
	 *
	 * @param dupsCount
	 * @return FileEntry instance with property set
	 */
	public FileEntry setDupsCount(int dupsCount) {
		this.dupsCount = dupsCount;
		return this;
	}
}
