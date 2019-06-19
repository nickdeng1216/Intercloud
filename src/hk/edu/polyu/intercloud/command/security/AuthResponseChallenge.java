package hk.edu.polyu.intercloud.command.security;

import hk.edu.polyu.intercloud.command.Command;
import hk.edu.polyu.intercloud.common.Common;
import hk.edu.polyu.intercloud.exceptions.DatabaseException;
import hk.edu.polyu.intercloud.exceptions.IntercloudException;
import hk.edu.polyu.intercloud.exceptions.SecurityException;
import hk.edu.polyu.intercloud.model.cloud.Cloud;
import hk.edu.polyu.intercloud.model.protocol.AdditionalInformation;
import hk.edu.polyu.intercloud.model.protocol.ExceptionInformation;
import hk.edu.polyu.intercloud.model.protocol.ExceptionProtocol;
import hk.edu.polyu.intercloud.model.protocol.GeneralInformation;
import hk.edu.polyu.intercloud.model.protocol.Protocol;
import hk.edu.polyu.intercloud.model.protocol.RequestInformation;
import hk.edu.polyu.intercloud.model.protocol.ResponseInformation;
import hk.edu.polyu.intercloud.security.RSA;
import hk.edu.polyu.intercloud.util.CertificateUtil;
import hk.edu.polyu.intercloud.util.DatabaseUtil;
import hk.edu.polyu.intercloud.util.LogUtil;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
 * step 3 in authentication
 * 
 * @author Kate.xie
 *
 */
public class AuthResponseChallenge implements Command {
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

		initialization();

		try {
			// verify challenge
			String responsechallenge = this.requestInformation
					.getValue("ResponseChallenge");
			String original_challenge = DatabaseUtil
					.getChallenge(this.requestInformation
							.getValue("AuthenticationRequestID"), protocol
							.getGeneralInformation().getTo());

			if (!responsechallenge.equals(original_challenge)) {
				return generateException(Common.ICCP_VER, "Unmatchedchallenge",
						"the challenge does not match");
			}

			// Update my_friends instead of DB
			if (Common.my_friends
					.containsKey(this.generalInformation.getFrom())) {
				Common.my_friends.get(this.generalInformation.getFrom())
						.setAuth(true);
			} else {
				String role = CertificateUtil.getRole(Common.KEY_PATH
						+ "Others" + File.separator
						+ this.protocol.getGeneralInformation().getFrom()
						+ ".cer");

				Common.my_friends.put(this.generalInformation.getFrom(),
						new Cloud(this.generalInformation.getFrom(),
								this.generalInformation.getFrom(), role, true));
			}
			// DatabaseUtil.update_auth(con, this.generalInformation.getFrom(),
			// true);

			String challenge_response = new String(RSA.decrypt(CertificateUtil
					.getPrivateKey(Common.KEY_PATH + "private.pem"), RSA
					.decryptBASE64(this.requestInformation
							.getValue("Challenge"))), "UTF-8");
			return generateProtocol(challenge_response);
		} catch (SQLException | ClassNotFoundException | ParseException e) {
			LogUtil.logException(e);
			return this.generateException("2",
					DatabaseException.class.getSimpleName(), e.getMessage());
		} catch (SecurityException e) {
			LogUtil.logException(e);
			return this.generateException("103",
					SecurityException.class.getSimpleName(), e.getMessage());
		} catch (UnsupportedEncodingException e) {
			LogUtil.logException(e);
			return this.generateException("0",
					IntercloudException.class.getSimpleName(), e.getMessage());
		} catch (IOException e) {
			LogUtil.logException(e);
			return this.generateException("0",
					IntercloudException.class.getSimpleName(), e.getMessage());
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

	private Protocol generateProtocol(String challenge_response) {

		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date date = new Date();
		String[] dateTime = dateFormat.format(date).split("\\s+");

		GeneralInformation generalInformation = new GeneralInformation(
				this.protocol.getGeneralInformation().getTo(), this.protocol
						.getGeneralInformation().getFrom(), dateTime[0],
				dateTime[1]);

		RequestInformation requestInformation = null;

		ResponseInformation responseInformation = new ResponseInformation();
		responseInformation.setCommand("ConfirmationOfAuthentication");
		responseInformation.setService("Security");
		responseInformation.addTags("ResponseChallenge", challenge_response);
		responseInformation.addTags("AuthenticationRequestID",
				this.requestInformation.getValue("AuthenticationRequestID"));

		AdditionalInformation additionalInformation = new AdditionalInformation();
		Protocol protocol = new Protocol(Common.ICCP_VER,
				this.protocol.getId(), generalInformation, requestInformation,
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
		exceptionInformation.addTags("Command", "AuthResponseChallenge");
		exceptionInformation.addTags("Code", code);
		exceptionInformation.addTags("Type", type);
		exceptionInformation.addTags("Message", message);

		AdditionalInformation additionalInformation = new AdditionalInformation();

		return new ExceptionProtocol(this.protocol.getProtocolVersion(),
				this.protocol.getId(), generalInformation,
				exceptionInformation, additionalInformation);
	}

}
