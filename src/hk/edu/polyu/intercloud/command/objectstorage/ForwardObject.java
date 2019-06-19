package hk.edu.polyu.intercloud.command.objectstorage;

import hk.edu.polyu.intercloud.aws.Delete;
import hk.edu.polyu.intercloud.aws.Download;
import hk.edu.polyu.intercloud.aws.Upload;
import hk.edu.polyu.intercloud.azurestorage.AzureStorageIntercloud;
import hk.edu.polyu.intercloud.command.Command;
import hk.edu.polyu.intercloud.common.Common;
import hk.edu.polyu.intercloud.ctlstorage.CtlStorageForIntercloud;
import hk.edu.polyu.intercloud.exceptions.DataVerificationException;
import hk.edu.polyu.intercloud.exceptions.DatabaseException;
import hk.edu.polyu.intercloud.exceptions.IntercloudException;
import hk.edu.polyu.intercloud.exceptions.SecurityException;
import hk.edu.polyu.intercloud.exceptions.StorageException;
import hk.edu.polyu.intercloud.exceptions.UnsupportedTransferMethodException;
import hk.edu.polyu.intercloud.fileserver.client.Ftp;
import hk.edu.polyu.intercloud.fileserver.client.Http;
import hk.edu.polyu.intercloud.fileserver.client.Udt;
import hk.edu.polyu.intercloud.fileserver.exceptions.FtpException;
import hk.edu.polyu.intercloud.fileserver.exceptions.HttpException;
import hk.edu.polyu.intercloud.fileserver.exceptions.UdtException;
import hk.edu.polyu.intercloud.gstorage.GoogleStorageIntercloud;
import hk.edu.polyu.intercloud.minio.MinioForIntercloud;
import hk.edu.polyu.intercloud.model.protocol.AdditionalInformation;
import hk.edu.polyu.intercloud.model.protocol.ExceptionInformation;
import hk.edu.polyu.intercloud.model.protocol.ExceptionProtocol;
import hk.edu.polyu.intercloud.model.protocol.GeneralInformation;
import hk.edu.polyu.intercloud.model.protocol.Protocol;
import hk.edu.polyu.intercloud.model.protocol.RequestInformation;
import hk.edu.polyu.intercloud.model.protocol.ResponseInformation;
import hk.edu.polyu.intercloud.security.Decryption;
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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.crypto.Cipher;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.xml.sax.SAXException;

/**
 * Forward class
 * 
 * @author Kate.xie
 *
 */
