package hk.edu.polyu.intercloud.security;

import hk.edu.polyu.intercloud.exceptions.SecurityException;
import hk.edu.polyu.intercloud.util.CertificateUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateException;

import javax.security.auth.x500.X500Principal;

import sun.security.pkcs10.PKCS10;
import sun.security.x509.X500Name;

/**
 * 
 * @author Kate.xie
 *
 */
@SuppressWarnings("restriction")
public class CSR {

	public static String generatePKCS10(String subject, String privateKey_path,
			String publicKey_path) throws SecurityException, IOException {
		try {
			PrivateKey privateKey = CertificateUtil
					.getPrivateKey(privateKey_path);
			PublicKey publicKey = CertificateUtil.getPublicKey(publicKey_path);
			// generate PKCS10 certificate request
			String sigAlg = "MD5withRSA";
			PKCS10 pkcs10 = new PKCS10(publicKey);
			Signature signature = Signature.getInstance(sigAlg);
			signature.initSign(privateKey);
			// common, orgUnit, org, locality, state, country
			X500Principal principal = new X500Principal(subject);
			X500Name x500name = null;
			x500name = new X500Name(principal.getEncoded());
			pkcs10.encodeAndSign(x500name, signature);
			ByteArrayOutputStream bs = new ByteArrayOutputStream();
			PrintStream ps = new PrintStream(bs);
			pkcs10.print(ps);
			byte[] content = bs.toByteArray();
			if (ps != null)
				ps.close();
			if (bs != null)
				bs.close();
			return new String(content);
		} catch (SignatureException | NoSuchAlgorithmException
				| InvalidKeyException | CertificateException e) {
			throw new SecurityException(e.getMessage(), e);
		}
	}

}