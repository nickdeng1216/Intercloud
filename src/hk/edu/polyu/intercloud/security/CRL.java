package hk.edu.polyu.intercloud.security;

import hk.edu.polyu.intercloud.common.Common;
import hk.edu.polyu.intercloud.exceptions.SecurityException;
import hk.edu.polyu.intercloud.util.CertificateUtil;
import hk.edu.polyu.intercloud.util.KeyUtil;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.CRLException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509CRL;
import java.security.cert.X509CRLEntry;
import java.security.cert.X509Certificate;
import java.util.Date;

import org.bouncycastle.asn1.x509.CRLNumber;
import org.bouncycastle.asn1.x509.CRLReason;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.x509.X509V2CRLGenerator;
import org.bouncycastle.x509.extension.AuthorityKeyIdentifierStructure;

@SuppressWarnings("deprecation")
public class CRL {
	static {
		Security.addProvider(new BouncyCastleProvider());
	}

	public static void main(String[] args) throws Exception {
		String caCertPath = Common.KEY_PATH + "CA" + File.separator
				+ "iccp.us.cer";
		String caPrivate = Common.KEY_PATH + "CA" + File.separator
				+ "private.pem";
		X509Certificate cert = CER.getcert(caCertPath);
		BigInteger revokedSerialNumber = cert.getSerialNumber();
		CRL crl = new CRL();
		String crlString = crl.createCRL(caCertPath, caPrivate,
				revokedSerialNumber);
		KeyUtil.writefile(Common.KEY_PATH + "CA" + File.separator
				+ "iccp.us.crl", crlString);
	}

	public String createCRL(String caCert_path, String caPrivate_path,
			BigInteger revokedSerialNumber) throws SecurityException {
		try {
			X509Certificate caCert = CER.getcert(caCert_path);
			X509V2CRLGenerator crlGen = new X509V2CRLGenerator();
			Date now = new Date();
			crlGen.setIssuerDN(caCert.getSubjectX500Principal());
			crlGen.setThisUpdate(now);
			crlGen.setNextUpdate(new Date(now.getTime() + 36000 * 2 * 10));
			crlGen.setSignatureAlgorithm("MD5WithRSAEncryption");
			crlGen.addCRLEntry(revokedSerialNumber, now,
					CRLReason.CESSATION_OF_OPERATION);
			crlGen.addExtension(X509Extensions.AuthorityKeyIdentifier, false,
					new AuthorityKeyIdentifierStructure(caCert));
			crlGen.addExtension(X509Extensions.CRLNumber, false, new CRLNumber(
					BigInteger.valueOf(1)));
			String header = "-----BEGIN X509 CRL-----";
			String ender = "-----END X509 CRL-----";
			StringBuilder builder = new StringBuilder();
			builder.append(header);
			builder.append("\r\n");
			builder.append(RSA.encryptBASE64(crlGen.generateX509CRL(
					CertificateUtil.getPrivateKey(caPrivate_path), "BC")
					.getEncoded()));
			builder.append(ender);
			return builder.toString();
		} catch (CertificateParsingException | InvalidKeyException
				| CRLException | NoSuchProviderException
				| java.lang.SecurityException | SignatureException e) {
			throw new SecurityException(e.getMessage(), e);
		}
	}

	public static X509CRL getCRL(String crl_path) throws SecurityException {
		try {
			String crl = CertificateUtil.readfile(crl_path);
			byte[] crlbyte = CertificateUtil.decryptBASE64(crl);
			CertificateFactory cf = CertificateFactory.getInstance("X509");
			InputStream myInputStream = new ByteArrayInputStream(crlbyte);
			X509CRL crl_cert = (X509CRL) cf.generateCRL(myInputStream);
			myInputStream.close();
			return crl_cert;
		} catch (IOException | CertificateException | CRLException e) {
			throw new SecurityException(e.getMessage(), e);
		}
	}

	public boolean verifyrevoke(String cer_path) throws SecurityException {
		boolean flag = true;
		X509Certificate cert = CER.getcert(cer_path);
		String[] array = cert.getIssuerDN().toString().split(",");
		String CAname = array[0].split("=")[1];
		String caCrlpath = Common.KEY_PATH + "CA" + File.separator + CAname
				+ ".crl";
		X509CRL crl = getCRL(caCrlpath);
		X509CRLEntry result = crl.getRevokedCertificate(cert.getSerialNumber());
		if (result == null)
			flag = true;// not revoked
		else if (result != null)
			flag = false;// revoked
		return flag;
	}

	public String addCRL(String caCert_path, String caPrivate_path,
			BigInteger revokedSerialNumber, String crl_path)
			throws SecurityException {
		try {
			X509Certificate caCert = CER.getcert(caCert_path);
			CER.getcert(caCert_path);
			X509CRL crl = getCRL(crl_path);
			X509V2CRLGenerator crlGen = new X509V2CRLGenerator();
			Date now = new Date();
			crlGen.setIssuerDN(caCert.getSubjectX500Principal());
			crlGen.setThisUpdate(now);
			crlGen.setNextUpdate(new Date(now.getTime() + 36000 * 2 * 10));
			crlGen.setSignatureAlgorithm("MD5WithRSAEncryption");
			crlGen.addCRL(crl);
			crlGen.addCRLEntry(revokedSerialNumber, now,
					CRLReason.privilegeWithdrawn);
			crlGen.addExtension(X509Extensions.AuthorityKeyIdentifier, false,
					new AuthorityKeyIdentifierStructure(caCert));
			crlGen.addExtension(X509Extensions.CRLNumber, false, new CRLNumber(
					BigInteger.valueOf(1)));

			String header = "-----BEGIN X509 CRL-----";
			String ender = "-----END X509 CRL-----";
			StringBuilder builder = new StringBuilder();
			builder.append(header);
			builder.append("\r\n");
			builder.append(CertificateUtil.encryptBASE64(crlGen
					.generateX509CRL(
							CertificateUtil.getPrivateKey(caPrivate_path), "BC")
					.getEncoded()));
			builder.append(ender);
			return builder.toString();
		} catch (CRLException | CertificateParsingException
				| InvalidKeyException | NoSuchProviderException
				| java.lang.SecurityException | SignatureException e) {
			throw new SecurityException(e.getMessage(), e);
		}
	}

	public boolean verifyCA(String certPath) throws SecurityException {
		X509Certificate cert = CER.getcert(certPath);
		String[] array = cert.getIssuerDN().toString().split(",");
		String caName = array[0].split("=")[1];
		String caCertpath = Common.KEY_PATH + "CA" + File.separator + caName
				+ ".cer";
		X509Certificate cacert = CER.getcert(caCertpath);
		if (cert.getIssuerDN().equals(cacert.getSubjectDN())) {
			return true;
		} else {
			return false;
		}
	}

}
