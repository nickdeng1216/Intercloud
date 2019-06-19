package hk.edu.polyu.intercloud.api;

import hk.edu.polyu.intercloud.command.inquireresource.InquireForResource;
import hk.edu.polyu.intercloud.common.Common;
import hk.edu.polyu.intercloud.communication.Sockets;
import hk.edu.polyu.intercloud.exceptions.AuthenticationAPIException;
import hk.edu.polyu.intercloud.exceptions.InquireResourceAPIException;
import hk.edu.polyu.intercloud.exceptions.ProtocolException;
import hk.edu.polyu.intercloud.model.protocol.AdditionalInformation;
import hk.edu.polyu.intercloud.model.protocol.ExceptionProtocol;
import hk.edu.polyu.intercloud.model.protocol.GeneralInformation;
import hk.edu.polyu.intercloud.model.protocol.Protocol;
import hk.edu.polyu.intercloud.model.protocol.RequestInformation;
import hk.edu.polyu.intercloud.model.protocol.ResponseInformation;
import hk.edu.polyu.intercloud.util.DatabaseUtil;
import hk.edu.polyu.intercloud.util.ProtocolUtil;

import java.io.DataOutputStream;
import java.net.Socket;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class InquireResourceAPI {
	private String cloud;

	public InquireResourceAPI() {
	}

	public InquireResourceAPI(String cloud) throws AuthenticationAPIException {
		this.cloud = cloud;
		AuthenticationAPI aAPI = new AuthenticationAPI();
		aAPI.checkAuth(cloud);
	}

	public void inquire(double proceduceId, String tocloud, String service,
			HashMap<String, Object> requirements, boolean protocolSecurity,
			boolean syn, String refProtocolId)
			throws InquireResourceAPIException {
		try {
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date date = new Date();
			String[] dateTime = dateFormat.format(date).split("\\s+");

			GeneralInformation generalInformation = new GeneralInformation(
					Common.my_name, tocloud, dateTime[0], dateTime[1]);

			RequestInformation requestInformation = new RequestInformation();
			requestInformation.setService("InquireResource");
			requestInformation.setCommand("InquireForResource");

			if (service.equalsIgnoreCase("ObjectStorage")) {
				requestInformation.addTags("Service", service);
				for (HashMap.Entry<String, Object> entry : requirements
						.entrySet()) {
					if (entry.getKey().equalsIgnoreCase("Vendor")) {
						requestInformation.addTags("Vendor", entry.getValue()
								.toString());
					} else if (entry.getKey().equalsIgnoreCase(
							"ServiceProvider")) {
						requestInformation.addTags("ServiceProvider", entry
								.getValue().toString());
					} else if (entry.getKey().equalsIgnoreCase("Disk")) {
						requestInformation.addTags("Disk", entry.getValue()
								.toString());
					} else if (entry.getKey().equalsIgnoreCase("Geolocation")) {
						requestInformation.addTags("Geolocation", entry
								.getValue().toString());
					}
				}

			} else if (service.equalsIgnoreCase("VM")) {
				requestInformation.addTags("Service", service);
				for (HashMap.Entry<String, Object> entry : requirements
						.entrySet()) {
					if (entry.getKey().equalsIgnoreCase("Vendor")) {
						requestInformation.addTags("Vendor", entry.getValue()
								.toString());
					} else if (entry.getKey().equalsIgnoreCase(
							"ServiceProvider")) {
						requestInformation.addTags("ServiceProvider", entry
								.getValue().toString());
					} else if (entry.getKey().equalsIgnoreCase("Disk")) {
						requestInformation.addTags("Disk", entry.getValue()
								.toString());
					} else if (entry.getKey().equalsIgnoreCase("CPU")) {
						requestInformation.addTags("CPU", entry.getValue()
								.toString());
					} else if (entry.getKey().equalsIgnoreCase("Memory")) {
						requestInformation.addTags("Memory", entry.getValue()
								.toString());
					} else if (entry.getKey().equalsIgnoreCase("Geolocation")) {
						requestInformation.addTags("Geolocation", entry
								.getValue().toString());
					}
				}
			}

			ResponseInformation responseInformation = null;

			InquireForResource inquireForResource = new InquireForResource();
			AdditionalInformation additionalInformation = inquireForResource
					.pre_execute(null, null, null);

			String protocolID = ProtocolUtil.generateID();

			Protocol requestProtocolObject = new Protocol(Common.ICCP_VER,
					protocolID, generalInformation, requestInformation,
					responseInformation, additionalInformation, null);

			String requestProtocolString = ProtocolUtil.generateRequest(
					requestProtocolObject, protocolSecurity);

			// System.err.println(requestProtocolString);

			String ip = Common.my_friends.get(tocloud).getIp();
			int port = Common.GW_PORT;
			/**
			 * New Sockets client to send protocol.
			 */
			Sockets socket = new Sockets(ip, port, Common.my_name);

			try {
				socket.sendMessage(requestProtocolString);
			} catch (Exception e) {
				e.printStackTrace();
			}

			if (syn) {
				// Add the waiting protocol id to flag
				String[] flagContent = new String[2];
				flagContent[0] = "0";
				if (refProtocolId != null) {
					flagContent[1] = refProtocolId;
				}
				Common.flag.put(protocolID, flagContent);

				// Wait for the response protocol(may be exception protocol)
				int i = 0;
				do {
					i++;
					Thread.sleep(3000);
				} while (!Common.flag.get(protocolID)[0].equalsIgnoreCase("2")
						&& i <= 5);

				if (i == 6) {
					Common.flag.remove(protocolID);
					throw new InquireResourceAPIException(
							"Inquire for resource failed or timeout.");
				}
			}
		} catch (Exception e) {
			// TODO: Put into DB??
			throw new InquireResourceAPIException(e.getMessage(), e);
		}
	}

	public void discoverResource(String service,
			HashMap<String, Object> requirements, boolean protocolSecurity,
			double clientRequestId) throws InquireResourceAPIException {
		InquireResourceAPI irAPI = new InquireResourceAPI();
		String referencingId = Double.toString(clientRequestId);

		// Self Inquire
		try {
			irAPI.selfInquire(12345678, Common.my_name, service, requirements,
					false, true, referencingId);

			// Obtain protocolId from Common.flag
			String protocolIdFromMyCloud = null;
			for (Map.Entry<String, String[]> entry : Common.flag.entrySet()) {
				if (entry.getValue()[0].equalsIgnoreCase("2")
						&& entry.getValue()[1].equalsIgnoreCase(referencingId)) {
					protocolIdFromMyCloud = entry.getKey();
					Common.flag.remove(entry.getKey());
					break;
				}
			}
			// Get response protocol from Database
			if (protocolIdFromMyCloud != null) {
				String protocolStringFromMyCloud = DatabaseUtil
						.getResultTrack(Long.valueOf(protocolIdFromMyCloud));
				Protocol protocolObjectFromMyCloud = ProtocolUtil
						.parseProtocolType(protocolStringFromMyCloud);

				if (protocolObjectFromMyCloud instanceof ExceptionProtocol) {
					System.out.println("No Resource Found from MyCloud "
							+ clientRequestId);
				} else if (protocolObjectFromMyCloud.getResponseInformation()
						.getTags().containsKey("Result")) {
					System.out.println("No Resource Found from MyCloud "
							+ clientRequestId);
				}
			}
		} catch (InquireResourceAPIException e) {

		} catch (NumberFormatException | SQLException | ParseException
				| ProtocolException | ClassNotFoundException e) {

		}

		// Get recommended clouds' domain names list from Exchange
		ArrayList<String> recommendedCloudList = new ArrayList<>();
		try {
			if (Common.my_exchange == null || Common.my_exchange == "") {
				throw new InquireResourceAPIException(
						"Do not have any Exchange.");
			}
			irAPI = new InquireResourceAPI(Common.my_exchange);
			irAPI.inquire(12345678, Common.my_exchange, service, requirements,
					protocolSecurity, true, referencingId);

			// Obtain protocolId from Common.flag
			String protocolIdFromExchange = null;
			for (Map.Entry<String, String[]> entry : Common.flag.entrySet()) {
				if (entry.getValue()[0].equalsIgnoreCase("2")
						&& entry.getValue()[1].equalsIgnoreCase(referencingId)) {
					protocolIdFromExchange = entry.getKey();
					Common.flag.remove(entry.getKey());
					break;
				}
			}
			// Get response protocol from Database
			if (protocolIdFromExchange != null) {
				String protocolStringFromExchange = DatabaseUtil
						.getResultTrack(Long.valueOf(protocolIdFromExchange));
				Protocol protocolObjectFromExchange = ProtocolUtil
						.parseProtocolType(protocolStringFromExchange);

				if (protocolObjectFromExchange instanceof ExceptionProtocol) {
					System.out.println("No Resource Found from Exchange "
							+ clientRequestId);
					return;
				} else if (protocolObjectFromExchange.getResponseInformation()
						.getTags().containsKey("Result")) {
					System.out.println("No Resource Found from Exchange "
							+ clientRequestId);
					return;
				} else if (protocolObjectFromExchange.getResponseInformation()
						.getTags().containsKey("Cloud")) {
					String[] cloudArray = protocolObjectFromExchange
							.getResponseInformation().getTags().get("Cloud")
							.split(";");
					for (int i = 0; i < cloudArray.length; i++) {
						recommendedCloudList.add(cloudArray[i]);
					}
					System.out.println("Some Clouds Found from Exchange "
							+ clientRequestId);
				}
			}

		} catch (AuthenticationAPIException e) {
			throw new InquireResourceAPIException(e.getMessage());
		} catch (NumberFormatException | SQLException | ParseException
				| ProtocolException | ClassNotFoundException e) {
			throw new InquireResourceAPIException(e.getMessage());
		}

		// 2. Inquiring resource according to the recommended cloud list
		if (recommendedCloudList.isEmpty()) {
			System.out.println("No recommended Cloud from Exchange "
					+ clientRequestId);
			return;
		}

		for (String cloud : recommendedCloudList) {
			try {
				irAPI = new InquireResourceAPI(cloud);
				irAPI.inquire(12345678, cloud, service, requirements,
						protocolSecurity, true, referencingId);

				// Obtain protocolId from Common.flag
				String protocolIdFromCloud = null;
				for (Map.Entry<String, String[]> entry : Common.flag.entrySet()) {
					if (entry.getValue()[0].equalsIgnoreCase("2")
							&& entry.getValue()[1]
									.equalsIgnoreCase(referencingId)) {
						protocolIdFromCloud = entry.getKey();
						Common.flag.remove(entry.getKey());
						break;
					}
				}
				// Get response protocol from Database
				if (protocolIdFromCloud != null) {
					String protocolStringFromCloud = DatabaseUtil
							.getResultTrack(Long.valueOf(protocolIdFromCloud));
					Protocol protocolObjectFromCloud = ProtocolUtil
							.parseProtocolType(protocolStringFromCloud);
					if (protocolObjectFromCloud instanceof ExceptionProtocol) {
						continue;
					} else if (protocolObjectFromCloud.getResponseInformation()
							.getTags().containsKey("Result")) {
						continue;
					} else {
						System.out.println("Resource Found from Cloud "
								+ protocolObjectFromCloud
										.getGeneralInformation().getFrom()
								+ " " + clientRequestId);
						return;
					}
				}
			} catch (AuthenticationAPIException e) {

			} catch (InquireResourceAPIException e) {

			} catch (NumberFormatException | SQLException | ParseException
					| ProtocolException | ClassNotFoundException e) {

			}
		}

		System.out.println("No Resource Found from any Cloud "
				+ clientRequestId);
	}

	// ----- For doing Experiments only!!! ----- //
	public void selfInquire(double proceduceId, String tocloud, String service,
			HashMap<String, Object> requirements, boolean protocolSecurity,
			boolean syn, String refProtocolId)
			throws InquireResourceAPIException {
		try {
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date date = new Date();
			String[] dateTime = dateFormat.format(date).split("\\s+");

			GeneralInformation generalInformation = new GeneralInformation(
					Common.my_name, tocloud, dateTime[0], dateTime[1]);

			RequestInformation requestInformation = new RequestInformation();
			requestInformation.setService("InquireResource");
			requestInformation.setCommand("InquireForResource");

			if (service.equalsIgnoreCase("ObjectStorage")) {
				requestInformation.addTags("Service", service);
				for (HashMap.Entry<String, Object> entry : requirements
						.entrySet()) {
					if (entry.getKey().equalsIgnoreCase("Vendor")) {
						requestInformation.addTags("Vendor", entry.getValue()
								.toString());
					} else if (entry.getKey().equalsIgnoreCase(
							"ServiceProvider")) {
						requestInformation.addTags("ServiceProvider", entry
								.getValue().toString());
					} else if (entry.getKey().equalsIgnoreCase("Disk")) {
						requestInformation.addTags("Disk", entry.getValue()
								.toString());
					} else if (entry.getKey().equalsIgnoreCase("Geolocation")) {
						requestInformation.addTags("Geolocation", entry
								.getValue().toString());
					}
				}

			} else if (service.equalsIgnoreCase("VM")) {
				requestInformation.addTags("Service", service);
				for (HashMap.Entry<String, Object> entry : requirements
						.entrySet()) {
					if (entry.getKey().equalsIgnoreCase("Vendor")) {
						requestInformation.addTags("Vendor", entry.getValue()
								.toString());
					} else if (entry.getKey().equalsIgnoreCase(
							"ServiceProvider")) {
						requestInformation.addTags("ServiceProvider", entry
								.getValue().toString());
					} else if (entry.getKey().equalsIgnoreCase("Disk")) {
						requestInformation.addTags("Disk", entry.getValue()
								.toString());
					} else if (entry.getKey().equalsIgnoreCase("CPU")) {
						requestInformation.addTags("CPU", entry.getValue()
								.toString());
					} else if (entry.getKey().equalsIgnoreCase("Memory")) {
						requestInformation.addTags("Memory", entry.getValue()
								.toString());
					} else if (entry.getKey().equalsIgnoreCase("Geolocation")) {
						requestInformation.addTags("Geolocation", entry
								.getValue().toString());
					}
				}
			}

			ResponseInformation responseInformation = null;

			InquireForResource inquireForResource = new InquireForResource();
			AdditionalInformation additionalInformation = inquireForResource
					.pre_execute(null, null, null);

			String protocolID = ProtocolUtil.generateID();

			Protocol requestProtocolObject = new Protocol(Common.ICCP_VER,
					protocolID, generalInformation, requestInformation,
					responseInformation, additionalInformation, null);

			String requestProtocolString = ProtocolUtil.generateRequest(
					requestProtocolObject, protocolSecurity);

			// System.err.println(requestProtocolString);

			String ip = Common.my_friends.get(tocloud).getIp();
			int port = Common.GW_PORT;
			/**
			 * New Sockets client to send protocol.
			 */
			Socket socket = new Socket(ip, port);
			DataOutputStream out = new DataOutputStream(
					socket.getOutputStream());
			out.writeUTF(requestProtocolString);
			out.flush();
			socket.close();

			if (syn) {
				// Add the waiting protocol id to flag
				String[] flagContent = new String[2];
				flagContent[0] = "0";
				if (refProtocolId != null) {
					flagContent[1] = refProtocolId;
				}
				Common.flag.put(protocolID, flagContent);

				// Wait for the response protocol(may be exception protocol)
				int i = 0;
				do {
					i++;
					Thread.sleep(3000);
				} while (!Common.flag.get(protocolID)[0].equalsIgnoreCase("2")
						&& i <= 5);

				if (i == 6) {
					Common.flag.remove(protocolID);
					throw new InquireResourceAPIException(
							"Inquire for resource failed or timeout.");
				}
			}
		} catch (Exception e) {
			// TODO: Put into DB??
			throw new InquireResourceAPIException(e.getMessage(), e);
		}
	}

	// Developing, not tested!
	public void inquireResourceComplete(double proceduceId, String service,
			HashMap<String, Object> requirements, String scope,
			String searchingMethod, boolean protocolSecurity)
			throws InquireResourceAPIException {
		try {
			InquireResourceAPI irAPI = null;

			// 1. Get recommended clouds' domain names list from Exchange
			String referencingId = ProtocolUtil.generateID();
			ArrayList<String> recommendedCloudList = new ArrayList<>();
			if (Common.my_exchange == null || Common.my_exchange == "") {
				throw new InquireResourceAPIException(
						"Do not have any Exchange.");
			}
			irAPI = new InquireResourceAPI(Common.my_exchange);
			irAPI.inquire(proceduceId, Common.my_exchange, service,
					requirements, protocolSecurity, true, referencingId);

			// Obtain protocolId from Common.flag
			String protocolIdFromExchange = null;
			for (Map.Entry<String, String[]> entry : Common.flag.entrySet()) {
				if (entry.getValue()[0].equalsIgnoreCase("2")
						&& entry.getValue()[1].equalsIgnoreCase(referencingId)) {
					protocolIdFromExchange = entry.getKey();
					Common.flag.remove(entry.getKey());
					break;
				}
			}
			// Get response protocol from Database
			if (protocolIdFromExchange != null) {
				String protocolStringFromExchange = DatabaseUtil
						.getResultTrack(Long.valueOf(protocolIdFromExchange));
				Protocol protocolObjectFromExchange = ProtocolUtil
						.parseProtocolType(protocolStringFromExchange);

				if (protocolObjectFromExchange instanceof ExceptionProtocol) {
					return;
				} else if (protocolObjectFromExchange.getResponseInformation()
						.getTags().containsKey("Result")) {
					return;
				} else if (protocolObjectFromExchange.getResponseInformation()
						.getTags().containsKey("Cloud")) {
					String[] cloudArray = protocolObjectFromExchange
							.getResponseInformation().getTags().get("Cloud")
							.split(";");
					for (int i = 0; i < cloudArray.length; i++) {
						recommendedCloudList.add(cloudArray[i]);
					}
				}
			}

			// 2. Inquiring resource according to the recommended cloud list
			if (recommendedCloudList.size() <= 0) {
				return;
			}

			if (searchingMethod.equalsIgnoreCase("Sequential")) {
				for (String cloud : recommendedCloudList) {
					irAPI = new InquireResourceAPI(cloud);
					irAPI.inquire(proceduceId, cloud, service, requirements,
							protocolSecurity, true, referencingId);

					// Obtain protocolId from Common.flag
					String protocolIdFromCloud = null;
					for (Map.Entry<String, String[]> entry : Common.flag
							.entrySet()) {
						if (entry.getValue()[0].equalsIgnoreCase("2")
								&& entry.getValue()[1]
										.equalsIgnoreCase(referencingId)) {
							protocolIdFromCloud = entry.getKey();
							Common.flag.remove(entry.getKey());
							break;
						}
					}
					// Get response protocol from Database
					if (protocolIdFromCloud != null) {
						String protocolStringFromExchange = DatabaseUtil
								.getResultTrack(Long
										.valueOf(protocolIdFromExchange));
						Protocol protocolObjectFromExchange = ProtocolUtil
								.parseProtocolType(protocolStringFromExchange);

						if (protocolObjectFromExchange instanceof ExceptionProtocol) {
							continue;
						} else if (protocolObjectFromExchange
								.getResponseInformation().getTags()
								.containsKey("Result")) {
							continue;
						} else {
							return;
						}
					}
				}
			} else if (searchingMethod.equalsIgnoreCase("Broadcast")) {
				for (String cloud : recommendedCloudList) {
					Thread t = new Thread() {
						@Override
						public void run() {
							InquireResourceAPI irAPI;
							try {
								irAPI = new InquireResourceAPI(cloud);
								irAPI.inquire(proceduceId, cloud, service,
										requirements, protocolSecurity, true,
										referencingId);
							} catch (AuthenticationAPIException
									| InquireResourceAPIException e) {

							}
						}
					};
					t.start();
				}

				// Wait for the response protocol(may be exception protocol)
				int i = 0;
				int responseCount = 0;
				do {
					i++;
					// Obtain protocolID from Common.flag
					String protocolIdFromCloud = null;
					for (Map.Entry<String, String[]> entry : Common.flag
							.entrySet()) {
						if (entry.getValue()[0].equalsIgnoreCase("2")
								&& entry.getValue()[1]
										.equalsIgnoreCase(referencingId)) {
							protocolIdFromCloud = entry.getKey();
							Common.flag.remove(entry.getKey());
							responseCount++;
							break;
						}
					}
					// Get response protocol from Database
					if (protocolIdFromCloud != null) {
						String protocolStringFromExchange = DatabaseUtil
								.getResultTrack(Long
										.valueOf(protocolIdFromExchange));
						Protocol protocolObjectFromExchange = ProtocolUtil
								.parseProtocolType(protocolStringFromExchange);

						if (protocolObjectFromExchange instanceof ExceptionProtocol) {
							continue;
						} else if (protocolObjectFromExchange
								.getResponseInformation().getTags()
								.containsKey("Result")) {
							continue;
						} else {
							return;
						}
					}
					Thread.sleep(50);
				} while (i <= 10000
						|| recommendedCloudList.size() > responseCount);
			}
		} catch (Exception e) {
			// TODO: Put into DB??
			throw new InquireResourceAPIException(e.getMessage(), e);
		}

	}
}
