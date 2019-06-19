package hk.edu.polyu.intercloud.security;

import hk.edu.polyu.intercloud.exceptions.SecurityException;
import hk.edu.polyu.intercloud.util.LogUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.io.FilenameUtils;

/**
 * The class digests file OR String.
 * 
 * @author Kate.Xie
 * 
 */
public class Digest {
	/**
	 * The method is used for digesting file.
	 * 
	 * @param path
	 * @return
	 * @throws IOException
	 * @throws SecurityException
	 * @throws Exception
	 */
	public static String digestFile(String path) throws IOException,
			SecurityException {
		try {
			File file = new File(path);
			if (!file.isFile()) {
				return "";
			}
			long startTime = System.currentTimeMillis();
			LogUtil.logPerformance("Digest START", FilenameUtils.getName(path),
					startTime, 0);
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			FileInputStream fis = new FileInputStream(path);
			byte[] dataBytes = new byte[1024];
			int nread = 0;
			while ((nread = fis.read(dataBytes)) != -1) {
				md.update(dataBytes, 0, nread);
			}
			byte[] mdbytes = md.digest();
			long endTime = System.currentTimeMillis();
			LogUtil.logPerformance("Digest END", FilenameUtils.getName(path),
					endTime, endTime - startTime);
			fis.close();
			return bytetohex(mdbytes);
		} catch (NoSuchAlgorithmException e) {
			throw new SecurityException(e.getMessage(), e);
		}
	}

	public static String digestString(String plaintest)
			throws SecurityException {
		// long startTime = System.currentTimeMillis();
		// LogUtil.logPerformance("Digest START", plaintest, startTime, 0);
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(plaintest.getBytes());
			byte[] mdbytes = md.digest();
			return bytetohex(mdbytes);
		} catch (NoSuchAlgorithmException e) {
			throw new SecurityException(e.getMessage(), e);
		}
		// long endTime = System.currentTimeMillis();
		// LogUtil.logPerformance("Digest END", plaintest, endTime, endTime
		// - startTime);
	}

	public static byte[] hextobyte(String hex) {
		int length = hex.length();
		byte[] data = new byte[length / 2];
		for (int i = 0; i < length; i += 2) {
			data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4) + Character
					.digit(hex.charAt(i + 1), 16));
		}
		return data;
	}

	public static String bytetohex(byte[] mdbytes) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < mdbytes.length; i++) {
			sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16)
					.substring(1));
		}
		return sb.toString();
	}

}
