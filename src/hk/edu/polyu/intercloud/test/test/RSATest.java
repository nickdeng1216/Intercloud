package hk.edu.polyu.intercloud.test.test;

import hk.edu.polyu.intercloud.security.Digest;
import hk.edu.polyu.intercloud.security.Encryption;
import hk.edu.polyu.intercloud.security.RSA;

/**
 * 
 * @author Kate.Xie
 *
 */

public class RSATest {

	public static void main(String[] args) throws Exception {
		String publicKey;
		String privateKey;

		// String keypath = System.getProperty("user.dir") + "/Key/private.pem";
		// byte b[] = Encryption.encrypt("wwaa学生都是sds哦无aa",
		// Encryption.makeKey(keypath, "kate"));
		// String cipher = new String(b, "ISO-8859-1");
		// byte de[] = Decryption.decrypt(cipher, Encryption.makeKey(keypath,
		// "kate"));
		// String decoded1 = new String(de, "UTF-8");
		// System.out.println("plaintest is " + decoded1);
		// makekey
		// Generatekeypair.generatepublickey();
		// Generatekeypair.generateprivatekey();
		publicKey = RSA.readfile(System.getProperty("user.dir") + "/Key/public.pem");
		privateKey = RSA.readfile(System.getProperty("user.dir") + "/Key/private.pem");

		// encrypt
		System.out.println("EN(private)---DE(public)");

		String inputStr = Encryption.makeKey(System.getProperty("user.dir") + "/Key/private.pem", "kate");

		byte[] data = inputStr.getBytes();
		System.out.println(inputStr);

		byte[] encodedData = RSA.encryptByPrivateKey(data, privateKey);
		String cipher = Digest.bytetohex(encodedData);
		byte[] decodedData1 = RSA.decryptByPublicKey(Digest.hextobyte(cipher), publicKey);

		System.out.println(new String(decodedData1, "UTF-8"));

		System.out.println("EN(public)---DE(private)");

		byte[] data1 = inputStr.getBytes();

		byte[] encodedData1 = RSA.encryptByPublicKey(data1, publicKey);
		String cipher1 = Digest.bytetohex(encodedData1);
		byte[] decodedData11 = RSA.decryptByPrivateKey(Digest.hextobyte(cipher1), privateKey);

		System.out.println(new String(decodedData11, "UTF-8"));

	}

}