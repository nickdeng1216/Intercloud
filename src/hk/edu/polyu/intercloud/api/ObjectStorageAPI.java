package hk.edu.polyu.intercloud.api;

import hk.edu.polyu.intercloud.command.objectstorage.DeleteObject;
import hk.edu.polyu.intercloud.command.objectstorage.ForwardObject;
import hk.edu.polyu.intercloud.command.objectstorage.GetObject;
import hk.edu.polyu.intercloud.command.objectstorage.ListObject;
import hk.edu.polyu.intercloud.command.objectstorage.NotificationofForward;
import hk.edu.polyu.intercloud.command.objectstorage.PutObject;
import hk.edu.polyu.intercloud.common.Common;
import hk.edu.polyu.intercloud.communication.Sockets;
import hk.edu.polyu.intercloud.exceptions.*;
import hk.edu.polyu.intercloud.model.protocol.AdditionalInformation;
import hk.edu.polyu.intercloud.model.protocol.ExceptionInformation;
import hk.edu.polyu.intercloud.model.protocol.ExceptionProtocol;
import hk.edu.polyu.intercloud.model.protocol.GeneralInformation;
import hk.edu.polyu.intercloud.model.protocol.Protocol;
import hk.edu.polyu.intercloud.model.protocol.RequestInformation;
import hk.edu.polyu.intercloud.model.protocol.ResponseInformation;
import hk.edu.polyu.intercloud.util.DatabaseUtil;
import hk.edu.polyu.intercloud.util.ProtocolUtil;

import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

/**
 * The API class for Object Storage service
 * 
 * @author Kate
 */
public class ObjectStorageAPI {

	/**
	 * The target Cloud
	 */
	private String cloud;

	/**
	 * Construct an Object Storage API class object.
	 * 
	 * @param cloud
	 *            The name of the target cloud
	 * @throws AuthenticationAPIException
	 * @throws ObjectStorageAPIException
	 */
	public ObjectStorageAPI(String cloud) throws AuthenticationAPIException,
			ObjectStorageAPIException {
		this.cloud = cloud;
		AuthenticationAPI aAPI = new AuthenticationAPI();
		aAPI.checkAuth(cloud);
		checkMyService();
	}

	public final class DATA_SECURITY {
		public static final String PUBLIC = "PUBLIC";
		public static final String PRIVATE = "PRIVATE";
		public static final String SHARED = "SHARED";
	}

	/**
	 * Query for a list of objects stored in the target cloud. Example:
	 * 
	 * <pre>
	 * list(true);
	 * </pre>
	 * 
	 * queries for a list of objects stored in the target cloud.
	 * 
	 * @param protocolSecurity
	 *            Whether the protocol message needs to be signed or not.
	 * @return The protocol ID, used for status checking.
	 * @throws ObjectStorageAPIException
	 */
	public String list(boolean protocolSecurity)
			throws ObjectStorageAPIException {
		String protocolid = ProtocolUtil.generateID();

        try {
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = new Date();
            String[] dateTime = dateFormat.format(date).split("\\s+");

            GeneralInformation generalInformation = new GeneralInformation(Common.my_name, cloud, dateTime[0], dateTime[1]);

            RequestInformation requestInformation = new RequestInformation();
            requestInformation.setCommand("ListObject");
            requestInformation.setService("ObjectStorage");
            ResponseInformation responseInformation = null;

            ListObject listobject = new ListObject();
            HashMap<String, String> map = new HashMap<>();

            AdditionalInformation additionalInformation = listobject.pre_execute(null, map, Common.my_service_providers.get("ObjectStorage"));

            Protocol protocol = new Protocol(Common.ICCP_VER, protocolid, generalInformation, requestInformation,
                    responseInformation, additionalInformation, null);

            String pro_String = ProtocolUtil.generateRequest(protocol, protocolSecurity);

            String ip = Common.my_friends.get(cloud).getIp();
            int port = Common.GW_PORT;

            /**
             * New Sockets client to send protocol.
             */
            Sockets socket = new Sockets(ip, port, Common.my_name);

            if (requestInformation != null && responseInformation == null) {
                socket.sendMessage(pro_String);
            }

            return protocolid;

        } catch (NoSuchDataException e) {
            throw new ObjectStorageAPIException(e.getMessage(), e);
        } catch (ProtocolException e) {
            throw new ObjectStorageAPIException(e.getMessage(), e);
        } catch (ClassNotFoundException e) {
            throw new ObjectStorageAPIException(e.getMessage(), e);
        } catch (SQLException e) {
            throw new ObjectStorageAPIException(e.getMessage(), e);
        } catch (ParseException e) {
            throw new ObjectStorageAPIException(e.getMessage(), e);
        }
	}

