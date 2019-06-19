package hk.edu.polyu.intercloud.api;

import hk.edu.polyu.intercloud.common.Common;
import hk.edu.polyu.intercloud.communication.Sockets;
import hk.edu.polyu.intercloud.exceptions.AuthenticationAPIException;
import hk.edu.polyu.intercloud.exceptions.DNSServiceAPIException;
import hk.edu.polyu.intercloud.model.protocol.AdditionalInformation;
import hk.edu.polyu.intercloud.model.protocol.GeneralInformation;
import hk.edu.polyu.intercloud.model.protocol.Protocol;
import hk.edu.polyu.intercloud.model.protocol.RequestInformation;
import hk.edu.polyu.intercloud.model.protocol.ResponseInformation;
import hk.edu.polyu.intercloud.util.ProtocolUtil;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class DNSServiceAPI {
	// the target cloud.
	private String cloud;

	public DNSServiceAPI(String cloud) throws AuthenticationAPIException {
		this.cloud = cloud;
		AuthenticationAPI aAPI = new AuthenticationAPI();
		aAPI.checkAuth(cloud);
	}

	/**
	 * Add TXT records
	 * 
	 * @param records
	 *            a HashMap of TXT records to be added. e.g. Key:Service
	 *            Value:ObjectStorage:Minio;VM:VMware
	 * @param protocolSecurity
	 *            whether the protocol message needs to be signed or not.
	 * @throws DNSServiceAPIException
	 */
	public void addRecord(HashMap<String, Object> records,
			boolean protocolSecurity) throws DNSServiceAPIException {

		try {
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date date = new Date();
			String[] dateTime = dateFormat.format(date).split("\\s+");

			GeneralInformation generalInformation = new GeneralInformation(
					Common.my_name, this.cloud, dateTime[0], dateTime[1]);

			RequestInformation requestInformation = new RequestInformation();
			requestInformation.setCommand("AddRecord");
			requestInformation.setService("DNS");

			for (Map.Entry<String, Object> entry : records.entrySet()) {
				requestInformation.addTags(entry.getKey()
						.replaceAll("\\s+", ""), entry.getValue().toString()
						.replaceAll("\\s+", ""));
			}

			ResponseInformation responseInformation = null;

			AdditionalInformation additionalInformation = new AdditionalInformation();

			Protocol requestProtocolObject = new Protocol(Common.ICCP_VER,
					ProtocolUtil.generateID(), generalInformation,
					requestInformation, responseInformation,
					additionalInformation, null);

			String requestProtocolString = ProtocolUtil.generateRequest(
					requestProtocolObject, protocolSecurity);

			String ip = Common.my_friends.get(this.cloud).getIp();

			// New Sockets client to send protocol.
			Sockets socket = new Sockets(ip, Common.GW_PORT, Common.my_name);
			socket.sendMessage(requestProtocolString);

		} catch (Exception e) {
			throw new DNSServiceAPIException(e.getMessage(), e);
		}
	}

	/**
	 * Update TXT records
	 * 
	 * @param records
	 *            a HashMap of TXT records to be updated. e.g. Key:Service
	 *            Value:ObjectStorage:Minio;VM:VMware
	 * @param protocolSecurity
	 *            whether the protocol message needs to be signed or not.
	 * @throws DNSServiceAPIException
	 */
	public void updateRecord(HashMap<String, Object> records,
			boolean protocolSecurity) throws DNSServiceAPIException {

		try {
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date date = new Date();
			String[] dateTime = dateFormat.format(date).split("\\s+");

			GeneralInformation generalInformation = new GeneralInformation(
					Common.my_name, this.cloud, dateTime[0], dateTime[1]);

			RequestInformation requestInformation = new RequestInformation();
			requestInformation.setCommand("UpdateRecord");
			requestInformation.setService("DNS");

			for (Map.Entry<String, Object> entry : records.entrySet()) {
				requestInformation.addTags(entry.getKey()
						.replaceAll("\\s+", ""), entry.getValue().toString()
						.replaceAll("\\s+", ""));
			}

			ResponseInformation responseInformation = null;

			AdditionalInformation additionalInformation = new AdditionalInformation();

			Protocol requestProtocolObject = new Protocol(Common.ICCP_VER,
					ProtocolUtil.generateID(), generalInformation,
					requestInformation, responseInformation,
					additionalInformation, null);

			String requestProtocolString = ProtocolUtil.generateRequest(
					requestProtocolObject, protocolSecurity);

			String ip = Common.my_friends.get(this.cloud).getIp();

			// New Sockets client to send protocol.
			Sockets socket = new Sockets(ip, Common.GW_PORT, Common.my_name);
			socket.sendMessage(requestProtocolString);

		} catch (Exception e) {
			throw new DNSServiceAPIException(e.getMessage(), e);
		}
	}

	/**
	 * Delete all TXT records
	 * 
	 * @param protocolSecurity
	 *            whether the protocol message needs to be signed or not.
	 * @throws DNSServiceAPIException
	 */
	public void deleteAllRecord(boolean protocolSecurity)
			throws DNSServiceAPIException {

		try {
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date date = new Date();
			String[] dateTime = dateFormat.format(date).split("\\s+");

			GeneralInformation generalInformation = new GeneralInformation(
					Common.my_name, this.cloud, dateTime[0], dateTime[1]);

			RequestInformation requestInformation = new RequestInformation();
			requestInformation.setCommand("DeleteAllRecord");
			requestInformation.setService("DNS");

			ResponseInformation responseInformation = null;

			AdditionalInformation additionalInformation = new AdditionalInformation();

			Protocol requestProtocolObject = new Protocol(Common.ICCP_VER,
					ProtocolUtil.generateID(), generalInformation,
					requestInformation, responseInformation,
					additionalInformation, null);

			String requestProtocolString = ProtocolUtil.generateRequest(
					requestProtocolObject, protocolSecurity);

			String ip = Common.my_friends.get(this.cloud).getIp();

			// New Sockets client to send protocol.
			Sockets socket = new Sockets(ip, Common.GW_PORT, Common.my_name);
			socket.sendMessage(requestProtocolString);

		} catch (Exception e) {
			throw new DNSServiceAPIException(e.getMessage(), e);
		}
	}
}
