package hk.edu.polyu.intercloud.command.security;

import hk.edu.polyu.intercloud.command.Command;
import hk.edu.polyu.intercloud.common.Common;
import hk.edu.polyu.intercloud.exceptions.ProtocolException;
import hk.edu.polyu.intercloud.exceptions.SecurityException;
import hk.edu.polyu.intercloud.exceptions.UnmatchedChallengeException;
import hk.edu.polyu.intercloud.model.cloud.Cloud;
import hk.edu.polyu.intercloud.model.protocol.AdditionalInformation;
import hk.edu.polyu.intercloud.model.protocol.GeneralInformation;
import hk.edu.polyu.intercloud.model.protocol.Protocol;
import hk.edu.polyu.intercloud.model.protocol.RequestInformation;
import hk.edu.polyu.intercloud.model.protocol.ResponseInformation;
import hk.edu.polyu.intercloud.util.CertificateUtil;
import hk.edu.polyu.intercloud.util.DatabaseUtil;
import hk.edu.polyu.intercloud.util.LogUtil;
import hk.edu.polyu.intercloud.util.ProtocolUtil;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

/**
 * step 4 in authentication
 * 
 * @author Kate.xie
 *
 */
public class ConfirmationOfAuthentication implements Command {
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
			@SuppressWarnings("unused")
			boolean a = ProtocolUtil.verifyProtocol(protocol);

			// verify challenge
			String responsechallenge = this.responseInformation
					.getValue("ResponseChallenge");

			String original_challenge = DatabaseUtil.getChallenge(
					this.responseInformation
							.getValue("AuthenticationRequestID"), protocol
							.getGeneralInformation().getTo());

			if (!responsechallenge.equals(original_challenge)) {
				throw new UnmatchedChallengeException("Unmatchedchallenge" + ""
						+ "");

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
		} catch (SQLException | ClassNotFoundException | ParseException e) {
			LogUtil.logException(e);
		} catch (SecurityException e) {
			LogUtil.logException(e);
		} catch (UnmatchedChallengeException e) {
			LogUtil.logException(e);
		} catch (ProtocolException e) {
			LogUtil.logException(e);
		}
		return new Protocol(null, null, null, null, null, null, null);
	}

	@Override
	public AdditionalInformation pre_execute(String protocol,
			HashMap<String, String> info, String systemType)
			throws ParserConfigurationException, SAXException, IOException,
			Exception {
		return null;
	}

	@Override
	public void initialization() {
		this.generalInformation = protocol.getGeneralInformation();
		this.responseInformation = protocol.getResponseInformation();
		this.additionalInformation = protocol.getAdditionalInformation();
	}

}