	/**
	 * Store an object using the Embedded method. Example:
	 * 
	 * <pre>
	 * put(&quot;Rabbit.jpg&quot;, true, DATA_SECURITY.PUBLIC, true);
	 * </pre>
	 * 
	 * stores Rabbit.jpg to the target cloud by a signed protocol message,
	 * overwrites all previous versions.
	 * 
	 * @param objName
	 *            The name of the object to be stored.
	 * @param protocolSecurity
	 *            Whether the protocol message needs to be signed or not.
	 * @param dataSecurity
	 *            The data security level, can either be: DATA_SECURITY.PUBLIC,
	 *            DATA_SECURITY.PRIVATE or DATA_SECURITY.SHARED.
	 * @param overwrite
	 *            Overwrite all previous versions or not.
	 * @return The protocol ID, used for status checking.
	 * @throws ObjectStorageAPIException
	 */
	public String put(String objName, boolean protocolSecurity,
			String dataSecurity, boolean overwrite)
			throws ObjectStorageAPIException {
		String protocolid = ProtocolUtil.generateID();
		try {
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date date = new Date();
			String[] dateTime = dateFormat.format(date).split("\\s+");

			GeneralInformation generalInformation = new GeneralInformation(
					Common.my_name, cloud, dateTime[0], dateTime[1]);

			RequestInformation requestInformation = new RequestInformation();
			requestInformation.setCommand("PutObject");
			requestInformation.setService("ObjectStorage");
			requestInformation.addTags("ObjectName", objName);
			requestInformation.addTags("TransferMethod", "Embedded");

			ResponseInformation responseInformation = null;

			PutObject putobject = new PutObject();
			HashMap<String, String> map = new HashMap<>();
			map.put("ObjectName", objName);
			map.put("TransferMethod", "Embedded");
			map.put("Encoding", "Base64");
			map.put("DataDigestAlgorithm", "SHA256");
			map.put("DataSecurity", dataSecurity);
			map.put("ID", protocolid);
			map.put("StorageCloud", cloud);
			if (overwrite) {
				map.put("Overwrite", "True");
			} else {
				map.put("Overwrite", "False");
			}

			AdditionalInformation additionalInformation = putobject
					.pre_execute(null, map,
							Common.my_service_providers.get("ObjectStorage"));

			Protocol protocol = new Protocol(Common.ICCP_VER, protocolid,
					generalInformation, requestInformation,
					responseInformation, additionalInformation, null);

			String pro_String = ProtocolUtil.generateRequest(protocol,
					protocolSecurity);

			String ip = Common.my_friends.get(cloud).getIp();
			int port = Common.GW_PORT;

			/**
			 * New Sockets client to send protocol.
			 */
			Sockets socket = new Sockets(ip, port, Common.my_name);

			if (requestInformation != null && responseInformation == null) {
				socket.sendMessage(pro_String);
			}

			return protocolid;

		} catch (Exception e) {
			throw new ObjectStorageAPIException(e.getMessage(), e);
		}
	}

	/**
	 * Store an object using other methods. Example:
	 * 
	 * <pre>
	 * put(&quot;Butterfly.jpg&quot;, &quot;HTTPS&quot;, true, DATA_SECURITY.PRIVATE, false);
	 * </pre>
	 * 
	 * encrypts and stores Butterfly.jpg to the target cloud via HTTPS by a
	 * signed protocol message. Old versions would not be overwritten.
	 * 
	 * @param objName
	 *            The name of object to be stored.
	 * @param transferProtocol
	 *            The data transfer method, e.g. HTTPS.
	 * @param protocolSecurity
	 *            Whether the protocol message needs to be signed or not.
	 * @param dataSecurity
	 *            The data security level, can either be DATA_SECURITY.PUBLIC,
	 *            DATA_SECURITY.PRIVATE or DATA_SECURITY.SHARED.
	 * @param overwrite
	 *            Overwrite all previous versions or not.
	 * @return The protocol ID, used for status checking.
	 * @throws ObjectStorageAPIException
	 */
	public String put(String objName, String transferProtocol,
			boolean protocolSecurity, String dataSecurity, boolean overwrite)
			throws ObjectStorageAPIException {
		try {
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date date = new Date();
			String[] dateTime = dateFormat.format(date).split("\\s+");

			GeneralInformation generalInformation = new GeneralInformation(
					Common.my_name, cloud, dateTime[0], dateTime[1]);

			RequestInformation requestInformation = new RequestInformation();
			requestInformation.setCommand("PutObject");
			requestInformation.setService("ObjectStorage");
			requestInformation.addTags("ObjectName", objName);
			requestInformation.addTags("TransferMethod", transferProtocol);

			PutObject putObject = new PutObject();
			String protocolid = ProtocolUtil.generateID();
			HashMap<String, String> map = new HashMap<>();

			map.put("TransferMethod", transferProtocol);
			map.put("Encoding", "Base64");
			map.put("DataDigestAlgorithm", "SHA256");
			map.put("ObjectName", objName);
			map.put("DataSecurity", dataSecurity);
			map.put("ID", protocolid);
			map.put("StorageCloud", cloud);
			if (overwrite) {
				map.put("Overwrite", "True");
			} else {
				map.put("Overwrite", "False");
			}

			AdditionalInformation additionalInformation = putObject
					.pre_execute(null, map,
							Common.my_service_providers.get("ObjectStorage"));

			ResponseInformation responseInformation = null;

			Protocol protocol = new Protocol(Common.ICCP_VER, protocolid,
					generalInformation, requestInformation,
					responseInformation, additionalInformation, null);

			String pro_String = ProtocolUtil.generateRequest(protocol,
					protocolSecurity);

			String ip = Common.my_friends.get(cloud).getIp();
			int port = Common.GW_PORT;

			/**
			 * New Sockets client to send protocol.
			 */
			Sockets socket = new Sockets(ip, port, Common.my_name);

			if (requestInformation != null && responseInformation == null) {
				socket.sendMessage(pro_String);
			}

			return protocolid;
		} catch (Exception e) {
			throw new ObjectStorageAPIException(e.getMessage(), e);
		}
	}

