package hk.edu.polyu.intercloud.security;

import hk.edu.polyu.intercloud.exceptions.SecurityException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.io.FilenameUtils;

/**
 * 
 * The class is used in AES encryption. The interface is the copy method. In
 * makekey, password = First 16 character of hex format of (SHA256(privatekey +
 * filename ));
 * 
 * @author Kate.xie
 */
public class Encryption {

	private static final int IV_LENGTH = 16;

	public static byte[] encrypt(String plainText, String password)
			throws IOException, SecurityException {
		ByteArrayInputStream bis = new ByteArrayInputStream(
				plainText.getBytes("UTF8"));
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		encrypt(bis, bos, password);
		return bos.toByteArray();
	}

	public static void encrypt(InputStream in, OutputStream out, String password)
			throws IOException, SecurityException {

		SecureRandom r = new SecureRandom();
		byte[] iv = new byte[IV_LENGTH];
		r.nextBytes(iv);
		out.write(iv); // write IV as a prefix
		out.flush();

		try {
			Cipher cipher = Cipher.getInstance("AES/CFB8/NoPadding"); // "DES/ECB/PKCS5Padding";"AES/CBC/PKCS5Padding"

			SecretKeySpec keySpec = new SecretKeySpec(password.getBytes(),
					"AES");
			IvParameterSpec ivSpec = new IvParameterSpec(iv);
			cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
			out = new CipherOutputStream(out, cipher);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException
				| InvalidKeyException | InvalidAlgorithmParameterException e) {
			throw new SecurityException(e.getMessage(), e);
		}

		byte[] buf = new byte[1024];
		int numRead = 0;
		while ((numRead = in.read(buf)) >= 0) {
			out.write(buf, 0, numRead);
		}
		out.close();

	}

	public static void copy(int mode, String inputFile, String outputFile,
			String keypath) throws IOException, SecurityException {
		BufferedInputStream is = new BufferedInputStream(new FileInputStream(
				inputFile));
		BufferedOutputStream os = new BufferedOutputStream(
				new FileOutputStream(outputFile));
		String name = FilenameUtils.getName(inputFile);
		String password = makeKey(keypath, name);
		// System.out.println(name + "encrypt password is" + password);
		encrypt(is, os, password);
		is.close();
		os.close();
	}

	public static String makeKey(String keypath, String filename)
			throws IOException, SecurityException {
		String part1 = Encryption.readFile(keypath);
		String part2 = filename;
		String whole = part1 + part2;
		String digest = null;
		digest = Digest.digestString(whole);
		int padding = 0;
		if (digest.length() < 16) {
			padding = 16 - digest.length();
			String appex = "0";
			for (int i = 0; i < padding - 1; i++)
				appex += "0";
			digest += appex;
		}
		return digest.substring(0, 16);
	}

	public static String makeKeyShared() throws IOException {
		Random rand = new Random();
		StringBuilder password = new StringBuilder();
		for (int i = 0; i < 16; i++) {
			password.append(rand.nextInt(9));
		}
		return password.toString();
	}

	static String readFile(String path) throws IOException {

		File inputFile = new File(path);
		StringBuffer result = new StringBuffer();
		BufferedReader reader = new BufferedReader(new FileReader(inputFile));
		String lineToRemove = "-----BEGIN RSA PRIVATE KEY-----";
		String lineToRemove_1 = "-----END RSA PRIVATE KEY-----";
		String currentLine;

		while ((currentLine = reader.readLine()) != null) {
			// trim newline when comparing with lineToRemove
			String trimmedLine = currentLine.trim();
			if (trimmedLine.equals(lineToRemove))
				System.out.print("-");
			else if (trimmedLine.equals(lineToRemove_1))
				System.out.print("-");
			else
				result.append(currentLine);
		}

		reader.close();
		return result.toString();
	}

}
