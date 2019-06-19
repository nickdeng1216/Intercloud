package hk.edu.polyu.intercloud.command.security;

import hk.edu.polyu.intercloud.command.Command;
import hk.edu.polyu.intercloud.common.Common;
import hk.edu.polyu.intercloud.exceptions.DatabaseException;
import hk.edu.polyu.intercloud.exceptions.IntercloudException;
import hk.edu.polyu.intercloud.exceptions.ProtocolException;
import hk.edu.polyu.intercloud.exceptions.SecurityException;
import hk.edu.polyu.intercloud.model.protocol.AdditionalInformation;
import hk.edu.polyu.intercloud.model.protocol.ExceptionInformation;
import hk.edu.polyu.intercloud.model.protocol.ExceptionProtocol;
import hk.edu.polyu.intercloud.model.protocol.GeneralInformation;
import hk.edu.polyu.intercloud.model.protocol.Protocol;
import hk.edu.polyu.intercloud.model.protocol.RequestInformation;
import hk.edu.polyu.intercloud.model.protocol.ResponseInformation;
import hk.edu.polyu.intercloud.security.CER;
import hk.edu.polyu.intercloud.security.CRL;
import hk.edu.polyu.intercloud.security.Encryption;
import hk.edu.polyu.intercloud.security.RSA;
import hk.edu.polyu.intercloud.util.CertificateUtil;
import hk.edu.polyu.intercloud.util.DatabaseUtil;
import hk.edu.polyu.intercloud.util.KeyUtil;
import hk.edu.polyu.intercloud.util.LogUtil;
import hk.edu.polyu.intercloud.util.ProtocolUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.xml.sax.SAXException;

/**
 * step 1 in authentication
 * 
 * @author Kate.xie
 *
 */
public class Authentication implements Command {

	private Protocol protocol;
	private GeneralInformation generalInformation;
	private RequestInformation requestInformation;
	private ResponseInformation responseInformation;
	private AdditionalInformation additionalInformation;

	@Override
	public void setProtocol(Protocol protocol) {
		this.protocol = protocol;

	}

	@Override
	public Protocol getProtocol() {

		return this.protocol;
	}

	@Override
	public Protocol execute(List<Object> o) {
		String friend_cloud_cert_path = Common.KEY_PATH + "Others"
				+ File.separator
				+ this.protocol.getGeneralInformation().getFrom() + ".cer";
		String friend_cloud_publickey_path = Common.KEY_PATH + "Others"
				+ File.separator
				+ this.protocol.getGeneralInformation().getFrom() + ".pem";
		initialization();

		// process the certificate content
		CER cer = new CER();
		CRL crl = new CRL();
		String content = this.requestInformation.getValue("Certificate");
		String header = "-----BEGIN CERTIFICATE-----";
		String ender = "-----END CERTIFICATE-----";

		String sentence = content.replace(header, "");
		String sentence_new = sentence.replace(ender, "");
		// write the certificate
		StringBuffer public_temp = new StringBuffer();
		public_temp.append(header);
		public_temp.append("\r\n");
		public_temp.append(sentence_new);
		public_temp.append("\r\n");
		public_temp.append(ender);

		try {
			FileUtils.forceMkdir(new File(Common.KEY_PATH + "Others"));
			KeyUtil.writefile(friend_cloud_cert_path, public_temp.toString());
			KeyUtil.retrivePublicKey(friend_cloud_cert_path,
					friend_cloud_publickey_path);
			String challenge = Encryption.makeKeyShared();
			DatabaseUtil.insertAuthentication(protocol.getId(), challenge,
					this.protocol.getGeneralInformation().getTo());

			// check expired or not
			boolean step1 = cer.checkValidity_string(sentence_new);
			// check ca's signature
			boolean step2 = cer.verify_string(sentence_new, Common.KEY_PATH
					+ "CA" + File.separator + Common.ca_name + ".pem");
			// check with ca name
			boolean step3 = crl.verifyCA(friend_cloud_cert_path);
			// check crl
			boolean step4 = crl.verifyrevoke(friend_cloud_cert_path);

			// XXX
			System.out.println(this.generalInformation.getFrom()
					+ " - 4 steps auth result: " + step1 + " " + step2 + " "
					+ step3 + " " + step4 + " ");

			// if not valid return exception
			if (!(step1 && step2 && step3 && step4)) {

				File file = new File(friend_cloud_cert_path);
				File file_key = new File(friend_cloud_publickey_path);
				if (file.delete())
					LogUtil.logError("Certificate deleted");
				if (file_key.delete())
					LogUtil.logError("Public key deleted");

				return this.generateException("600",
						"InvalidCertificateException",
						"The certificate is invalid.");
			}
			if (!ProtocolUtil.verifyProtocol(this.protocol)) {
				LogUtil.logError("Received Protocol Error");
				return null;
			}

			return this.generateProtocol(challenge);

		} catch (IOException e) {
			LogUtil.logException(e);
			return this.generateException("0",
					IntercloudException.class.getSimpleName(), e.getMessage());
		} catch (SecurityException e) {
			LogUtil.logException(e);
			return this.generateException("103",
					SecurityException.class.getSimpleName(), e.getMessage());
		} catch (ProtocolException e) {
			LogUtil.logException(e);
			return this.generateException("0",
					IntercloudException.class.getSimpleName(), e.getMessage());
		} catch (ClassNotFoundException | SQLException | ParseException e) {
			LogUtil.logException(e);
			return this.generateException("2",
					DatabaseException.class.getSimpleName(), e.getMessage());
		} catch (CertificateException e) {
			LogUtil.logException(e);
			return this.generateException("103",
					SecurityException.class.getSimpleName(), e.getMessage());
		}
	}

