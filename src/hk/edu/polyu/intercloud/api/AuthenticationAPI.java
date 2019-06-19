package hk.edu.polyu.intercloud.api;

import hk.edu.polyu.intercloud.common.Common;
import hk.edu.polyu.intercloud.communication.Sockets;
import hk.edu.polyu.intercloud.exceptions.AuthenticationAPIException;
import hk.edu.polyu.intercloud.model.protocol.AdditionalInformation;
import hk.edu.polyu.intercloud.model.protocol.GeneralInformation;
import hk.edu.polyu.intercloud.model.protocol.Protocol;
import hk.edu.polyu.intercloud.model.protocol.RequestInformation;
import hk.edu.polyu.intercloud.model.protocol.ResponseInformation;
import hk.edu.polyu.intercloud.util.CertificateUtil;
import hk.edu.polyu.intercloud.util.ProtocolUtil;

import java.io.FileInputStream;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 
 * @author Kate.xie
 *
 */
public class AuthenticationAPI {
	public void authentication(long id, String tocloud)
			throws AuthenticationAPIException {
		try {
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date date = new Date();
			String[] dateTime = dateFormat.format(date).split("\\s+");

			GeneralInformation generalInformation = new GeneralInformation(
					Common.my_name, tocloud, dateTime[0], dateTime[1]);

			RequestInformation requestInformation = new RequestInformation();
			requestInformation.setCommand("Authentication");
			requestInformation.setService("security");
			InputStream inStream = new FileInputStream(Common.KEY_PATH
					+ Common.my_name + ".cer");

			requestInformation.addTags("Certificate",
					CertificateUtil.getStringFromInputStream(inStream));

			AdditionalInformation additional = new AdditionalInformation();

			ResponseInformation responseInformation = null;
			Protocol protocol = new Protocol(Common.ICCP_VER,
					Long.toString(System.currentTimeMillis()),
					generalInformation, requestInformation,
					responseInformation, additional, null);

			String pro_String = ProtocolUtil.generateRequest(protocol, true);

			inStream.close();

			Sockets socket = new Sockets(tocloud, Common.GW_PORT,
					Common.my_name);
			socket.sendMessage(pro_String);
		} catch (Exception e) {
			throw new AuthenticationAPIException(e.getMessage(), e);
		}

	}

	public void checkAuth(String tocloud) throws AuthenticationAPIException {
		try {
			boolean auth = true;
			if (Common.my_friends.containsKey(tocloud)
					&& Common.my_friends.get(tocloud).getAuth()) {
				auth = true;
			}

			if (!auth) {
				int i = 0;
				System.out.println("===== AUTHENTICATION =====");
				authentication(0, tocloud);
				do {
					i++;
					Thread.sleep(3000);
					if (Common.my_friends.containsKey(tocloud)
							&& Common.my_friends.get(tocloud).getAuth()) {
						auth = true;
					}
				} while (!auth && i <= 5);
			}

			if (!auth) {
				throw new AuthenticationAPIException(
						"Authentication failed or timeout.");
			}
		} catch (Exception e) {
			throw new AuthenticationAPIException(e.getMessage(), e);
		}
	}

}
