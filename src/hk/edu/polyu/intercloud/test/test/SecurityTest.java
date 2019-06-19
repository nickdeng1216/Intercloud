package hk.edu.polyu.intercloud.test.test;

import hk.edu.polyu.intercloud.security.Decryption;
import hk.edu.polyu.intercloud.security.Digest;
import hk.edu.polyu.intercloud.security.Encryption;

import java.io.File;
import java.nio.file.Files;

import javax.crypto.Cipher;

import org.apache.commons.io.FilenameUtils;

public class SecurityTest {
	public static void main(String[] args) {

		// GetKey password = First 16 character of hex format of
		// (SHA256(privatekey + filename ));
		try {

			System.out.println("answer  is"
					+ Encryption.makeKey(System.getProperty("user.dir")
							+ "/Key/Cloud3.PKEY", "2.jpg"));

		} catch (Exception e) {

		}

		// Encrypt
		try {
			String fileName = System.getProperty("user.dir") + "/encrypt/2.jpg";
			String tempFileName = fileName + ".enc";
			String keypath = System.getProperty("user.dir")
					+ "/Key/Cloud3.PKEY";

			File file = new File(fileName);
			if (!file.exists()) {
				System.out.println("No file " + fileName);
				return;
			}
			File file2 = new File(tempFileName);
			Files.deleteIfExists(file2.toPath());

			Encryption.copy(Cipher.ENCRYPT_MODE, fileName, tempFileName,
					keypath);

			System.out
					.println("Success. Find encrypted and decripted files in current directory");
		} catch (Exception e) {

		}

		// Decrypt
		try {

			String inputFile = System.getProperty("user.dir")
					+ "/encrypt/2.jpg.enc";
			String keypath = System.getProperty("user.dir")
					+ "/Key/Cloud3.PKEY";

			String name_enc = FilenameUtils.getName(inputFile);
			String name = FilenameUtils.removeExtension(name_enc);

			String outFile = System.getProperty("user.dir") + "/decrypt/"
					+ name;
			System.out.print(outFile);

			File file = new File(inputFile);

			if (!file.exists()) {
				System.out.println("No file " + inputFile);
				return;
			}

			File file1 = new File(outFile);
			Files.deleteIfExists(file1.toPath());

			Decryption.copy(Cipher.DECRYPT_MODE, inputFile, outFile, keypath);

			System.out
					.println("Success. Find decripted file in current directory");
		} catch (Exception e) {

		}

		// GetDigest in HEX
		try {

			System.out.println(Digest.digestFile(System.getProperty("user.dir")
					+ "/encrypt/2.jpg"));
		} catch (Exception e) {

		}

	}
}
