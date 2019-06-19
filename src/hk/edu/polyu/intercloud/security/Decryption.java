package hk.edu.polyu.intercloud.security;

import hk.edu.polyu.intercloud.exceptions.SecurityException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.io.FilenameUtils;

/**
 * The class decrypts cryptographic files.
 * 
 * @author Kate.xie
 */
public class Decryption {
	private static final int IV_LENGTH = 16;

	public static byte[] decrypt(byte[] cipher, String password)
			throws IOException, SecurityException {
		ByteArrayInputStream bis = new ByteArrayInputStream(cipher);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		decrypt(bis, bos, password);
		return bos.toByteArray();
	}

	public static void decrypt(InputStream in, OutputStream out, String password)
			throws IOException, SecurityException {

		byte[] iv = new byte[IV_LENGTH];
		in.read(iv);

		try {
			Cipher cipher = Cipher.getInstance("AES/CFB8/NoPadding"); // "DES/ECB/PKCS5Padding";"AES/CBC/PKCS5Padding"
			SecretKeySpec keySpec = new SecretKeySpec(password.getBytes(),
					"AES");
			IvParameterSpec ivSpec = new IvParameterSpec(iv);
			cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

			in = new CipherInputStream(in, cipher);
			byte[] buf = new byte[1024];
			int numRead = 0;
			while ((numRead = in.read(buf)) >= 0) {
				out.write(buf, 0, numRead);
			}
			out.close();
		} catch (NoSuchPaddingException | NoSuchAlgorithmException
				| InvalidKeyException | InvalidAlgorithmParameterException e) {
			throw new SecurityException(e.getMessage(), e);
		}
	}

	public static void copy(int mode, String inputFile, String outputFile,
			String keypath) throws IOException, SecurityException {

		BufferedInputStream is = new BufferedInputStream(new FileInputStream(
				inputFile));
		BufferedOutputStream os = new BufferedOutputStream(
				new FileOutputStream(outputFile));
		String name_enc = FilenameUtils.getName(inputFile);
		String name = FilenameUtils.removeExtension(name_enc);
		// XXX
		name = name_enc;

		String password = makeKey(keypath, name);
		decrypt(is, os, password);
		is.close();
		os.close();

	}

	static String makeKey(String keypath, String filename) throws IOException,
			SecurityException {
		String part1 = Encryption.readFile(keypath);
		String part2 = filename;
		String whole = part1 + part2;
		String digest = Digest.digestString(whole);
		return digest.substring(0, 16);
	}

	static String readFile(String path) throws IOException {
		int lines = 0;
		int flag = 0;

		StringBuilder result = new StringBuilder();
		BufferedReader reader = new BufferedReader(new FileReader(path));

		while (reader.readLine() != null)
			lines++;
		reader.close();
		System.out.print(lines);

		FileReader fr_2 = new FileReader(path);
		BufferedReader br_2 = new BufferedReader(fr_2);

		while (br_2.ready()) {
			flag++;
			if (flag == 1 || flag == lines)
				System.out.println("Delete the line:" + br_2.readLine());
			else
				result.append(br_2.readLine());
		}

		fr_2.close();
		br_2.close();

		return result.toString();
	}
}
