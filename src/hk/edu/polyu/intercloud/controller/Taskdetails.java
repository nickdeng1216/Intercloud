package hk.edu.polyu.intercloud.controller;

import hk.edu.polyu.intercloud.command.objectstorage.PutObject;
import hk.edu.polyu.intercloud.common.Common;
import hk.edu.polyu.intercloud.communication.Sockets;
import hk.edu.polyu.intercloud.model.protocol.AdditionalInformation;
import hk.edu.polyu.intercloud.model.protocol.GeneralInformation;
import hk.edu.polyu.intercloud.model.protocol.Protocol;
import hk.edu.polyu.intercloud.model.protocol.RequestInformation;
import hk.edu.polyu.intercloud.model.protocol.ResponseInformation;
import hk.edu.polyu.intercloud.security.RSA;
import hk.edu.polyu.intercloud.util.ProtocolUtil;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

/**
 * The class is a thread to run scheduled task
 * 
 * @author Kate
 *
 */
public class Taskdetails extends Thread {
	private Map<String, Object> files;
	private String transferProtocol;
	private boolean securitystatus;
	private String tocloud;
	private String jobName = "taskdetails";
	private String data_security_level;
	private boolean overwrite;

	// public static Cloud cloud;

	public Taskdetails(Map<String, Object> files, String transferProtocol,
			Boolean securitystatus, String tocloud, String data_security_level,
			boolean overwrite) {
		this.files = files;
		this.transferProtocol = transferProtocol;
		this.tocloud = tocloud;
		this.securitystatus = securitystatus;
		this.data_security_level = data_security_level;
		this.overwrite = overwrite;

	}

	@Override
	public void run() {
		synchronized (this) {
			String ip = Common.my_friends.get(tocloud).getIp();
			int port = Common.GW_PORT;

			/**
			 * New Sockets client to send protocol.
			 */

			for (Map.Entry<String, Object> entry : files.entrySet()) {

				String a = null;
				Sockets socket = new Sockets(ip, port, Common.my_name);
				String id = ProtocolUtil.generateID();
				try {
					a = put(id, entry.getValue().toString(), transferProtocol,
							securitystatus, tocloud, data_security_level,
							overwrite);

				} catch (Exception e1) {

					e1.printStackTrace();
				}
				try {
					socket.sendMessage(a);

					String[] flagContent = new String[2];
					flagContent[0] = "0";

					Common.flag.put(id, flagContent);

					do {

						Thread.sleep(3000);
					} while (Common.flag.get(id)[0].equalsIgnoreCase("0"));

				} catch (Exception e) {
					System.out.print("Exception in schdeled backup");
				}

			}

		}
	}

	public String put(String id, String objName, String transferProtocol,
			Boolean securitystatus, String tocloud, String data_security_level,
			boolean overwrite) throws ParserConfigurationException,
			SAXException, IOException, Exception {

		try {
			String privateKey = RSA.readfile(System.getProperty("user.dir")
					+ "/Key/private.pem");
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date date = new Date();
		String[] dateTime = dateFormat.format(date).split("\\s+");

		GeneralInformation generalInformation = new GeneralInformation(
				Common.my_name, tocloud, dateTime[0], dateTime[1]);

		RequestInformation requestInformation = new RequestInformation();
		requestInformation.setCommand("PutObject");
		requestInformation.addTags("ObjectName", objName);
		requestInformation.setService("ObjectStorage");
		requestInformation.addTags("TransferMethod", transferProtocol);

		PutObject putObject = new PutObject();

		HashMap<String, String> map = new HashMap<String, String>();
		map.put("TransferMethod", transferProtocol);
		map.put("Encoding", "Base64");
		map.put("DigestAlgorithm", "SHA256");
		map.put("ObjectName", objName);
		map.put("DataSecurity", data_security_level);
		map.put("ID", id);
		map.put("StorageCloud", tocloud);
		if (overwrite) {
			map.put("Overwrite", "True");
		} else {
			map.put("Overwrite", "False");
		}

		AdditionalInformation additionalInformation = putObject.pre_execute(
				null, map, Common.my_service_providers.get("ObjectStorage"));

		ResponseInformation responseInformation = null;

		Protocol protocol = new Protocol("2", id, generalInformation,
				requestInformation, responseInformation, additionalInformation,
				null);

		String pro_String = ProtocolUtil.generateRequest(protocol,
				securitystatus);
		return pro_String;

	}

}
