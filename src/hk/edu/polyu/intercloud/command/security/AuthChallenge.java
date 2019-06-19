package hk.edu.polyu.intercloud.command.security;

import hk.edu.polyu.intercloud.command.Command;
import hk.edu.polyu.intercloud.common.Common;
import hk.edu.polyu.intercloud.communication.Sockets;
import hk.edu.polyu.intercloud.exceptions.CertificateInvalidException;
import hk.edu.polyu.intercloud.exceptions.NoSuchDataException;
import hk.edu.polyu.intercloud.exceptions.ProtocolException;
import hk.edu.polyu.intercloud.exceptions.SecurityException;
import hk.edu.polyu.intercloud.model.protocol.AdditionalInformation;
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

import org.xml.sax.SAXException;

/**
 * step 2 in authentication
 * 
 * @author Kate.xie
 *
 */
public class AuthChallenge implements Command {
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
		try {
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
			String content = this.responseInformation.getValue("Certificate");
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

			KeyUtil.writefile(friend_cloud_cert_path, public_temp.toString());

			KeyUtil.retrivePublicKey(friend_cloud_cert_path,
					friend_cloud_publickey_path);

			String challenge_original = Encryption.makeKeyShared();
			DatabaseUtil.insertAuthentication(protocol.getId(),
					challenge_original, this.protocol.getGeneralInformation()
							.getTo());

			boolean step1 = cer.checkValidity_string(sentence_new);
			// check ca's signature
			boolean step2 = cer.verify_string(sentence_new, Common.KEY_PATH
					+ "CA" + File.separator + Common.ca_name + ".pem");
			// check with ca name
			boolean step3 = crl.verifyCA(friend_cloud_cert_path);
			// check crl
			boolean step4 = crl.verifyrevoke(friend_cloud_cert_path);

			// if not valid return exception
			if (!(step1 && step2 && step3 && step4))
				throw new CertificateInvalidException("certificate invalid");

			if (!ProtocolUtil.verifyProtocol(this.protocol)) {
				System.err.print("Received Protocol Error");
				return null;
			}

			String challenge_response = new String(RSA.decrypt(CertificateUtil
					.getPrivateKey(Common.KEY_PATH + "private.pem"), RSA
					.decryptBASE64(this.responseInformation
							.getValue("Challenge"))), "UTF-8");

			String Protocol = ProtocolUtil.generateRequest(
					generateProtocol(challenge_response, challenge_original),
					true);
			String fromName = this.getProtocol().getGeneralInformation()
					.getTo();
			String toName = this.getProtocol().getGeneralInformation()
					.getFrom();

			String ip = this.getProtocol().getGeneralInformation().getFrom();
			int port = Common.GW_PORT;
			Sockets socket = new Sockets(ip, port, fromName);
			socket.sendMessage(Protocol);

			return this
					.generateProtocol(challenge_response, challenge_original);
		} catch (IOException e) {
			LogUtil.logException(e);
			return new Protocol(null, null, null, null, null, null, null);
		} catch (SecurityException | CertificateInvalidException
				| CertificateException e) {
			LogUtil.logException(e);
			return new Protocol(null, null, null, null, null, null, null);
		} catch (ProtocolException e) {
			LogUtil.logException(e);
			return new Protocol(null, null, null, null, null, null, null);
		} catch (ClassNotFoundException | SQLException | ParseException e) {
			LogUtil.logException(e);
			return new Protocol(null, null, null, null, null, null, null);
		} catch (NoSuchDataException e) {
			LogUtil.logException(e);
			return new Protocol(null, null, null, null, null, null, null);
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
		this.responseInformation = protocol.getResponseInformation();
		this.additionalInformation = protocol.getAdditionalInformation();
	}

	private Protocol generateProtocol(String challenge_response,
			String challenge) throws SecurityException, IOException {

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

		ResponseInformation responseInformation = null;

		RequestInformation requestInformation = new RequestInformation();
		requestInformation.setCommand("AuthResponseChallenge");
		requestInformation.setService("Security");
		String mychallenge = RSA.encryptBASE64(RSA.encrypt(
				CertificateUtil.getPublicKey(friend_cloud_publickey_path),
				challenge.getBytes()));
		KeyUtil.writefile(Common.KEY_PATH + "challenge.txt", mychallenge);
		InputStream inStream = new FileInputStream(Common.KEY_PATH
				+ "challenge.txt");
		requestInformation.addTags("Challenge",
				CertificateUtil.getStringFromInputStream(inStream));
		requestInformation.addTags("ResponseChallenge", challenge_response);
		requestInformation.addTags("AuthenticationRequestID", protocol.getId());
		AdditionalInformation additionalInformation = new AdditionalInformation();
		Protocol protocol = new Protocol(Common.ICCP_VER, Long.toString(System
				.currentTimeMillis()), generalInformation, requestInformation,
				responseInformation, additionalInformation, null);

		return protocol;

	}
}
