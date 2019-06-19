package hk.edu.polyu.intercloud.security;

import hk.edu.polyu.intercloud.exceptions.SecurityException;
import hk.edu.polyu.intercloud.util.CertificateUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/**
 * This class is used for RSA sign , verify and encrypt.
 * 
 * @Kate.Xie
 *
 */
@SuppressWarnings("restriction")
public class RSA {

	public static String sign(byte[] data, String privateKey)
			throws SecurityException {
		try {
			PrivateKey priKey = CertificateUtil.getPrivateKey(privateKey);
			Signature signature = Signature.getInstance("SHA1withRSA");
			signature.initSign(priKey);
			signature.update(data);
			return encryptBASE64(signature.sign());
		} catch (Exception e) {
			throw new SecurityException(e.getMessage(), e);
		}
	}

	public static boolean verify(byte[] data, String publicKey, String sign)
			throws SecurityException {
		try {
			PublicKey pubKey = CertificateUtil.getPublicKey(publicKey);
			Signature signature = Signature.getInstance("SHA1withRSA");
			signature.initVerify(pubKey);
			signature.update(data);
			return signature.verify(decryptBASE64(sign));
		} catch (Exception e) {
			throw new SecurityException(e.getMessage(), e);
		}
	}

	public static byte[] decryptByPrivateKey(byte[] data, String key)
			throws SecurityException {
		try {
			byte[] keyBytes = decryptBASE64(key);
			PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(keyBytes);
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			Key privateKey = keyFactory.generatePrivate(pkcs8KeySpec);
			Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
			cipher.init(Cipher.DECRYPT_MODE, privateKey);
			return cipher.doFinal(data);
		} catch (Exception e) {
			throw new SecurityException(e.getMessage(), e);
		}
	}

	public static byte[] encryptByPublicKey(byte[] data, String key)
			throws SecurityException {
		try {
			byte[] keyBytes = decryptBASE64(key);
			X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(keyBytes);
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			Key publicKey = keyFactory.generatePublic(x509KeySpec);
			Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
			cipher.init(Cipher.ENCRYPT_MODE, publicKey);
			return cipher.doFinal(data);
		} catch (Exception e) {
			throw new SecurityException(e.getMessage(), e);
		}
	}

	public static byte[] encryptByPrivateKey(byte[] data, String key)
			throws SecurityException {
		try {
			byte[] keyBytes = decryptBASE64(key);
			PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(keyBytes);
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			Key privateKey = keyFactory.generatePrivate(pkcs8KeySpec);
			Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
			cipher.init(Cipher.ENCRYPT_MODE, privateKey);
			return cipher.doFinal(data);
		} catch (Exception e) {
			throw new SecurityException(e.getMessage(), e);
		}
	}

	public static byte[] decryptByPublicKey(byte[] data, String key)
			throws SecurityException {
		try {
			byte[] keyBytes = decryptBASE64(key);
			X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(keyBytes);
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			Key publicKey = keyFactory.generatePublic(x509KeySpec);
			Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
			cipher.init(Cipher.DECRYPT_MODE, publicKey);
			return cipher.doFinal(data);
		} catch (Exception e) {
			throw new SecurityException(e.getMessage(), e);
		}
	}

	public static byte[] decryptBASE64(String key) throws IOException {
		return (new BASE64Decoder()).decodeBuffer(key);
	}

	public static String encryptBASE64(byte[] key) {
		return (new BASE64Encoder()).encodeBuffer(key);
	}

	public static String readfile(String file) throws IOException {
		// Read key from file
		File inputFile = new File(file);
		StringBuffer result = new StringBuffer();
		StringBuffer result_1 = new StringBuffer();
		BufferedReader reader = new BufferedReader(new FileReader(inputFile));
		String lineToRemove = "-----BEGIN RSA PRIVATE KEY-----";
		String lineToRemove_1 = "-----END RSA PRIVATE KEY-----";
		String lineToRemove_2 = "-----BEGIN PUBLIC KEY-----";
		String lineToRemove_3 = "-----END PUBLIC KEY-----";
		String lineToRemove_4 = "-----BEGIN NEW CERTIFICATE REQUEST-----";
		String lineToRemove_5 = "-----END NEW CERTIFICATE REQUEST-----";
		String lineToRemove_6 = "-----BEGIN CERTIFICATE-----";
		String lineToRemove_7 = "-----END CERTIFICATE-----";
		String lineToRemove_8 = "-----BEGIN X509 CRL-----";
		String lineToRemove_9 = "-----END X509 CRL-----";
		String currentLine;

		while ((currentLine = reader.readLine()) != null) {
			result_1.append(currentLine);
			// trim newline when comparing with lineToRemove
			String trimmedLine = currentLine.trim();
			if (trimmedLine.equals(lineToRemove)
					|| trimmedLine.equals(lineToRemove_1)
					|| trimmedLine.equals(lineToRemove_2)
					|| trimmedLine.equals(lineToRemove_3)
					|| trimmedLine.equals(lineToRemove_4)
					|| trimmedLine.equals(lineToRemove_5)
					|| trimmedLine.equals(lineToRemove_6)
					|| trimmedLine.equals(lineToRemove_7)
					|| trimmedLine.equals(lineToRemove_8)
					|| trimmedLine.equals(lineToRemove_9)) {
				// System.out.print("-");
			} else {
				result.append(currentLine);
			}
		}
		reader.close();
		return result.toString();
	}

	public static byte[] encrypt(PublicKey publicKey, byte[] plainTextData)
			throws SecurityException {
		try {
			Cipher cipher = Cipher.getInstance("RSA");
			// cipher = Cipher.getInstance("RSA", new BouncyCastleProvider());
			cipher.init(Cipher.ENCRYPT_MODE, publicKey);
			return cipher.doFinal(plainTextData);
		} catch (NoSuchPaddingException | NoSuchAlgorithmException
				| InvalidKeyException | IllegalBlockSizeException
				| BadPaddingException e) {
			throw new SecurityException(e.getMessage(), e);
		}

	}

	public static byte[] encrypt(PrivateKey privateKey, byte[] plainTextData)
			throws SecurityException {
		try {
			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.ENCRYPT_MODE, privateKey);
			return cipher.doFinal(plainTextData);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException
				| InvalidKeyException | IllegalBlockSizeException
				| BadPaddingException e) {
			throw new SecurityException(e.getMessage(), e);
		}
	}

	public static byte[] decrypt(PrivateKey privateKey, byte[] cipherData)
			throws SecurityException {
		try {
			Cipher cipher = Cipher.getInstance("RSA");
			// cipher= Cipher.getInstance("RSA", new BouncyCastleProvider());
			cipher.init(Cipher.DECRYPT_MODE, privateKey);
			return cipher.doFinal(cipherData);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException
				| InvalidKeyException | IllegalBlockSizeException
				| BadPaddingException e) {
			throw new SecurityException(e.getMessage(), e);
		}

	}
}