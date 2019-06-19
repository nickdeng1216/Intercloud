package hk.edu.polyu.intercloud.util;

import hk.edu.polyu.intercloud.exceptions.SecurityException;
import hk.edu.polyu.intercloud.security.CER;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/**
 * used to get public key or private key format enbase 64 or debase 64 read
 * 
 * @author Kate.xie
 *
 */
@SuppressWarnings("restriction")
public class CertificateUtil {

	public static byte[] decryptBASE64(String key) throws IOException {
		return (new BASE64Decoder()).decodeBuffer(key);
	}

	public static String encryptBASE64(byte[] key) {
		return (new BASE64Encoder()).encodeBuffer(key);
	}

	static void writefile(String filensme, String content) throws IOException {
		FileOutputStream fop = null;
		File file = null;
		try {
			file = new File(filensme);
			fop = new FileOutputStream(file);
			// if file does not exists, then create it
			if (!file.exists()) {
				file.createNewFile();
			}
			byte[] contentInBytes = content.getBytes();
			fop.write(contentInBytes);
			fop.flush();
			fop.close();
		} catch (IOException e) {
			throw new IOException(e.getMessage(), e);
		} finally {
			if (fop != null) {
				fop.close();
			}
		}
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
		String resultcert = result.toString();
		String header = "-----BEGIN CERTIFICATE-----";
		String ender = "-----END CERTIFICATE-----";
		return resultcert.replace(ender, "");
	}

	public static String readfile_string(String content) throws IOException {
		// Read key from file
		StringBuffer result = new StringBuffer();
		StringBuffer result_1 = new StringBuffer();
		BufferedReader reader = new BufferedReader(new StringReader(content));
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
		return result.toString();
	}

	public static String readfileinbase64(String path)
			throws FileNotFoundException, IOException {
		InputStream in = new FileInputStream(new File(path));
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		StringBuilder out = new StringBuilder();
		String line;
		while ((line = reader.readLine()) != null) {
			out.append(line);
		}
		System.out.println(out.toString()); // Prints the string content read
											// from input stream
		reader.close();
		return out.toString();
	}

	public static PublicKey getPublicKey(String path) throws SecurityException {
		try {
			String key_public = CertificateUtil.readfile(path);
			byte[] keyBytes_pub = CertificateUtil.decryptBASE64(key_public);
			X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(
					keyBytes_pub);
			KeyFactory keyFactory_pub = KeyFactory.getInstance("RSA");
			PublicKey publicKey = keyFactory_pub.generatePublic(x509KeySpec);
			return publicKey;
		} catch (IOException | NoSuchAlgorithmException
				| InvalidKeySpecException e) {
			throw new SecurityException(e.getMessage(), e);
		}

	}

	public static PrivateKey getPrivateKey(String path)
			throws SecurityException {
		try {
			String key = CertificateUtil.readfile(path);
			byte[] keyBytes = CertificateUtil.decryptBASE64(key);
			PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(keyBytes);
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			PrivateKey privateKey = keyFactory.generatePrivate(pkcs8KeySpec);
			return privateKey;
		} catch (IOException | NoSuchAlgorithmException
				| InvalidKeySpecException e) {
			throw new SecurityException(e.getMessage(), e);
		}
	}

	public static String getStringFromInputStream(InputStream is)
			throws IOException {
		BufferedReader br = null;
		StringBuilder sb = new StringBuilder();
		String line;
		try {
			br = new BufferedReader(new InputStreamReader(is));
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}

		} catch (IOException e) {
			throw new IOException(e.getMessage(), e);
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					throw new IOException(e.getMessage(), e);
				}
			}
		}
		return sb.toString();
	}

	public static String getRole(String path) throws SecurityException {
		X509Certificate cert = CER.getcert(path);
		String[] array = cert.getSubjectDN().toString().split(",");
		String role = array[3].split("=")[1];
		return role;
	}
}
