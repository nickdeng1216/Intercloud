package hk.edu.polyu.intercloud.command.objectstorage;

import hk.edu.polyu.intercloud.aws.Download;
import hk.edu.polyu.intercloud.azurestorage.AzureStorageIntercloud;
import hk.edu.polyu.intercloud.command.Command;
import hk.edu.polyu.intercloud.common.Common;
import hk.edu.polyu.intercloud.ctlstorage.CtlStorageForIntercloud;
import hk.edu.polyu.intercloud.exceptions.DatabaseException;
import hk.edu.polyu.intercloud.exceptions.IntercloudException;
import hk.edu.polyu.intercloud.exceptions.NoSuchDataException;
import hk.edu.polyu.intercloud.exceptions.SecurityException;
import hk.edu.polyu.intercloud.exceptions.StorageException;
import hk.edu.polyu.intercloud.gstorage.GoogleStorageIntercloud;
import hk.edu.polyu.intercloud.minio.MinioForIntercloud;
import hk.edu.polyu.intercloud.model.protocol.AdditionalInformation;
import hk.edu.polyu.intercloud.model.protocol.ExceptionInformation;
import hk.edu.polyu.intercloud.model.protocol.ExceptionProtocol;
import hk.edu.polyu.intercloud.model.protocol.GeneralInformation;
import hk.edu.polyu.intercloud.model.protocol.Protocol;
import hk.edu.polyu.intercloud.model.protocol.RequestInformation;
import hk.edu.polyu.intercloud.model.protocol.ResponseInformation;
import hk.edu.polyu.intercloud.security.Digest;
import hk.edu.polyu.intercloud.security.Encryption;
import hk.edu.polyu.intercloud.security.RSA;
import hk.edu.polyu.intercloud.swift.SwiftForIntercloud;
import hk.edu.polyu.intercloud.util.CertificateUtil;
import hk.edu.polyu.intercloud.util.DatabaseUtil;
import hk.edu.polyu.intercloud.util.KeyUtil;
import hk.edu.polyu.intercloud.util.LogUtil;
import hk.edu.polyu.intercloud.util.PropertiesReader;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.xml.sax.SAXException;

/**
 * 
 * @author harry
 *
 */
public class GetObject implements Command {

	private Protocol protocol;
	private GeneralInformation generalInformation;
	private RequestInformation requestInformation;
	private AdditionalInformation additionalInformation;
	private PropertiesReader propertiesUtil;

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
		String cloudType = null;
		String sharedKey = null;
		String esKey = null;
		HashMap<String, String> map = new HashMap<>();
		initialization();