	/**
	 * Store multiple objects using the Embedded method. Example:
	 * 
	 * <pre>
	 * put(list, true, DATA_SECURITY.PRIVATE, false);
	 * </pre>
	 * 
	 * encrypts and stores the list of objects to the target cloud by a signed
	 * protocol message. Old versions would not be overwritten.
	 * 
	 * @param objects
	 *            An ArrayList of the names of object to be stored.
	 * @param protocolSecurity
	 *            Whether the protocol message needs to be signed or not.
	 * @param dataSecurity
	 *            The data security level, can either be DATA_SECURITY.PUBLIC,
	 *            DATA_SECURITY.PRIVATE or DATA_SECURITY.SHARED.
	 * @param overwrite
	 *            Overwrite all previous versions or not.
	 * @return The protocol ID, used for status checking.
	 * @throws ObjectStorageAPIException
	 */
	public String put(ArrayList<String> objects, boolean protocolSecurity,
			String dataSecurity, boolean overwrite)
			throws ObjectStorageAPIException {
		try {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < objects.size(); i++) {
				sb.append(objects.get(i)).append(";");
			}
			sb.deleteCharAt(sb.length() - 1);
			return put(sb.toString(), protocolSecurity, dataSecurity, overwrite);
		} catch (Exception e) {
			throw new ObjectStorageAPIException(e.getMessage(), e);
		}
	}

	/**
	 * Store multiple objects using the other methods. Example:
	 * 
	 * <pre>
	 * put(list, "HTTPS", true, DATA_SECURITY.PRIVATE, true);
	 * 
	 * <pre>
	 * encrypts and stores the list of objects to the target cloud via HTTPS by
	 * a signed protocol message. All old versions would be overwritten.
	 * 
	 * @param objects
	 *            An ArrayList of the names of object to be stored.
	 * @param transferProtocol
	 *            The data transfer method, e.g. HTTPS.
	 * @param protocolSecurity
	 *            Whether the protocol message needs to be signed or not.
	 * @param dataSecurity
	 *            The data security level, can either be DATA_SECURITY.PUBLIC,
	 *            DATA_SECURITY.PRIVATE or DATA_SECURITY.SHARED.
	 * @param overwrite
	 *            Overwrite all previous versions or not.
	 * @return The protocol ID, used for status checking.
	 * @throws ObjectStorageAPIException
	 */
	public String put(ArrayList<String> objects, String transferProtocol,
			boolean protocolSecurity, String dataSecurity, boolean overwrite)
			throws ObjectStorageAPIException {
		try {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < objects.size(); i++) {
				sb.append(objects.get(i)).append(";");
			}
			sb.deleteCharAt(sb.length() - 1);
			return put(sb.toString(), transferProtocol, protocolSecurity,
					dataSecurity, overwrite);
		} catch (Exception e) {
			throw new ObjectStorageAPIException(e.getMessage(), e);
		}
	}

	/**
	 * Retrieve an object using the Embedded method. Example:
	 * 
	 * <pre>
	 * get(&quot;Cat.jpg&quot;, false, DATA_SECURITY.PUBLIC);
	 * </pre>
	 * 
	 * retrieves Cat.jpg from the target cloud by an unsigned protocol message.
	 * 
	 * @param objName
	 *            The name of object to be retrieved. Whether the protocol
	 *            message needs to be signed or not.
	 * @param dataSecurity
	 *            The data security level, can either be DATA_SECURITY.PUBLIC,
	 *            DATA_SECURITY.PRIVATE or DATA_SECURITY.SHARED.
	 * @return The protocol ID, used for status checking.
	 * @throws ObjectStorageAPIException
	 */
	public String get(String objName, boolean protocolSecurity,
			String dataSecurity) throws ObjectStorageAPIException {
		try {
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date date = new Date();
			String[] dateTime = dateFormat.format(date).split("\\s+");

			GeneralInformation generalInformation = new GeneralInformation(
					Common.my_name, cloud, dateTime[0], dateTime[1]);

			RequestInformation requestInformation = new RequestInformation();
			requestInformation.setCommand("GetObject");
			requestInformation.setService("ObjectStorage");
			requestInformation.addTags("ObjectName", objName);
			requestInformation.addTags("TransferMethod", "Embedded");

			ResponseInformation responseInformation = null;
			GetObject getobject = new GetObject();
			String protocolid = ProtocolUtil.generateID();
			HashMap<String, String> map = new HashMap<>();
			map.put("TransferMethod", "Embedded");
			map.put("ObjectName", objName);
			map.put("DataDigestAlgorithm", "SHA256");
			map.put("ToCloud", cloud);
			map.put("ID", protocolid);
			String level = DatabaseUtil.getDataSecurityLevelOwn(objName, cloud);
			if (level.equalsIgnoreCase("private")
					&& !dataSecurity.equalsIgnoreCase("private"))
				throw new DataSecurityLevelUnmatchedException(
						"Unmatched data security level");
			else if (!level.equalsIgnoreCase("private")
					&& dataSecurity.equalsIgnoreCase("private"))
				throw new DataSecurityLevelUnmatchedException(
						"Unmatched data security level");
			map.put("DataSecurity", dataSecurity);
			AdditionalInformation additionalInformation = getobject
					.pre_execute(null, map,
							Common.my_service_providers.get("ObjectStorage"));

			Protocol protocol = new Protocol(Common.ICCP_VER, protocolid,
					generalInformation, requestInformation,
					responseInformation, additionalInformation, null);
			String pro_String = ProtocolUtil.generateRequest(protocol,
					protocolSecurity);

			String ip = Common.my_friends.get(cloud).getIp();
			int port = Common.GW_PORT;

			/**
			 * New Sockets client to send protocol.
			 */
			Sockets socket = new Sockets(ip, port, Common.my_name);

			if (requestInformation != null && responseInformation == null) {
				socket.sendMessage(pro_String);
			}

			return protocolid;
		} catch (Exception e) {
			throw new ObjectStorageAPIException(e.getMessage(), e);
		}
	}

	/**
	 * Retrieve an object using other methods. Example:
	 * 
	 * <pre>
	 * get(&quot;Cow.jpg&quot;, &quot;UDT&quot;, true, DATA_SECURITY.SHARED);
	 * </pre>
	 * 
	 * retrieves Cow.jpg via UDT by a signed protocol message. As the data
	 * security mode is shared, the target cloud first encrypts the object
	 * before transferring it, and the receiving cloud decrypts it afterwards.
	 * 
	 * @param objName
	 *            The name of object to be retrieved.
	 * @param transferProtocol
	 *            The data transfer method, e.g. HTTPS.
	 * @param protocolSecurity
	 *            Whether the protocol message needs to be signed or not.
	 * @param dataSecurity
	 *            The data security level, can either be DATA_SECURITY.PUBLIC,
	 *            DATA_SECURITY.PRIVATE or DATA_SECURITY.SHARED.
	 * @return The protocol ID, used for status checking.
	 * @throws ObjectStorageAPIException
	 */
	public String get(String objName, String transferProtocol,
			boolean protocolSecurity, String dataSecurity)
			throws ObjectStorageAPIException {
		try {
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date date = new Date();
			String[] dateTime = dateFormat.format(date).split("\\s+");

			GeneralInformation generalInformation = new GeneralInformation(
					Common.my_name, cloud, dateTime[0], dateTime[1]);

			RequestInformation requestInformation = new RequestInformation();
			requestInformation.setCommand("GetObject");
			requestInformation.setService("ObjectStorage");
			requestInformation.addTags("ObjectName", objName);
			requestInformation.addTags("TransferMethod", transferProtocol);
			String protocolid = ProtocolUtil.generateID();
			GetObject getobject = new GetObject();
			HashMap<String, String> map = new HashMap<>();
			map.put("TransferMethod", transferProtocol);
			map.put("ObjectName", objName);
			map.put("DataDigestAlgorithm", "SHA256");
			map.put("ID", protocolid);
			map.put("ToCloud", cloud);
			String level = DatabaseUtil.getDataSecurityLevelOwn(objName, cloud);
			if (level.equalsIgnoreCase("private")
					&& !dataSecurity.equalsIgnoreCase("private"))
				throw new DataSecurityLevelUnmatchedException(
						"Unmatched data security level");
			else if (!level.equalsIgnoreCase("private")
					&& dataSecurity.equalsIgnoreCase("private"))
				throw new DataSecurityLevelUnmatchedException(
						"Unmatched data security level");

			map.put("DataSecurity", dataSecurity);
			AdditionalInformation additionalInformation = getobject
					.pre_execute(null, map,
							Common.my_service_providers.get("ObjectStorage"));

			ResponseInformation responseInformation = null;
			Protocol protocol = new Protocol(Common.ICCP_VER, protocolid,
					generalInformation, requestInformation,
					responseInformation, additionalInformation, null);

			String pro_String = ProtocolUtil.generateRequest(protocol,
					protocolSecurity);

			String ip = Common.my_friends.get(cloud).getIp();
			int port = Common.GW_PORT;

			/**
			 * New Sockets client to send protocol.
			 */
			Sockets socket = new Sockets(ip, port, Common.my_name);

			if (requestInformation != null && responseInformation == null) {
				socket.sendMessage(pro_String);
			}

			return protocolid;
		} catch (Exception e) {
			throw new ObjectStorageAPIException(e.getMessage(), e);
		}
	}

	/**
	 * Retrieve a specific version of object using the Embedded method. Example:
	 * 
	 * <pre>
	 * getVersion("Dog.jpg", "20110507120303", false,
	 * DATA_SECURITY.PUBLIC);
	 * 
	 * <pre>
	 * retrieves Dog.jpg (version 2011-05-07 12:03:03) from the target cloud by
	 * an unsigned protocol message.
	 * 
	 * @param objName
	 *            The name of object to be retrieved.
	 * @param versionDate
	 *            The date and time of the version, with format
	 *            "yyyyMMddHHmmss".
	 * @param protocolSecurity
	 *            Whether the protocol message needs to be signed or not.
	 * @param dataSecurity
	 *            The data security level, can either be DATA_SECURITY.PUBLIC,
	 *            DATA_SECURITY.PRIVATE or DATA_SECURITY.SHARED.
	 * @return The protocol ID, used for status checking.
	 * @throws ObjectStorageAPIException
	 */
	public String getVersion(String objName, String versionDate,
			boolean protocolSecurity, String dataSecurity)
			throws ObjectStorageAPIException {
		try {
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date date = new Date();
			String[] dateTime = dateFormat.format(date).split("\\s+");

			GeneralInformation generalInformation = new GeneralInformation(
					Common.my_name, cloud, dateTime[0], dateTime[1]);

			RequestInformation requestInformation = new RequestInformation();
			requestInformation.setCommand("GetObject");
			requestInformation.setService("ObjectStorage");
			requestInformation.addTags("ObjectName", objName);
			requestInformation.addTags("TransferMethod", "Embedded");

			ResponseInformation responseInformation = null;
			GetObject getobject = new GetObject();
			String protocolid = ProtocolUtil.generateID();
			HashMap<String, String> map = new HashMap<>();
			map.put("TransferMethod", "Embedded");
			map.put("ObjectName", objName);
			map.put("DataDigestAlgorithm", "SHA256");
			map.put("ToCloud", cloud);
			map.put("ID", protocolid);
			map.put("Version", versionDate);
			String level = DatabaseUtil.getDataSecurityLevelOwn(objName, cloud);
			if (level.equalsIgnoreCase("private")
					&& !dataSecurity.equalsIgnoreCase("private"))
				throw new DataSecurityLevelUnmatchedException(
						"Unmatched data security level");
			else if (!level.equalsIgnoreCase("private")
					&& dataSecurity.equalsIgnoreCase("private"))
				throw new DataSecurityLevelUnmatchedException(
						"Unmatched data security level");
			map.put("DataSecurity", dataSecurity);
			AdditionalInformation additionalInformation = getobject
					.pre_execute(null, map,
							Common.my_service_providers.get("ObjectStorage"));

			Protocol protocol = new Protocol(Common.ICCP_VER, protocolid,
					generalInformation, requestInformation,
					responseInformation, additionalInformation, null);
			String pro_String = ProtocolUtil.generateRequest(protocol,
					protocolSecurity);

			String ip = Common.my_friends.get(cloud).getIp();
			int port = Common.GW_PORT;

			/**
			 * New Sockets client to send protocol.
			 */
			Sockets socket = new Sockets(ip, port, Common.my_name);

			if (requestInformation != null && responseInformation == null) {
				socket.sendMessage(pro_String);
			}

			return protocolid;
		} catch (Exception e) {
			throw new ObjectStorageAPIException(e.getMessage(), e);
		}
	}

	/**
	 * Retrieve a specific version of object using the other method. Example:
	 * 
	 * <pre>
	 * getVersion(&quot;Duck.jpg&quot;, &quot;HTTPS&quot;, &quot;20110507120303&quot;, false, DATA_SECURITY.PUBLIC);
	 * </pre>
	 * 
	 * retrieves Duck.jpg (version 2011-05-07 12:03:03) from the target cloud
	 * via HTTPS by an unsigned protocol message.
	 * 
	 * @param objName
	 *            The name of object to be retrieved.
	 * @param transferProtocol
	 *            The data transfer method, e.g. HTTPS.
	 * @param versionDate
	 *            The date and time of the version, with format
	 *            "yyyyMMddHHmmss".
	 * @param protocolSecurity
	 *            Whether the protocol message needs to be signed or not.
	 * @param dataSecurity
	 *            The data security level, can either be DATA_SECURITY.PUBLIC,
	 *            DATA_SECURITY.PRIVATE or DATA_SECURITY.SHARED.
	 * @return The protocol ID, used for status checking.
	 * @throws ObjectStorageAPIException
	 */
	public String getVersion(String objName, String transferProtocol,
			String versionDate, boolean protocolSecurity, String dataSecurity)
			throws ObjectStorageAPIException {
		try {
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date date = new Date();
			String[] dateTime = dateFormat.format(date).split("\\s+");

			GeneralInformation generalInformation = new GeneralInformation(
					Common.my_name, cloud, dateTime[0], dateTime[1]);

			RequestInformation requestInformation = new RequestInformation();
			requestInformation.setCommand("GetObject");
			requestInformation.setService("ObjectStorage");
			requestInformation.addTags("ObjectName", objName);
			requestInformation.addTags("TransferMethod", transferProtocol);
			String protocolid = ProtocolUtil.generateID();
			GetObject getobject = new GetObject();
			HashMap<String, String> map = new HashMap<>();
			map.put("TransferMethod", transferProtocol);
			map.put("ObjectName", objName);
			map.put("DataDigestAlgorithm", "SHA256");
			map.put("ID", protocolid);
			map.put("ToCloud", cloud);
			map.put("Version", versionDate);
			String level = DatabaseUtil.getDataSecurityLevelOwn(objName, cloud);
			if (level.equalsIgnoreCase("private")
					&& !dataSecurity.equalsIgnoreCase("private"))
				throw new DataSecurityLevelUnmatchedException(
						"Unmatched data security level");
			else if (!level.equalsIgnoreCase("private")
					&& dataSecurity.equalsIgnoreCase("private"))
				throw new DataSecurityLevelUnmatchedException(
						"Unmatched data security level");

			map.put("DataSecurity", dataSecurity);
			AdditionalInformation additionalInformation = getobject
					.pre_execute(null, map,
							Common.my_service_providers.get("ObjectStorage"));

			ResponseInformation responseInformation = null;
			Protocol protocol = new Protocol(Common.ICCP_VER, protocolid,
					generalInformation, requestInformation,
					responseInformation, additionalInformation, null);

			String pro_String = ProtocolUtil.generateRequest(protocol,
					protocolSecurity);

			String ip = Common.my_friends.get(cloud).getIp();
			int port = Common.GW_PORT;

			/**
			 * New Sockets client to send protocol.
			 */
			Sockets socket = new Sockets(ip, port, Common.my_name);

			if (requestInformation != null && responseInformation == null) {
				socket.sendMessage(pro_String);
			}

			return protocolid;
		} catch (Exception e) {
			throw new ObjectStorageAPIException(e.getMessage(), e);
		}
	}

	/**
	 * Forward an object to another cloud. Example:
	 * 
	 * <pre>
	 * forward(&quot;Sheep.jpg&quot;, &quot;c6.e2.r2.iccp.us&quot;, true, false);
	 * </pre>
	 * 
	 * forwards Sheep.jpg from the target cloud to the receiver cloud at
	 * "c2.e6.r2.iccp.us" by a signed protocol message. Old versions would not
	 * be overwritten.
	 * 
	 * @param objName
	 *            The name of object to be forwarded.
	 * @param receiver
	 *            The name of the Cloud which receives the object.
	 * @param protocolSecurity
	 *            Whether the protocol message needs to be signed or not.
	 * @param overwrite
	 *            Overwrite all previous versions or not.
	 * @return The protocol ID, used for status checking.
	 * @throws ObjectStorageAPIException
	 */
	public String forward(String objName, String receiver,
			boolean protocolSecurity, boolean overwrite)
			throws ObjectStorageAPIException {
		try {
			String protocolid_2 = Long.toString(Long.valueOf(ProtocolUtil
					.generateID()));

			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date date = new Date();
			String[] dateTime = dateFormat.format(date).split("\\s+");

			GeneralInformation generalInformation = new GeneralInformation(
					Common.my_name, cloud, dateTime[0], dateTime[1]);
			// common
			RequestInformation requestInformation = new RequestInformation();
			requestInformation.setCommand("NotificationofForward");
			requestInformation.setService("ObjectStorage");
			requestInformation.addTags("ObjectName", objName);

			NotificationofForward notification = new NotificationofForward();

			// protocol1
			String protocolid = ProtocolUtil.generateID();

			HashMap<String, String> map = new HashMap<>();

			String overwrite_str = null;
			if (overwrite)
				overwrite_str = "true";
			else
				overwrite_str = "false";

			map.put("DataDigestAlgorithm", "SHA256");
			map.put("ForwardCloud", receiver);
			map.put("PermissionID", protocolid_2);
			map.put("Overwrite", overwrite_str);

			AdditionalInformation additionalInformation = notification
					.pre_execute(null, map,
							Common.my_service_providers.get("ObjectStorage"));

			ResponseInformation responseInformation = null;

			Protocol protocol = new Protocol(Common.ICCP_VER, protocolid,
					generalInformation, requestInformation,
					responseInformation, additionalInformation, null);
			// transfer ownership
			String pro_String = ProtocolUtil.generateRequest(protocol,
					protocolSecurity);

			AuthenticationAPI aAPI = new AuthenticationAPI();
			aAPI.checkAuth(receiver);
			Date date_2 = new Date();
			String[] dateTime_2 = dateFormat.format(date_2).split("\\s+");

			GeneralInformation generalInformation_2 = new GeneralInformation(
					Common.my_name, receiver, dateTime_2[0], dateTime_2[1]);

			Protocol protocol_2 = new Protocol(Common.ICCP_VER, protocolid_2,
					generalInformation_2, requestInformation,
					responseInformation, additionalInformation, null);
			// transfer ownership
			String pro_String_2 = ProtocolUtil.generateRequest(protocol_2,
					protocolSecurity);

			String ip_2 = receiver;
			int port_2 = Common.GW_PORT;

			/**
			 * New Sockets client to send protocol.
			 */
			Sockets socket_2 = new Sockets(ip_2, port_2, Common.my_name);

			if (requestInformation != null && responseInformation == null) {
				socket_2.sendMessage(pro_String_2);
			}

			String ip = Common.my_friends.get(cloud).getIp();
			int port = Common.GW_PORT;

			/**
			 * New Sockets client to send protocol.
			 * 
			 */
			Sockets socket = new Sockets(ip, port, Common.my_name);

			if (requestInformation != null && responseInformation == null) {
				socket.sendMessage(pro_String);
			}
			return protocolid;
		} catch (Exception e) {
			throw new ObjectStorageAPIException(e.getMessage(), e);
		}
	}

	/**
	 * Forward multiple objects to another cloud. Example:
	 * 
	 * <pre>
	 * forward(list, &quot;c6.e2.r2.iccp.us&quot;, true, true);
	 * </pre>
	 * 
	 * forwards the list of objects from the target cloud to the receiver cloud
	 * at "c6.e2.r2.iccp.us" by a signed protocol message. All previous versions
	 * would be overwritten.
	 * 
	 * @param objects
	 *            An ArrayList of the names of object to be forwarded.
	 * @param receiver
	 *            The name of the Cloud which receives the object.
	 * @param protocolSecurity
	 *            Whether the protocol message needs to be signed or not.
	 * @return The protocol ID, used for status checking.
	 * @throws ObjectStorageAPIException
	 */
	public String forward(ArrayList<String> objects, String receiver,
			boolean protocolSecurity, boolean overwrite)
			throws ObjectStorageAPIException {
		try {
			String name_str = "";
			for (int i = 0; i < objects.size(); i++) {
				if (i == objects.size() - 1)
					name_str = name_str + objects.get(i);
				else
					name_str = name_str + objects.get(i) + ";";
			}
			return forward(name_str, receiver, protocolSecurity, overwrite);
		} catch (Exception e) {
			throw new ObjectStorageAPIException(e.getMessage(), e);
		}
	}

	/**
	 * Delete an object in the target cloud. Example:
	 * 
	 * <pre>
	 * delete("Horse.jpg", false;
	 * </pre>
	 * 
	 * deletes Horse.jpg in the target cloud by an unsigned message.
	 * 
	 * @param objName
	 *            The name of object to be deleted.
	 * @param protocolSecurity
	 *            Whether the protocol message needs to be signed or not.
	 * @return The protocol ID, used for status checking.
	 * @throws ObjectStorageAPIException
	 */
	public String delete(String objName, boolean protocolSecurity)
			throws ObjectStorageAPIException {
		try {
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date date = new Date();
			String[] dateTime = dateFormat.format(date).split("\\s+");

			GeneralInformation generalInformation = new GeneralInformation(
					Common.my_name, cloud, dateTime[0], dateTime[1]);

			RequestInformation requestInformation = new RequestInformation();
			requestInformation.setCommand("DeleteObject");
			requestInformation.setService("ObjectStorage");
			requestInformation.addTags("ObjectName", objName);

			DeleteObject delete = new DeleteObject();
			String protocolid = ProtocolUtil.generateID();
			HashMap<String, String> map = new HashMap<>();

			map.put("DataDigestAlgorithm", "SHA256");
			map.put("ObjectName", objName);
			map.put("ID", protocolid);
			map.put("ToCloud", cloud);

			AdditionalInformation additionalInformation = delete
					.pre_execute(null, map,
							Common.my_service_providers.get("ObjectStorage"));

			ResponseInformation responseInformation = null;

			Protocol protocol = new Protocol(Common.ICCP_VER, protocolid,
					generalInformation, requestInformation,
					responseInformation, additionalInformation, null);

			String pro_String = ProtocolUtil.generateRequest(protocol,
					protocolSecurity);

			String ip = Common.my_friends.get(cloud).getIp();
			int port = Common.GW_PORT;

			Sockets socket = new Sockets(ip, port, Common.my_name);
			socket.sendMessage(pro_String);

			return protocolid;
		} catch (Exception e) {
			throw new ObjectStorageAPIException(e.getMessage(), e);
		}
	}

	/**
	 * <b>This method is NOT intended to be called externally, or exception will
	 * be thrown.</b>
	 */
	public String forward(String id, String objName, String owner,
			String permissionid, boolean protocolSecurity, String overwrite)
			throws ObjectStorageAPIException {
		blockExternalCall();
		try {
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date date = new Date();
			String[] dateTime = dateFormat.format(date).split("\\s+");

			GeneralInformation generalInformation = new GeneralInformation(
					Common.my_name, cloud, dateTime[0], dateTime[1]);

			RequestInformation requestInformation = new RequestInformation();
			requestInformation.setCommand("ForwardObject");
			requestInformation.setService("ObjectStorage");
			requestInformation.addTags("ObjectName", objName);
			requestInformation.addTags("TransferMethod", "HTTPS");

			ForwardObject ForwardObject = new ForwardObject();
			String protocolid = ProtocolUtil.generateID();
			HashMap<String, String> map = new HashMap<>();
			String[] object_name = objName.split(";");
			String objectName_str = "";
			String data_security_level_str = "";
			String[] data_security_level = new String[object_name.length];
			for (int i = 0; i < object_name.length; i++) {
				data_security_level[i] = DatabaseUtil
						.getDataSecurityLevelOthers(object_name[i], owner);

				String objectName = DatabaseUtil.getStorageNameOthers(
						object_name[i], owner);
				if (i == object_name.length - 1) {
					objectName_str = objectName_str + objectName;
					data_security_level_str = data_security_level_str
							+ data_security_level[i];
				} else {
					objectName_str = objectName_str + objectName + ";";
					data_security_level_str = data_security_level_str
							+ data_security_level[i] + ";";

				}
			}
			map.put("TransferMethod",
					Common.file_transfer_methods.toArray()[0].toString());
			map.put("Encoding", "Base64");
			map.put("DigestAlgorithm", "SHA256");
			map.put("ObjectName", objectName_str);
			map.put("DataSecurity", data_security_level_str);
			map.put("StorageCloud", cloud);
			map.put("PermissionID", permissionid);
			map.put("Owner", owner);
			map.put("Overwrite", overwrite);

			AdditionalInformation additionalInformation = ForwardObject
					.pre_execute(null, map,
							Common.my_service_providers.get("ObjectStorage"));

			ResponseInformation responseInformation = null;

			Protocol protocol = new Protocol(Common.ICCP_VER, protocolid,
					generalInformation, requestInformation,
					responseInformation, additionalInformation, null);

			String pro_String = ProtocolUtil.generateRequest(protocol,
					protocolSecurity);

			/**
			 * New Sockets client to send protocol.
			 */
			Sockets socket = new Sockets(cloud, Common.GW_PORT, Common.my_name);

			if (requestInformation != null && responseInformation == null) {
				socket.sendMessage(pro_String);
			}

			String[] flagContent = new String[2];
			flagContent[0] = "0";

			Common.flag.put(protocolid, flagContent);

			do {
				Thread.sleep(3000);
			} while (Common.flag.get(protocolid)[0].equalsIgnoreCase("0"));
			return protocolid;

		} catch (Exception e) {

			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date date = new Date();
			String[] dateTime = dateFormat.format(date).split("\\s+");

			GeneralInformation generalInformation_2 = new GeneralInformation(
					Common.my_name, owner, dateTime[0], dateTime[1]);

			ExceptionInformation exceptionInformation = new ExceptionInformation();
			exceptionInformation.addTags("Command", "ForwardObject");
			exceptionInformation.addTags("Code", "2");
			exceptionInformation.addTags("Type", "ForwardException");
			exceptionInformation.addTags("Message", "Error in retrival object");

			AdditionalInformation additionalInformation_2 = new AdditionalInformation();

			ExceptionProtocol exception = new ExceptionProtocol(
					Common.ICCP_VER, id, generalInformation_2,
					exceptionInformation, additionalInformation_2);
			String exception_str = ProtocolUtil.generateException(exception);
			Sockets socket = new Sockets(owner, Common.GW_PORT, Common.my_name);

			try {
				socket.sendMessage(exception_str);
			} catch (Exception e1) {
				e1.printStackTrace();
			}

			throw new ObjectStorageAPIException(e.getMessage(), e);

		}
	}

	private void checkMyService() throws ObjectStorageAPIException {
		if (!Common.my_service_providers.containsKey("ObjectStorage")) {
			throw new ObjectStorageAPIException("ObjectStorage"
					+ " is not provided by " + Common.my_name);
		}
	}

	private void blockExternalCall() throws ObjectStorageAPIException {
		String method = new Throwable().getStackTrace()[1].getMethodName();
		String creator = new Throwable().getStackTrace()[2].getClassName();
		if (!creator.startsWith("hk.edu.polyu.intercloud.command.")) {
			throw new ObjectStorageAPIException("The method " + method
					+ " should NOT be invoked by an external application.");
		}
	}

}