public class ForwardObject implements Command {

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
		try {
			String cloudType = null;
			initialization();
			cloudType = Common.my_service_providers.get("ObjectStorage");
			propertiesUtil = new PropertiesReader(cloudType);

			// Digest of file uploaded to cloud
			// Check how many files will be forwarded
			String objectName = this.requestInformation.getValue("ObjectName");
			String[] object_name = objectName.split(";");
			String path = this.additionalInformation.getValue("Path");
			String[] path_result = path.split(";");
			String[] baseName = new String[object_name.length];
			String[] digest_uploadFile = new String[object_name.length];
			String[] extension = new String[object_name.length];
			String[] sLevel = this.additionalInformation.getValue(
					"DataSecurity").split(";");
			String[] path_uploadFile = new String[object_name.length];
			String[] datadigest = this.additionalInformation.getValue(
					"DataDigest").split(";");

			// Check permission
			String owner = this.additionalInformation.getValue("Owner");
			String permission_id = this.additionalInformation
					.getValue("PermissionID");
			String result = DatabaseUtil.getReqTrack(Long
					.parseLong(permission_id));
			String digest_uploadFile_str = "";
			String overwrite = this.additionalInformation.getValue("Overwrite");
			String bucket = propertiesUtil.getBucket();
			String transferMethod = this.requestInformation
					.getValue("TransferMethod");
			// Check necessity of upload
			boolean contain = false;

			boolean permission = false;
			if (result != null)
				permission = true;

			int i = 0;
			do {
				i++;
				Thread.sleep(2000);
			} while (DatabaseUtil.getReqTrack(Long.parseLong(permission_id)) == null
					&& i <= 3);

			if (result != null)
				permission = true;

			if (!permission)
				return this.generateException("103",
						SecurityException.class.getSimpleName(),
						"No permission.");
			// Multiple files transfer
			for (int j = 0; j < object_name.length; j++) {
				baseName[j] = FilenameUtils.getBaseName(path_result[j]);
				extension[j] = FilenameUtils.getExtension(path_result[j]);
				contain = DatabaseUtil.checkDuplicateOthersAll(datadigest[j]);
				if (!contain) {
					// After retrieving, the local path of file

					System.out.print(object_name[j]);
					path_uploadFile[j] = fileRetrieve(transferMethod,
							path_result[j], object_name[j], sLevel[j],
							datadigest[j]);

					// Digest on upload file
					digest_uploadFile[j] = Digest
							.digestFile(path_uploadFile[j]);

					// Upload file to cloud
					try {
						if (cloudType.equalsIgnoreCase("amazon")) {
							Upload upload = new Upload();
							upload.amazondo(path_uploadFile[j]);
						} else if (cloudType.equalsIgnoreCase("azure")) {
							AzureStorageIntercloud azure = new AzureStorageIntercloud();
							azure.uploadFile(bucket, path_uploadFile[j],
									baseName[j] + "." + extension[j], false);
						} else if (cloudType.equalsIgnoreCase("centurylink")) {
							CtlStorageForIntercloud c = new CtlStorageForIntercloud();
							c.putObject(bucket,
									baseName[j] + "." + extension[j] + "_"
											+ this.protocol.getId(), new File(
											path_uploadFile[j]));
						} else if (cloudType.equalsIgnoreCase("googlecloud")) {
							GoogleStorageIntercloud g = new GoogleStorageIntercloud();
							g.uploadFile(bucket, path_uploadFile[j]);
						} else if (cloudType.equalsIgnoreCase("minio")) {
							MinioForIntercloud minio = new MinioForIntercloud();
							minio.putObject(path_uploadFile[j], bucket,
									digest_uploadFile[j]);
						} else if (cloudType.equalsIgnoreCase("opensatck")) {
							SwiftForIntercloud a = new SwiftForIntercloud();
							a.upload(bucket, path_uploadFile[j]);
						}
					} catch (Exception e) {
						throw new StorageException(e.getMessage(), e);
					}

					// this method is used to judge whether delete older version
					overwrite(overwrite, object_name[j], owner, cloudType,
							datadigest[j], contain, path_result[j],
							path_uploadFile[j]);

					// DB Insert
					DatabaseUtil.insertOthersObjectTable(this.protocol.getId(),
							datadigest[j], owner, object_name[j], " ",
							digest_uploadFile[j], sLevel[j]);

					if (j == object_name.length - 1) {
						digest_uploadFile_str = digest_uploadFile_str
								+ digest_uploadFile[j];

					} else {
						digest_uploadFile_str = digest_uploadFile_str
								+ digest_uploadFile[j] + ";";

					}

				} else {
					// No need to upload file
					String uploaded_name = DatabaseUtil
							.getDuplicateNamesOthersAll(datadigest[j]);
					if (DatabaseUtil.checkDuplicateOthers(datadigest[j], owner)) {
						overwrite(overwrite, object_name[j], owner, cloudType,
								datadigest[j], contain, path_result[j],
								path_uploadFile[j]);
						DatabaseUtil.insertOthersObjectTable(
								this.protocol.getId(), uploaded_name, owner,
								object_name[j], " ", datadigest[j], sLevel[j]);

						System.out.println("Duplicated file");
					} else {
						overwrite(overwrite, object_name[j], owner, cloudType,
								datadigest[j], contain, path_result[j],
								path_uploadFile[j]);
						DatabaseUtil.insertOthersObjectTable(
								this.protocol.getId(), uploaded_name, owner,
								object_name[j], " ", datadigest[j], sLevel[j]);
					}
				}
			}
			// Return response protocol
			if (!contain)
				return generateProtocol(digest_uploadFile_str);
			else
				return generateProtocol(null);
		} catch (StorageException e) {
			LogUtil.logException(e);
			return this.generateException("3",
					StorageException.class.getSimpleName(), e.getMessage());
		} catch (ClassNotFoundException | SQLException | ParseException e) {
			LogUtil.logException(e);
			return this.generateException("2",
					DatabaseException.class.getSimpleName(), e.getMessage());
		} catch (InterruptedException e) {
			LogUtil.logException(e);
			return this.generateException("0",
					IntercloudException.class.getSimpleName(), e.getMessage());
		} catch (SecurityException e) {
			LogUtil.logException(e);
			return this.generateException("103",
					SecurityException.class.getSimpleName(), e.getMessage());
		} catch (UdtException | FtpException | HttpException e) {
			LogUtil.logException(e);
			return this.generateException("0",
					IntercloudException.class.getSimpleName(), e.getMessage());
		} catch (IOException e) {
			LogUtil.logException(e);
			return this.generateException("0",
					IntercloudException.class.getSimpleName(), e.getMessage());
		} catch (DataVerificationException e) {
			LogUtil.logException(e);
			return this.generateException("6",
					DataVerificationException.class.getSimpleName(),
					"Retrieved object is wrong.");
		} catch (InvalidKeyException e) {
			LogUtil.logException(e);
			return this.generateException("0",
					SecurityException.class.getSimpleName(), e.getMessage());
		} catch (URISyntaxException e) {
			LogUtil.logException(e);
			return this.generateException("0",
					IntercloudException.class.getSimpleName(), e.getMessage());
		}
	}

	/**
	 * In this command, pre_execute is used for downloading object before
	 * sending protocol
	 */
	@Override
	public AdditionalInformation pre_execute(String protocol,
			HashMap<String, String> info, String cloudType)
			throws ParserConfigurationException, SAXException, IOException,
			Exception {

		propertiesUtil = new PropertiesReader(cloudType);
		String bucket = propertiesUtil.getBucket();

		String id = info.get("ID");
		String owner = info.get("Owner");
		String storageCloud = info.get("StorageCloud");
		String sharedKey = Encryption.makeKeyShared();

		String sLevel_str = info.get("DataSecurity");
		String objectName = info.get("ObjectName");
		String[] objects = objectName.split(";");

		String tsfMethod = info.get("TransferMethod");

		AdditionalInformation additional = new AdditionalInformation();
		String[] path_result = new String[objects.length];
		String[] data = new String[objects.length];
		String[] digest = new String[objects.length];
		String[] path_result_modify = new String[objects.length];
		for (int i = 0; i < objects.length; i++) {
			if (cloudType.equalsIgnoreCase("amazon")) {
				Download download = new Download();
				path_result[i] = download.amazondo(objects[i],
						Common.DOWNLOAD_PATH + objects[i]);
				fileTest(path_result[i]);
			} else if (cloudType.equalsIgnoreCase("azure")) {
				AzureStorageIntercloud a = new AzureStorageIntercloud();
				path_result[i] = a.downloadFile(bucket, objects[i],
						Common.DOWNLOAD_PATH + objects[i]);
				fileTest(path_result[i]);
			} else if (cloudType.equalsIgnoreCase("googlecloud")) {
				GoogleStorageIntercloud g = new GoogleStorageIntercloud();
				path_result[i] = g.downloadFile(bucket, objects[i],
						Common.DOWNLOAD_PATH);
				fileTest(path_result[i]);
			} else if (cloudType.equalsIgnoreCase("centurylink")) {
				CtlStorageForIntercloud c = new CtlStorageForIntercloud();
				path_result[i] = Common.DOWNLOAD_PATH + objects[i];
				c.getObject(bucket, objects[i], new File(path_result[i]));
			} else if (cloudType.equalsIgnoreCase("minio")) {
				MinioForIntercloud minio = new MinioForIntercloud();
				minio.getObject(bucket, objects[i], Common.DOWNLOAD_PATH
						+ objects[i]);
				path_result[i] = Common.DOWNLOAD_PATH + objects[i];
			} else if (cloudType.equalsIgnoreCase("openstack")) {
				SwiftForIntercloud a = new SwiftForIntercloud();
				a.download(bucket, objects[i], System.getProperty("user.dir")
						+ "/download/" + objects[i]);

				path_result[i] = Common.DOWNLOAD_PATH + objects[i];
			}

		}

		// Judge data security level
		String[] sLevel = sLevel_str.split(";");
		for (int i = 0; i < sLevel.length; i++) {
			if (sLevel[i].equalsIgnoreCase("Public")) {
				// Judge different transfer methods
				if (tsfMethod.equalsIgnoreCase("FTPS")
						|| tsfMethod.equalsIgnoreCase("FTP")
						|| tsfMethod.equalsIgnoreCase("UDT")
						|| tsfMethod.equalsIgnoreCase("HTTP")
						|| tsfMethod.equalsIgnoreCase("HTTPS")) {
					// Digest of file
					digest[i] = Digest.digestFile(path_result[i]);
					if (tsfMethod.equalsIgnoreCase("FTP")) {
						additional.addTags("Port",
								String.valueOf(Common.FTP_PORT));
					} else if (tsfMethod.equalsIgnoreCase("FTPS")) {
						additional.addTags("Port",
								String.valueOf(Common.FTPS_PORT));
					} else if (tsfMethod.equalsIgnoreCase("UDT")) {
						additional.addTags("Port",
								String.valueOf(Common.UDT_PORT));
					} else if (tsfMethod.equalsIgnoreCase("HTTP")) {
						path_result[i] = "http://" + Common.my_ip + ":"
								+ Common.HTTP_PORT + "/" + objects[i];
						additional.addTags("Port",
								String.valueOf(Common.HTTP_PORT));
					} else if (tsfMethod.equalsIgnoreCase("HTTPS")) {
						path_result[i] = "https://" + Common.my_ip + ":"
								+ Common.HTTPS_PORT + "/" + objects[i];
						additional.addTags("Port",
								String.valueOf(Common.HTTPS_PORT));
					}

				} else {
					throw new UnsupportedTransferMethodException(
							"Gateway dose not support this trasfer method");
				}
			} else if (sLevel[i].equalsIgnoreCase("Private")) {

				if (tsfMethod.equalsIgnoreCase("FTPS")
						|| tsfMethod.equalsIgnoreCase("FTP")
						|| tsfMethod.equalsIgnoreCase("UDT")
						|| tsfMethod.equalsIgnoreCase("HTTP")
						|| tsfMethod.equalsIgnoreCase("HTTPS")) {
					// Encrypt downloaded file
					path_result[i] = encryptFile(path_result[i]);
					digest[i] = Digest.digestFile(path_result[i]);

					if (tsfMethod.equalsIgnoreCase("FTP")) {
						additional.addTags("Port",
								String.valueOf(Common.FTP_PORT));
					} else if (tsfMethod.equalsIgnoreCase("FTPS")) {
						additional.addTags("Port",
								String.valueOf(Common.FTPS_PORT));
					} else if (tsfMethod.equalsIgnoreCase("UDT")) {
						additional.addTags("Port",
								String.valueOf(Common.UDT_PORT));
					} else if (tsfMethod.equalsIgnoreCase("HTTP")) {
						path_result[i] = "http://" + Common.my_ip + ":"
								+ Common.HTTP_PORT + "/" + objects[i];
						additional.addTags("Port",
								String.valueOf(Common.HTTP_PORT));
					} else if (tsfMethod.equalsIgnoreCase("HTTPS")) {
						path_result[i] = "https://" + Common.my_ip + ":"
								+ Common.HTTPS_PORT + "/" + objects[i];
						additional.addTags("Port",
								String.valueOf(Common.HTTPS_PORT));
					}

				} else {
					throw new UnsupportedTransferMethodException(
							"Gateway dose not support this trasfer method");
				}
			} else if (sLevel[i].equalsIgnoreCase("Shared")) {
				// Generate random key

				if (tsfMethod.equalsIgnoreCase("FTPS")
						|| tsfMethod.equalsIgnoreCase("FTP")
						|| tsfMethod.equalsIgnoreCase("UDT")
						|| tsfMethod.equalsIgnoreCase("HTTP")
						|| tsfMethod.equalsIgnoreCase("HTTPS")) {
					// Digest of downloaded file
					digest[i] = Digest.digestFile(path_result[i]);
					// Then encrypt this file
					path_result[i] = rEnFile(path_result[i], sharedKey);

					if (tsfMethod.equalsIgnoreCase("FTP")) {
						additional.addTags("Port",
								String.valueOf(Common.FTP_PORT));
					} else if (tsfMethod.equalsIgnoreCase("FTPS")) {
						additional.addTags("Port",
								String.valueOf(Common.FTPS_PORT));
					} else if (tsfMethod.equalsIgnoreCase("UDT")) {
						additional.addTags("Port",
								String.valueOf(Common.UDT_PORT));
					} else if (tsfMethod.equalsIgnoreCase("HTTP")) {
						path_result[i] = "http://" + Common.my_ip + ":"
								+ Common.HTTP_PORT + "/" + objects[i];
						additional.addTags("Port",
								String.valueOf(Common.HTTP_PORT));
					} else if (tsfMethod.equalsIgnoreCase("HTTPS")) {
						path_result[i] = "https://" + Common.my_ip + ":"
								+ Common.HTTPS_PORT + "/" + objects[i];
						additional.addTags("Port",
								String.valueOf(Common.HTTPS_PORT));
					}

				} else {
					throw new UnsupportedTransferMethodException(
							"Gateway dose not support this trasfer method");
				}
				// Encrypt random key
				String sKey = enKey(sharedKey, storageCloud);
				additional.addTags("SharedKey", sKey);
			}

		}

		String path_str = "";
		String digest_str = "";
		for (int i = 0; i < objects.length; i++) {

			if (i == objects.length - 1) {
				path_str = path_str + path_result[i];
				digest_str = digest_str + digest[i];
			} else {
				path_str = path_str + path_result[i] + ";";
				digest_str = digest_str + digest[i] + ";";
			}
		}

		additional.addTags("DataSecurity", sLevel_str);
		additional.addTags("Encoding", info.get("Encoding"));
		additional.addTags("DataDigestAlgorithm", info.get("DigestAlgorithm"));
		additional.addTags("DataDigest", digest_str);
		additional.addTags("PermissionID", info.get("PermissionID"));
		additional.addTags("Overwrite", info.get("Overwrite"));
		additional.addTags("Owner", owner);
		additional.addTags("Path", path_str);
		additional.addTags("IP", Common.my_ip);
		return additional;
	}

	@Override
	public void initialization() {
		this.generalInformation = protocol.getGeneralInformation();
		this.requestInformation = protocol.getRequestInformation();
		this.additionalInformation = protocol.getAdditionalInformation();
	}

	/**
	 * Encrypt file in filePath
	 * 
	 * @param filePath
	 * @return
	 * @throws Exception
	 */
	private String encryptFile(String filePath) throws Exception {
		String encryptPath = filePath + "_enc";
		String keyPath = System.getProperty("user.dir") + "/Key/private.pem";
		Encryption.copy(Cipher.ENCRYPT_MODE, filePath, encryptPath, keyPath);
		new File(filePath).delete();
		FileUtils.moveFile(new File(encryptPath), new File(filePath));
		return filePath;
	}

	/**
	 * Used for shared random encryption.
	 * 
	 * @param filePath
	 * @return
	 * @throws Exception
	 */
	private String rEnFile(String filePath, String password) throws Exception {
		BufferedInputStream is = new BufferedInputStream(new FileInputStream(
				filePath));
		BufferedOutputStream os = new BufferedOutputStream(
				new FileOutputStream(filePath + "_enc"));
		Encryption.encrypt(is, os, password);
		new File(filePath).delete();
		FileUtils.moveFile(new File(filePath + "_enc"), new File(filePath));
		return filePath;
	}

	/**
	 * Decrypt shared file
	 * 
	 * @param filePath
	 * @param password
	 * @return
	 * @throws IOException
	 * @throws SecurityException
	 * @throws Exception
	 */
	private String rDeFile(String filePath, String password, String digest)
			throws IOException, SecurityException {
		// String name = FilenameUtils.getName(filePath);
		String outFile = Common.RETRIEVE_PATH + digest;

		BufferedInputStream is = new BufferedInputStream(new FileInputStream(
				filePath));
		BufferedOutputStream os = new BufferedOutputStream(
				new FileOutputStream(outFile + "_dec"));
		Decryption.decrypt(is, os, password);
		new File(filePath).delete();
		FileUtils.moveFile(new File(outFile + "_dec"), new File(filePath));
		return outFile;
	}

	/**
	 * Used for check of downlaoded file
	 * 
	 * @param path_result
	 * @throws InterruptedException
	 */
	private void fileTest(String path_result) throws InterruptedException {
		File sourceFile = new File(path_result);

		while (!sourceFile.exists()) {
			Thread.sleep(500);
		}
		long size = sourceFile.length();
		long newSize = Long.MAX_VALUE;
		while (size < newSize) {
			System.out.println("downloading...");
			size = sourceFile.length();
			Thread.sleep(500);
			newSize = sourceFile.length();
		}
		System.out.println("File Downloaded");
		System.out.println("Size: " + newSize);

	}

	/**
	 * When cloud accepts "PutObject", start retrieving objects.
	 * 
	 * @param transferProtocol
	 * @return
	 * @throws UdtException
	 * @throws FtpException
	 * @throws HttpException
	 * @throws SecurityException
	 * @throws IOException
	 * @throws DataVerificationException
	 * @throws Exception
	 */
	private String fileRetrieve(String transferProtocol, String path,
			String objectname, String sLevel, String digest)
			throws UdtException, FtpException, HttpException, IOException,
			SecurityException, DataVerificationException {
		// Target IP and Port
		String ip = this.additionalInformation.getValue("IP");
		int port = Integer
				.parseInt(this.additionalInformation.getValue("Port"));
		String tmpPath;

		// Retrieve to tmpPath
		tmpPath = Common.RETRIEVE_PATH + digest;

		// Download times
		int i = 10;
		// Start retrieving
		File file = new File(tmpPath);
		if (file.exists() && file.isFile()) {
			FileUtils.forceDelete(file);
		}
		do {
			if (transferProtocol.equalsIgnoreCase("UDT")) {
				Udt.download(ip, port, path, tmpPath);
			} else if (transferProtocol.equalsIgnoreCase("FTPS")) {
				// path[j] = FilenameUtils.getName(path_result[j]);
				String username = "intercloud";
				String password = "p@ssw0rd";
				boolean activeMode = false;
				boolean anonymous = false;
				boolean ssl = true;
				Ftp.download(ip, port, username, password, path, tmpPath,
						activeMode, anonymous, ssl);
			} else if (transferProtocol.equalsIgnoreCase("FTP")) {
				// path = FilenameUtils.getName(path[]);
				String username = "intercloud";
				String password = "p@ssw0rd";
				boolean activeMode = true;
				boolean anonymous = true;
				boolean ssl = false;
				Ftp.download(ip, port, username, password, path, tmpPath,
						activeMode, anonymous, ssl);
			} else if (transferProtocol.equalsIgnoreCase("HTTP")
					|| transferProtocol.equalsIgnoreCase("HTTPS")) {
				Http.download(path, tmpPath);
			}
			// Decrement of download times
			i--;
			// If security level is "Shared", decrypt before doing digest
			if (sLevel.equalsIgnoreCase("Shared")) {
				String deKey = deKey(this.additionalInformation
						.getValue("SharedKey"));
				tmpPath = rDeFile(tmpPath, deKey, digest);
			}
		} while (!Digest.digestFile(tmpPath).equalsIgnoreCase(digest) && i >= 0);

		if (i < 0) {
			throw new DataVerificationException("Retrieved Object is wrong");
		}

		return tmpPath;
	}

	/**
	 * Generate response protocol
	 * 
	 * @param digest
	 * @param security_tags
	 * @return
	 * @throws Exception
	 */
	private Protocol generateProtocol(String digest) {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date date = new Date();
		String[] dateTime = dateFormat.format(date).split("\\s+");
		GeneralInformation generalInformation = new GeneralInformation(
				this.protocol.getGeneralInformation().getTo(), this.protocol
						.getGeneralInformation().getFrom(), dateTime[0],
				dateTime[1]);

		RequestInformation requestInformation = null;

		ResponseInformation responseInformation = new ResponseInformation();
		responseInformation.setCommand("ConfirmationForForward");
		responseInformation.setService("ObjectStorage");
		responseInformation.addTags("ObjectName",
				this.requestInformation.getValue("ObjectName"));

		AdditionalInformation additionalInformation = new AdditionalInformation();

		if (digest != null) {
			additionalInformation.addTags("DataDigest", digest);
			additionalInformation.addTags("DataDigestAlgorithm", "SHA256");
		}

		Protocol protocol = new Protocol(this.protocol.getProtocolVersion(),
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
		exceptionInformation.addTags("Command", "ForwardObject");
		exceptionInformation.addTags("Code", code);
		exceptionInformation.addTags("Type", type);
		exceptionInformation.addTags("Message", message);

		AdditionalInformation additionalInformation = new AdditionalInformation();

		return new ExceptionProtocol(this.protocol.getProtocolVersion(),
				this.protocol.getId(), generalInformation,
				exceptionInformation, additionalInformation);
	}

	private String enKey(String key, String toCloud) throws SecurityException,
			IOException {
		String keyPath = System.getProperty("user.dir") + "/Key/Others/"
				+ toCloud + ".pem";
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
	 * Decrypt shared key
	 * 
	 * @param key
	 * @param objectName
	 * @return
	 * @throws IOException
	 * @throws SecurityException
	 * @throws Exception
	 */
	private String deKey(String key) throws IOException, SecurityException {
		String plainText = null;
		byte[] data = CertificateUtil.decryptBASE64(key);
		String keyPath = System.getProperty("user.dir") + "/Key/private.pem";
		byte[] p = RSA.decrypt(CertificateUtil.getPrivateKey(keyPath), data);
		plainText = new String(p, "UTF-8");
		return plainText;
	}

	// overwrite method
	private void overwrite(String overwrite, String objectname, String owner,
			String cloudType, String digest, boolean contain,
			String path_result, String path_uploadFile)
			throws InvalidKeyException, ClassNotFoundException, SQLException,
			IOException, URISyntaxException, StorageException {
		// whether has older version
		if (DatabaseUtil.checkDuplicateByCloud(objectname, owner)) {
			// delete older version object condition: 1.overwrite==true and
			// 2.only one;
			// cloud owns this object
			if (overwrite.equalsIgnoreCase("true")) {

				String bucket = propertiesUtil.getBucket();
				// List name =new ArrayList();
				List<String> name = DatabaseUtil.getOldVersionObjectName(
						objectname, owner);
				for (int i = 0; i < name.size(); i++) {
					if (DatabaseUtil.countOwnership(name.get(i), owner) == 0) {
						if (!DatabaseUtil.checkIfOldVersionIsSame(objectname,
								owner, digest)) {
							try {
								if (cloudType.equalsIgnoreCase("amazon")) {
									Delete d = new Delete();
									d.amazondo(name.get(i));
								} else if (cloudType.equalsIgnoreCase("azure")) {
									AzureStorageIntercloud a = new AzureStorageIntercloud();
									a.deleteFile(bucket, name.get(i));
								} else if (cloudType
										.equalsIgnoreCase("googlecloud")) {
									GoogleStorageIntercloud g = new GoogleStorageIntercloud();
									g.deleteFile(bucket, name.get(i));
								} else if (cloudType
										.equalsIgnoreCase("centurylink")) {
									CtlStorageForIntercloud c = new CtlStorageForIntercloud();
									c.removeObject(bucket, name.get(i));
								} else if (cloudType.equalsIgnoreCase("minio")) {
									MinioForIntercloud minio = new MinioForIntercloud();
									minio.removeObject(bucket, name.get(i));
								} else if (cloudType
										.equalsIgnoreCase("openstack")) {
									SwiftForIntercloud a = new SwiftForIntercloud();
									a.delete(bucket, name.get(i));
								} else {
									throw new StorageException(
											"Gateway does not support this vendor.");
								}
							} catch (Exception e) {
								throw new StorageException(e.getMessage(), e);
							}
						}
					}

				}
				DatabaseUtil.deleteOthersObjectTableByCloud(objectname, owner);
			}
		}
	}
}