	@Override
	public AdditionalInformation pre_execute(String protocol,
			HashMap<String, String> info, String systemType)
			throws ParserConfigurationException, SAXException, IOException,
			Exception {

		AdditionalInformation additional = new AdditionalInformation();
		return additional;
	}

	@Override
	public void initialization() {
		this.generalInformation = protocol.getGeneralInformation();
		this.requestInformation = protocol.getRequestInformation();
		this.additionalInformation = protocol.getAdditionalInformation();
	}

	private Protocol generateProtocol(String challenge) throws IOException,
			SecurityException {
		String friend_cloud_publickey_path = Common.KEY_PATH + "Others"
				+ File.separator
				+ this.protocol.getGeneralInformation().getFrom() + ".pem";

		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date date = new Date();
		String[] dateTime = dateFormat.format(date).split("\\s+");

		GeneralInformation generalInformation = new GeneralInformation(
				this.protocol.getGeneralInformation().getTo(), this.protocol
						.getGeneralInformation().getFrom(), dateTime[0],
				dateTime[1]);

		RequestInformation requestInformation = null;

		ResponseInformation responseInformation = new ResponseInformation();
		responseInformation.setCommand("AuthChallenge");
		responseInformation.setService("Security");

		InputStream inStream = new FileInputStream(Common.KEY_PATH
				+ Common.my_name + ".cer");
		responseInformation.addTags("Certificate",
				CertificateUtil.getStringFromInputStream(inStream));

		String mychallenge = RSA.encryptBASE64(RSA.encrypt(
				CertificateUtil.getPublicKey(friend_cloud_publickey_path),
				challenge.getBytes()));

		KeyUtil.writefile(Common.KEY_PATH + "challenge.txt", mychallenge);

		InputStream in = new FileInputStream(Common.KEY_PATH + "challenge.txt");
		responseInformation.addTags("Challenge",
				CertificateUtil.getStringFromInputStream(in));

		AdditionalInformation additionalInformation = new AdditionalInformation();
		Protocol protocol = new Protocol(Common.ICCP_VER, this.protocol.getId()
				.toString(), generalInformation, requestInformation,
				responseInformation, additionalInformation, null);

		return protocol;

	}

	private ExceptionProtocol generateException(String code, String type,
			String message) {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date date = new Date();
		String[] dateTime = dateFormat.format(date).split("\\s+");

		GeneralInformation generalInformation = new GeneralInformation(
				this.protocol.getGeneralInformation().getTo(), this.protocol
						.getGeneralInformation().getFrom(), dateTime[0],
				dateTime[1]);

		ExceptionInformation exceptionInformation = new ExceptionInformation();
		exceptionInformation.addTags("Command", "Authentication");
		exceptionInformation.addTags("Code", code);
		exceptionInformation.addTags("Type", type);
		exceptionInformation.addTags("Message", message);

		AdditionalInformation additionalInformation = new AdditionalInformation();

		return new ExceptionProtocol(this.protocol.getProtocolVersion(),
				this.protocol.getId(), generalInformation,
				exceptionInformation, additionalInformation);
	}

}
