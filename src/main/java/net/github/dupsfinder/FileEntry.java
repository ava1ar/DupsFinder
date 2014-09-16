package net.github.dupsfinder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Required file info class
 *
 * @author ava1ar
 */
public class FileEntry {

	// stored initially while isntance creation
	private final String path;
	private final Long size;
	// calculated on demand
	private String partialHashSum;
	private String hashSum;

	private FileEntry(Path path) throws IOException {
		this.path = path.toString();
		this.size = Files.size(path);
	}

	//static builder
	static FileEntry of(Path path) throws IOException {
		return new FileEntry(path);
	}
	
	/**
	 * @return file absolute path
	 */
	public String getPath() {
		return path;
	}

	/**
	 * @return file size
	 */
	public long getSize() {
		return size;
	}

	/**
	 * @param firstBytesCount
	 * @return first firstBytesCount bytes of file hash sum
	 * @throws net.github.dupsfinder.HashsumCalculationException
	 */
	public String getPartialHashSum(final int firstBytesCount) throws HashsumCalculationException {
		if (partialHashSum == null) {
			partialHashSum = Hashsum.getSha1sum(Paths.get(path), firstBytesCount);
		}
		// optimization: if file size <= firstBytesCount, partialHashSum = HashSum
		if (size <= firstBytesCount) {
			hashSum = partialHashSum;
		}
		return partialHashSum;
	}

	/**
	 * @return file hash sum
	 * @throws net.github.dupsfinder.HashsumCalculationException
	 */
	public String getHashSum() throws HashsumCalculationException {
		if (hashSum == null) {
			hashSum = Hashsum.getSha1sum(Paths.get(path));
		}
		return hashSum;
	}
}
