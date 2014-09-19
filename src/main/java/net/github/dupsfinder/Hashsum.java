package net.github.dupsfinder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Hashsum calculations utility class
 *
 * @author ava1ar
 */
public class Hashsum {

	// file read buffer size
	private static final int BUFFER_SIZE = 4096;

	private Hashsum() {
		// hidden constructor for the utility class
	}

	/**
	 * Calculated SHA-1 checksum form the file
	 *
	 * @param path path for file
	 * @return SHA-1 checksum of the file
	 */
	public static String getSHA1sum(Path path) {
		return getSHA1sum(path, 0);
	}

	/**
	 * Calculated SHA-1 checksum form the file
	 *
	 * @param path     path for file
	 * @param maxBytes calculate checksum only for first maxBytes bytes
	 * @return SHA-1 checksum of the file
	 */
	public static String getSHA1sum(Path path, int maxBytes) {
		return getHashSum(path, "SHA-1", maxBytes);
	}

	// calculate hashsum for path using algo 
	private static String getHashSum(Path path, String algo, int maxBytes) {
		StringBuilder sb = new StringBuilder("");
		try (InputStream is = Files.newInputStream(path)) {
			MessageDigest md = MessageDigest.getInstance(algo);
			DigestInputStream dis = new DigestInputStream(is, md);
			// if we want checksum for first maxBytes bytes only
			if (maxBytes > 0) {
				dis.read(new byte[maxBytes]);
				// else read whole file
			} else {
				byte[] buffer = new byte[BUFFER_SIZE];
				while (dis.read(buffer) != -1) {
					// read complete file
				}
			}
			// file checkum in byte array format
			byte[] mdbytes = md.digest();
			// convert the byte array to hex format
			for (int i = 0; i < mdbytes.length; i++) {
				sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
			}
		} catch (IOException | NoSuchAlgorithmException ex) {
			System.err.println("WARN " + ex);
		}
		return sb.toString();
	}
}
