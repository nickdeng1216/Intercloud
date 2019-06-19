package hk.edu.polyu.intercloud.util;

import hk.edu.polyu.intercloud.common.Common;
import hk.edu.polyu.intercloud.exceptions.SecurityException;
import hk.edu.polyu.intercloud.security.CER;
import hk.edu.polyu.intercloud.security.RSA;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

public class KeyUtil {

	public static void generateKeyPair() throws IOException, SecurityException {

		try {
			KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
			kpg.initialize(2048);
			KeyPair kp = kpg.genKeyPair();
			Key publicKey = kp.getPublic();
			Key privateKey = kp.getPrivate();

			String publicString = RSA.encryptBASE64(publicKey.getEncoded());

			StringBuffer public_temp = new StringBuffer();
			public_temp.append("-----BEGIN PUBLIC KEY-----");
			public_temp.append("\r\n");
			public_temp.append(publicString);
			public_temp.append("-----END PUBLIC KEY-----");
			writefile(Common.KEY_PATH + "public.pem", public_temp.toString());

			String privateString = RSA.encryptBASE64(privateKey.getEncoded());
			StringBuffer private_temp = new StringBuffer();
			private_temp.append("-----BEGIN RSA PRIVATE KEY-----");
			private_temp.append("\r\n");
			private_temp.append(privateString);
			private_temp.append("-----END RSA PRIVATE KEY-----");
			writefile(Common.KEY_PATH + "private.pem", private_temp.toString());
			System.out.println("Keypair Generated!");
		} catch (NoSuchAlgorithmException e) {
			throw new SecurityException(e.getMessage(), e);
		}
	}

	public static void retrivePublicKey(String cerpath, String publicpath)
			throws SecurityException, IOException {
		StringBuffer public_temp = new StringBuffer();
		String content = RSA.encryptBASE64(CER.getcert(cerpath).getPublicKey()
				.getEncoded());
		public_temp.append("-----BEGIN PUBLIC KEY-----");
		public_temp.append("\r\n");
		public_temp.append(content);
		public_temp.append("-----END PUBLIC KEY-----");
		writefile(publicpath, public_temp.toString());
	}

	public static void writefile(String filename, String content)
			throws IOException {
		FileOutputStream fop = null;
		File file = null;
		try {
			file = new File(filename);
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

}