		try {
			cloudType = Common.my_service_providers.get("ObjectStorage");
			propertiesUtil = new PropertiesReader(cloudType);
			String transferMethod = this.requestInformation
					.getValue("TransferMethod");
			String sLevel = this.additionalInformation.getValue("DataSecurity");
			String objectName = this.requestInformation.getValue("ObjectName");
			String[] objects = objectName.split(";");
			String[] localPath = new String[objects.length];
			String[] object_download = new String[objects.length];
			String[] data = new String[objects.length];
			String[] digest = new String[objects.length];
			String bucket = this.propertiesUtil.getBucket();

			// Generate Shared Key
			if (sLevel.equalsIgnoreCase("Shared")) {
				sharedKey = Encryption.makeKeyShared();
				// Encrypt Shared Key
				esKey = enKey(sharedKey, this.generalInformation.getFrom());
			}

			for (int i = 0; i < objects.length; i++) {
				// if the version of object has been specified
				String version = this.additionalInformation.getValue("Version");
				if (version != null) {
					object_download[i] = DatabaseUtil.getLatest(version,
							this.generalInformation.getFrom(), objectName);
					map.put("Version", version);
				} else {
					object_download[i] = DatabaseUtil.findObject(objectName,
							this.additionalInformation.getValue("DataDigest"));
				}

				try {
					if (Arrays.equals(object_download,
							new String[object_download.length])) {
						throw new NoSuchDataException(
								"Object with specified digest cannot be found");
					}
				} catch (NoSuchDataException ex) {
					return generateException("7", "NoSuchDataException",
							"Object with specified digest cannot be found");
				}

				System.out.println("SSS Downloading from storage START ["
						+ System.currentTimeMillis() + "]");
				try {
					if (cloudType.equalsIgnoreCase("amazon")) {
						Download a_download = new Download();
						localPath[i] = a_download.amazondo(object_download[i],
								Common.DOWNLOAD_PATH + object_download[i]);
					} else if (cloudType.equalsIgnoreCase("azure")) {
						AzureStorageIntercloud a = new AzureStorageIntercloud();
						a.downloadFile(bucket, object_download[i],
								Common.DOWNLOAD_PATH + object_download[i]);
						localPath[i] = System.getProperty("user.dir")
								+ File.separator + "download" + File.separator
								+ object_download[i];
					} else if (cloudType.equalsIgnoreCase("googlecloud")) {
						GoogleStorageIntercloud g = new GoogleStorageIntercloud();
						g.downloadFile(bucket, object_download[i],
								Common.DOWNLOAD_PATH);
						localPath[i] = Common.DOWNLOAD_PATH
								+ object_download[i];
					} else if (cloudType.equalsIgnoreCase("centurylink")) {
						localPath[i] = Common.DOWNLOAD_PATH
								+ object_download[i];
						CtlStorageForIntercloud c = new CtlStorageForIntercloud();
						c.getObject(bucket, object_download[i], new File(
								localPath[i]));
					} else if (cloudType.equalsIgnoreCase("minio")) {
						MinioForIntercloud minio = new MinioForIntercloud();
						minio.getObject(bucket, object_download[i],
								Common.DOWNLOAD_PATH + object_download[i]);
						localPath[i] = Common.DOWNLOAD_PATH
								+ object_download[i];
					} else if (cloudType.equalsIgnoreCase("openstack")) {
						SwiftForIntercloud a = new SwiftForIntercloud();
						a.download(bucket, object_download[i],
								Common.DOWNLOAD_PATH + object_download[i]);
						localPath[i] = Common.DOWNLOAD_PATH
								+ object_download[i];
					} else {
						throw new StorageException(
								"Gateway does not support this vendor.");
					}
				} catch (Exception e) {
					throw new StorageException(e.getMessage(), e);
				}
				System.out.println("SSS Downloading from storage END ["
						+ System.currentTimeMillis() + "]");
				if (sLevel.equalsIgnoreCase("Shared")) {
					if (!transferMethod.equalsIgnoreCase("Embedded")) {
						digest[i] = Digest.digestFile(localPath[i]);
						rEnFile(localPath[i], sharedKey);
					}
				} else {
					if (!transferMethod.equalsIgnoreCase("Embedded")) {
						System.out.println("DDD Digest calculation START ["
								+ System.currentTimeMillis() + "]");
						digest[i] = Digest.digestFile(localPath[i]);
						System.out.println("DDD Digest calculation END ["
								+ System.currentTimeMillis() + "]");
					}
				}

				if (transferMethod.equalsIgnoreCase("Embedded")) {
					data[i] = FileUtils.readFileToString(
							new File(localPath[i]), StandardCharsets.UTF_8);
					digest[i] = Digest.digestString(data[i]);
					if (sLevel.equalsIgnoreCase("Shared")) {
						data[i] = rEnString(data[i], sharedKey);
					}
					// Base64 encoding
					byte[] encodeBytes = Base64.getEncoder().encode(
							data[i].getBytes());
					data[i] = new String(encodeBytes);
				} else if (transferMethod.equalsIgnoreCase("HTTP")) {
					localPath[i] = "http://" + Common.my_ip + ":"
							+ Common.HTTP_PORT + "/" + object_download[i];
				} else if (transferMethod.equalsIgnoreCase("HTTPS")) {
					localPath[i] = "https://" + Common.my_ip + ":"
							+ Common.HTTPS_PORT + "/" + object_download[i];
				}
			}

			if (transferMethod.equalsIgnoreCase("Embedded")) {
				StringBuilder dataString = new StringBuilder();
				StringBuilder digestString = new StringBuilder();
				for (int i = 0; i < objects.length; i++) {
					dataString.append(data[i]).append(";");
					digestString.append(digest[i]).append(";");
				}
				map.put("Data",
						dataString.toString().substring(0,
								dataString.length() - 1));
				map.put("DataDigest",
						digestString.toString().substring(0,
								digestString.length() - 1));
			} else {
				StringBuilder pathString = new StringBuilder();
				StringBuilder digestString = new StringBuilder();
				for (int i = 0; i < objects.length; i++) {
					digestString.append(digest[i]).append(";");
				}
				map.put("IP", Common.my_ip);
				if (transferMethod.equalsIgnoreCase("FTP")) {
					map.put("Port", Integer.toString(Common.FTP_PORT));
				} else if (transferMethod.equalsIgnoreCase("FTPS")) {
					map.put("Port", Integer.toString(Common.FTPS_PORT));
				} else if (transferMethod.equalsIgnoreCase("UDT")) {
					map.put("Port", Integer.toString(Common.UDT_PORT));
				} else if (transferMethod.equalsIgnoreCase("HTTP")) {
					map.put("Port", Integer.toString(Common.HTTP_PORT));
				} else if (transferMethod.equalsIgnoreCase("HTTPS")) {
					map.put("Port", Integer.toString(Common.HTTPS_PORT));
				}
				for (int i = 0; i < objects.length; i++) {
					pathString.append(localPath[i]).append(";");
				}
				map.put("Path",
						pathString.toString().substring(0,
								pathString.length() - 1));
				map.put("DataDigest",
						digestString.toString().substring(0,
								digestString.length() - 1));
			}
			map.put("DataSecurity", sLevel);
			map.put("Encoding", "Base64");
			if (sLevel.equalsIgnoreCase("Shared")) {
				map.put("SharedKey", esKey);
			}
			return this.generateProtocol(map, transferMethod);
		} catch (StorageException e) {
			LogUtil.logException(e);
			return this.generateException("3",
					StorageException.class.getSimpleName(), e.getMessage());
		} catch (IOException e) {
			LogUtil.logException(e);
			return this.generateException("0",
					IntercloudException.class.getSimpleName(), e.getMessage());
		} catch (SecurityException e) {
			LogUtil.logException(e);
			return this.generateException("103",
					SecurityException.class.getSimpleName(), e.getMessage());
		} catch (ClassNotFoundException | SQLException | ParseException e) {
			LogUtil.logException(e);
			return this.generateException("2",
					DatabaseException.class.getSimpleName(), e.getMessage());
		}
	}

	@Override
	public AdditionalInformation pre_execute(String protocol,
			HashMap<String, String> info, String systemType)
			throws ParserConfigurationException, SAXException, IOException,
			Exception {
		AdditionalInformation additional = new AdditionalInformation();
		additional.addTags("DataSecurity", info.get("DataSecurity"));
		additional.addTags("DataDigestAlgorithm",
				info.get("DataDigestAlgorithm"));
		if (info.containsKey("Version")) {
			additional.addTags("Version", info.get("Version"));
			additional.addTags(
					"DataDigest",
					DatabaseUtil.getLatestDigest(info.get("ObjectName"),
							info.get("ToCloud"), info.get("Version")));
		} else {
			additional.addTags(
					"DataDigest",
					DatabaseUtil.getDigestOwn(info.get("ObjectName"),
							info.get("ToCloud")));
		}
		// Update os_own_objects last get (user should add 'id' into Map in the
		// Main.java)
		DatabaseUtil.updateLastGetAttemptOwn(info.get("ID"));
		return additional;
	}

	@Override
	public void initialization() {
		this.generalInformation = protocol.getGeneralInformation();
		this.requestInformation = protocol.getRequestInformation();
		this.additionalInformation = protocol.getAdditionalInformation();
	}

	/**
	 * Encrypt shared key
	 * 
	 * @param rKey
	 * @param objName
	 * @return
	 * @throws SecurityException
	 * @throws IOException
	 * @throws Exception
	 */
	private String enKey(String key, String fromCloud)
			throws SecurityException, IOException {
		String keyPath = System.getProperty("user.dir") + "/Key/Others/"
				+ fromCloud + ".pem";
		byte b[] = RSA.encrypt(CertificateUtil.getPublicKey(keyPath),
				key.getBytes());
		String key1 = CertificateUtil.encryptBASE64(b);
		KeyUtil.writefile(System.getProperty("user.dir")
				+ "/Key/Others/temp.txt", key1);
		String key2 = CertificateUtil.readfileinbase64(System
				.getProperty("user.dir") + "/Key/Others/temp.txt");
		return key2;
	}

	/**
	 * Used for shared random encryption.
	 * 
	 * @param filePath
	 * @return
	 * @throws SecurityException
	 * @throws IOException
	 */
	private String rEnFile(String filePath, String password)
			throws IOException, SecurityException {
		String encryptPath = filePath + "_enc";
		BufferedInputStream is = new BufferedInputStream(new FileInputStream(
				filePath));
		BufferedOutputStream os = new BufferedOutputStream(
				new FileOutputStream(encryptPath));
		Encryption.encrypt(is, os, password);
		new File(filePath).delete();
		FileUtils.moveFile(new File(encryptPath), new File(filePath));
		return filePath;
	}

	/**
	 * Used for shared random encryption of string.
	 * 
	 * @param msg
	 * @return
	 * @throws SecurityException
	 * @throws IOException
	 */
	private String rEnString(String msg, String password) throws IOException,
			SecurityException {
		StringBuilder strBuffer = new StringBuilder();
		ByteArrayInputStream bis = new ByteArrayInputStream(
				msg.getBytes("UTF8"));
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		Encryption.encrypt(bis, bos, password);
		byte b[] = bos.toByteArray();
		for (int i = 0; i < b.length; i++) {
			strBuffer.append(Integer.toString((b[i] & 0xff) + 0x100, 16)
					.substring(1));
		}
		return strBuffer.toString();
	}

	private Protocol generateProtocol(HashMap<String, String> map,
			String transferMethod) {

		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date date = new Date();
		String[] dateTime = dateFormat.format(date).split("\\s+");

		GeneralInformation generalInformation = new GeneralInformation(
				this.protocol.getGeneralInformation().getTo(), this.protocol
						.getGeneralInformation().getFrom(), dateTime[0],
				dateTime[1]);

		ResponseInformation responseInformation = new ResponseInformation();
		responseInformation.setCommand("ConfirmationForGet");
		responseInformation.setService("ObjectStorage");
		responseInformation.addTags("ObjectName",
				this.requestInformation.getValue("ObjectName"));
		responseInformation.addTags("TransferMethod",
				this.requestInformation.getValue("TransferMethod"));

		AdditionalInformation additionalInformation = new AdditionalInformation();
		if (transferMethod.equalsIgnoreCase("Embedded")) {
			additionalInformation.addTags("Data", map.get("Data"));
			additionalInformation.addTags("Encoding", map.get("Encoding"));
		} else if (transferMethod.equalsIgnoreCase("UDT")) {
			additionalInformation.addTags("IP", map.get("IP"));
			additionalInformation.addTags("Port", map.get("Port"));
			additionalInformation.addTags("Path", map.get("Path"));
			additionalInformation.addTags("Encoding", map.get("Encoding"));
		} else if (transferMethod.equalsIgnoreCase("FTPS")
				|| transferMethod.equalsIgnoreCase("FTP")) {
			additionalInformation.addTags("IP", map.get("IP"));
			additionalInformation.addTags("Port", map.get("Port"));
			additionalInformation.addTags("Path", map.get("Path"));
			additionalInformation.addTags("Encoding", map.get("Encoding"));
		} else if (transferMethod.equalsIgnoreCase("HTTP")
				|| transferMethod.equalsIgnoreCase("HTTPS")) {
			additionalInformation.addTags("IP", map.get("IP"));
			additionalInformation.addTags("Port", map.get("Port"));
			additionalInformation.addTags("Path", map.get("Path"));
			additionalInformation.addTags("Encoding", map.get("Encoding"));
		}

		if (map.containsKey("SharedKey"))
			additionalInformation.addTags("SharedKey", map.get("SharedKey"));
		if (map.containsKey("Version")) {
			additionalInformation.addTags("Version", map.get("Version"));
		}

		additionalInformation.addTags("DataDigestAlgorithm", "SHA256");
		additionalInformation.addTags("DataDigest", map.get("DataDigest"));
		additionalInformation.addTags("DataSecurity", map.get("DataSecurity"));

		String body = null;

		Protocol protocol = new Protocol(this.protocol.getProtocolVersion(),
				this.protocol.getId(), generalInformation, null,
				responseInformation, additionalInformation, body);

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
		exceptionInformation.addTags("Command", "GetObject");
		exceptionInformation.addTags("Code", code);
		exceptionInformation.addTags("Type", type);
		exceptionInformation.addTags("Message", message);

		AdditionalInformation additionalInformation = new AdditionalInformation();

		return new ExceptionProtocol(this.protocol.getProtocolVersion(),
				this.protocol.getId(), generalInformation,
				exceptionInformation, additionalInformation);
	}
}
