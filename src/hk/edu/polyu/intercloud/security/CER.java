package hk.edu.polyu.intercloud.security;

import hk.edu.polyu.intercloud.common.Common;
import hk.edu.polyu.intercloud.exceptions.SecurityException;
import hk.edu.polyu.intercloud.util.CertificateUtil;
import hk.edu.polyu.intercloud.util.KeyUtil;
import hk.edu.polyu.intercloud.util.LogUtil;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.swing.JOptionPane;

import sun.security.pkcs10.PKCS10;
import sun.security.x509.AlgorithmId;
import sun.security.x509.CertificateAlgorithmId;
import sun.security.x509.CertificateSerialNumber;
import sun.security.x509.CertificateValidity;
import sun.security.x509.CertificateVersion;
import sun.security.x509.CertificateX509Key;
import sun.security.x509.X500Name;
import sun.security.x509.X509CertImpl;
import sun.security.x509.X509CertInfo;

@SuppressWarnings("restriction")
public class CER {

	public static void main(String args[]) {
		try {
			cer();
		} catch (SecurityException | IOException | ParseException e) {
			e.printStackTrace();
		}
	}

	static void cer() throws SecurityException, IOException, ParseException {
		String cloudName = JOptionPane.showInputDialog("Name:");
		String caPrivatePem = Common.KEY_PATH + "CA" + File.separator
				+ "private.pem";
		String caPublicPem = Common.KEY_PATH + "CA" + File.separator
				+ "public.pem";
		if (!(new File(caPrivatePem).exists())
				|| !(new File(caPublicPem).exists())) {
			JOptionPane.showMessageDialog(null, "CA's keys missing.");
			return;
		}
		Calendar c = Calendar.getInstance();
		c.add(Calendar.YEAR, 2);
		String cer = signCert(
				"CN=iccp.us, L=polyu, C=hksar, S=root",
				RSA.decryptBASE64(CertificateUtil.readfile(Common.KEY_PATH
						+ cloudName + ".csr")), Common.KEY_PATH + "CA"
						+ File.separator + "private.pem", c.getTime());
		KeyUtil.writefile(Common.KEY_PATH + cloudName + ".cer", cer);
		JOptionPane.showMessageDialog(null, "CER generated.");
	}

	static String signCert(String issuer_info, byte[] csr,
			String caPrivatePath, Date expiryDate) throws SecurityException,
			ParseException {
		try {
			PKCS10 a = new PKCS10(csr);

			X509CertInfo info = new X509CertInfo();

			SimpleDateFormat sdf = new SimpleDateFormat("dd-M-yyyy");
			String dateInString = "07-03-2017";
			Date startDate = sdf.parse(dateInString);

			CertificateValidity interval = new CertificateValidity(startDate,
					expiryDate);
			BigInteger sn = new BigInteger(64, new SecureRandom());
			X500Name owner = new X500Name(a.getSubjectName().toString());

			X500Name issuer = new X500Name(issuer_info);
			info.set(X509CertInfo.VALIDITY, interval);
			info.set(X509CertInfo.SERIAL_NUMBER,
					new CertificateSerialNumber(sn));
			info.set(X509CertInfo.SUBJECT, owner);
			info.set(X509CertInfo.ISSUER, issuer);
			info.set(X509CertInfo.KEY,
					new CertificateX509Key(a.getSubjectPublicKeyInfo()));
			info.set(X509CertInfo.VERSION, new CertificateVersion(
					CertificateVersion.V3));
			AlgorithmId algo = new AlgorithmId(
					AlgorithmId.md5WithRSAEncryption_oid);
			info.set(X509CertInfo.ALGORITHM_ID,
					new CertificateAlgorithmId(algo));

			// Sign the cert to identify the algorithm that's used.
			X509CertImpl cert = new X509CertImpl(info);
			cert.sign(CertificateUtil.getPrivateKey(caPrivatePath),
					"MD5withRSA");

			String header = "-----BEGIN CERTIFICATE-----";
			String ender = "-----END CERTIFICATE-----";
			StringBuilder builder = new StringBuilder();
			builder.append(header);
			builder.append("\r\n");
			builder.append(RSA.encryptBASE64(cert.getEncoded()));
			builder.append(ender);

			return builder.toString();
		} catch (SignatureException | NoSuchAlgorithmException | IOException
				| CertificateException | InvalidKeyException
				| NoSuchProviderException e) {
			throw new SecurityException(e.getMessage(), e);
		}
	}

	public boolean verify(String path, String CAPub_path)
			throws CertificateException, IOException, SecurityException {
		boolean verify_flag = true;
		InputStream inStream = new FileInputStream(path);
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		X509CertImpl cert = (X509CertImpl) cf.generateCertificate(inStream);
		inStream.close();
		PublicKey capublickey = CertificateUtil.getPublicKey(CAPub_path);
		try {
			cert.verify(capublickey);
		} catch (InvalidKeyException | CertificateException
				| NoSuchAlgorithmException | NoSuchProviderException
				| SignatureException e) {
			LogUtil.logException(e);
			verify_flag = false;
		}
		return verify_flag;
	}

	public boolean verify_string(String content, String CAPub_path)
			throws IOException, CertificateException, SecurityException {
		boolean verify_flag = true;
		InputStream inStream = new ByteArrayInputStream(
				RSA.decryptBASE64(content));
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		X509CertImpl cert = (X509CertImpl) cf.generateCertificate(inStream);
		inStream.close();
		PublicKey capublickey = CertificateUtil.getPublicKey(CAPub_path);
		try {
			cert.verify(capublickey);
		} catch (InvalidKeyException | CertificateException
				| NoSuchAlgorithmException | NoSuchProviderException
				| SignatureException e) {
			LogUtil.logException(e);
			verify_flag = false;
		}
		return verify_flag;
	}

	public static X509Certificate getcert(String path) throws SecurityException {
		try {
			InputStream inStream = new FileInputStream(path);
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			X509CertImpl cert = (X509CertImpl) cf.generateCertificate(inStream);
			inStream.close();
			return cert;
		} catch (CertificateException | IOException e) {
			throw new SecurityException(e.getMessage(), e);
		}
	}

	public boolean checkValidity(String path) throws CertificateException,
			IOException {
		boolean verify_flag = true;
		InputStream inStream = new FileInputStream(path);
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		X509CertImpl cert = (X509CertImpl) cf.generateCertificate(inStream);
		inStream.close();
		try {
			cert.checkValidity(new Date());
		} catch (CertificateExpiredException e) {
			System.out.println(e.getClass().getSimpleName());
			verify_flag = false;
		} catch (CertificateNotYetValidException e) {
			System.out.println(e.getClass().getSimpleName());
			verify_flag = true;
		}
		return verify_flag;
	}

	public boolean checkValidity_string(String content) throws IOException,
			CertificateException {
		boolean verify_flag = true;
		InputStream inStream = new ByteArrayInputStream(
				RSA.decryptBASE64(content));
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		X509CertImpl cert = (X509CertImpl) cf.generateCertificate(inStream);
		try {
			cert.checkValidity(new Date());
		} catch (CertificateExpiredException e) {
			System.out.println(e.getClass().getSimpleName());
			verify_flag = false;
		} catch (CertificateNotYetValidException e) {
			System.out.println(e.getClass().getSimpleName());
			verify_flag = true;
		}
		inStream.close();
		return verify_flag;
	}
}
