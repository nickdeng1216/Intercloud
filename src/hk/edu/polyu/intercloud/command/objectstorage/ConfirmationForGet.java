package hk.edu.polyu.intercloud.command.objectstorage;

import hk.edu.polyu.intercloud.aws.Upload;
import hk.edu.polyu.intercloud.azurestorage.AzureStorageIntercloud;
import hk.edu.polyu.intercloud.command.Command;
import hk.edu.polyu.intercloud.common.Common;
import hk.edu.polyu.intercloud.ctlstorage.CtlStorageForIntercloud;
import hk.edu.polyu.intercloud.exceptions.DataVerificationException;
import hk.edu.polyu.intercloud.exceptions.SecurityException;
import hk.edu.polyu.intercloud.exceptions.StorageException;
import hk.edu.polyu.intercloud.fileserver.client.Ftp;
import hk.edu.polyu.intercloud.fileserver.client.Http;
import hk.edu.polyu.intercloud.fileserver.client.Udt;
import hk.edu.polyu.intercloud.fileserver.exceptions.FtpException;
import hk.edu.polyu.intercloud.fileserver.exceptions.HttpException;
import hk.edu.polyu.intercloud.fileserver.exceptions.UdtException;
import hk.edu.polyu.intercloud.gstorage.GoogleStorageIntercloud;
import hk.edu.polyu.intercloud.minio.MinioForIntercloud;
import hk.edu.polyu.intercloud.model.protocol.AdditionalInformation;
import hk.edu.polyu.intercloud.model.protocol.GeneralInformation;
import hk.edu.polyu.intercloud.model.protocol.Protocol;
import hk.edu.polyu.intercloud.model.protocol.ResponseInformation;
import hk.edu.polyu.intercloud.security.Decryption;
import hk.edu.polyu.intercloud.security.Digest;
import hk.edu.polyu.intercloud.security.Encryption;
import hk.edu.polyu.intercloud.security.RSA;
import hk.edu.polyu.intercloud.swift.SwiftForIntercloud;
import hk.edu.polyu.intercloud.util.CertificateUtil;
import hk.edu.polyu.intercloud.util.DatabaseUtil;
import hk.edu.polyu.intercloud.util.LogUtil;
import hk.edu.polyu.intercloud.util.PropertiesReader;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.crypto.Cipher;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.xml.sax.SAXException;

/**
 * 
 * @author harry
 *
 */

public class ConfirmationForGet implements Command {

	private Protocol protocol;
	private GeneralInformation generalInformation;
	private ResponseInformation responseInformation;
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

			// Check data transfer method
			String transferMethod = this.responseInformation
					.getValue("TransferMethod");
			// Get version of object
			String version = this.additionalInformation.getValue("Version");

			if (transferMethod.equalsIgnoreCase("Embedded")) {
				// Check digest of data
				boolean contain = DatabaseUtil.getDigestOwn(
						this.responseInformation.getValue("ObjectName"),
						this.protocol.getGeneralInformation().getFrom())
						.equalsIgnoreCase(
								this.additionalInformation
										.getValue("DataDigest"));

				if (contain) {
					String objectName = this.responseInformation
							.getValue("ObjectName");
					String[] objectArray = objectName.split(";");
					String data = this.additionalInformation.getValue("Data");
					String[] dataArray = data.split(";");
					String digest = this.additionalInformation
							.getValue("DataDigest");
					String[] digestArray = digest.split(";");
					// Get security level from database.
					String sLevel = this.additionalInformation
							.getValue("DataSecurity");

					for (int i = 0; i < objectArray.length; i++) {
						// Base64 decode
						byte[] decodeBytes = Base64.getDecoder().decode(
								dataArray[i].getBytes());
						dataArray[i] = new String(decodeBytes);
						// Digest on data
						String digestCheck = null;
						if (sLevel.equalsIgnoreCase("Private")) {
							digestCheck = Digest.digestString(dataArray[i]);
							dataArray[i] = decryptMsg(dataArray[i],
									objectArray[i]);
						} else if (sLevel.equalsIgnoreCase("Shared")) {
							String deKey = deKey(this.additionalInformation
									.getValue("SharedKey"));
							dataArray[i] = rDeString(dataArray[i], deKey);
							digestCheck = Digest.digestString(dataArray[i]);
						} else if (sLevel.equalsIgnoreCase("Public")) {
							digestCheck = Digest.digestString(dataArray[i]);
						}

						if (!digestCheck.equalsIgnoreCase(digestArray[i])) {
							throw new DataVerificationException(
									"Retrieved object is wrong!");
						}

						String path = System.getProperty("user.dir")
								+ "/download/" + objectArray[i];

						File file = new File(path);
						file.createNewFile();
						FileWriter fw = new FileWriter(file.getAbsoluteFile());
						BufferedWriter bw = new BufferedWriter(fw);
						bw.write(dataArray[i]);
						bw.close();

						// Upload file to cloud
						String bucket = propertiesUtil.getBucket();
						try {
							if (cloudType.equalsIgnoreCase("amazon")) {
								Upload upload = new Upload();
								upload.amazondo(path);
							} else if (cloudType.equalsIgnoreCase("azure")) {
								AzureStorageIntercloud a = new AzureStorageIntercloud();
								a.uploadFile(bucket, path, objectArray[i], true);
							} else if (cloudType
									.equalsIgnoreCase("googlecloud")) {
								GoogleStorageIntercloud g = new GoogleStorageIntercloud();
								g.uploadFile(bucket, path);
							} else if (cloudType
									.equalsIgnoreCase("centurylink")) {
								CtlStorageForIntercloud c = new CtlStorageForIntercloud();
								c.putObject(bucket, objectArray[i], new File(
										path));
							} else if (cloudType.equalsIgnoreCase("minio")) {
								MinioForIntercloud minio = new MinioForIntercloud();
								minio.putObject(path, bucket, objectArray[i]);
							} else if (cloudType.equalsIgnoreCase("openstack")) {
								SwiftForIntercloud a = new SwiftForIntercloud();
								a.upload(bucket, path);
							} else {
								throw new StorageException(
										"Gateway does not support this vendor.");
							}
						} catch (Exception e) {
							throw new StorageException(e.getMessage(), e);
						}
						System.out.println("SSS Uploading to storage END ["
								+ System.currentTimeMillis() + "]");
					}

				} else {
					// Throw exception if the digest of received data
					// doesn't equal to the DB record
					throw new DataVerificationException("Not Original Object");
				}
			} else if (!transferMethod.equalsIgnoreCase("Embedded")) {
				// Retrieve file The path of retrieved file
				String[] pathRetrieve = null;
				String objectName = this.responseInformation
						.getValue("ObjectName");
				String[] objectArray = objectName.split(";");
				String digest = this.additionalInformation
						.getValue("DataDigest");
				String[] digestArray = digest.split(";");
				String sLevel = this.additionalInformation
						.getValue("DataSecurity");

				// Get retrieve path and digest
				pathRetrieve = fileRetrieve(transferMethod);

				for (int i = 0; i < objectArray.length; i++) {
					String pathUploadFile = pathRetrieve[i];
					if (sLevel.equalsIgnoreCase("Private")) {
						// Decrypt file
						System.out.println("DDD Decryption START ["
								+ System.currentTimeMillis() + "]");
						pathUploadFile = decryptFile(pathRetrieve[i]);
						System.out.println("DDD Decryption END ["
								+ System.currentTimeMillis() + "]");
					}

					// Verify digest of file
					boolean contain = false;
					if (version == null) {
						contain = DatabaseUtil
								.getDigestOwn(
										objectArray[i],
										this.protocol.getGeneralInformation()
												.getFrom()).equalsIgnoreCase(
										digestArray[i]);
					} else {
						contain = DatabaseUtil
								.getLatestDigest(
										objectArray[i],
										this.protocol.getGeneralInformation()
												.getFrom(), version)
								.equalsIgnoreCase(digestArray[i]);
					}

					if (contain) {
						String bucket = propertiesUtil.getBucket();

						// Upload file to cloud
						System.out.println("SSS Uploading to storage START ["
								+ System.currentTimeMillis() + "]");
						try {
							if (cloudType.equalsIgnoreCase("amazon")) {
								Upload upload = new Upload();
								upload.amazondo(pathUploadFile);
							} else if (cloudType.equalsIgnoreCase("azure")) {
								AzureStorageIntercloud a = new AzureStorageIntercloud();
								a.uploadFile(bucket, pathUploadFile,
										objectArray[i], true);
							} else if (cloudType
									.equalsIgnoreCase("googlecloud")) {
								GoogleStorageIntercloud g = new GoogleStorageIntercloud();
								g.uploadFile(bucket, pathUploadFile);
							} else if (cloudType
									.equalsIgnoreCase("centurylink")) {
								CtlStorageForIntercloud c = new CtlStorageForIntercloud();
								c.putObject(bucket, objectArray[i], new File(
										pathUploadFile));
							} else if (cloudType.equalsIgnoreCase("minio")) {
								MinioForIntercloud minio = new MinioForIntercloud();
								minio.putObject(pathUploadFile, bucket,
										objectArray[i]);
							} else if (cloudType.equalsIgnoreCase("openstack")) {
								SwiftForIntercloud a = new SwiftForIntercloud();
								a.upload(bucket, pathUploadFile);
							} else {
								throw new StorageException(
										"Gateway does not support this vendor.");
							}
						} catch (Exception e) {
							throw new StorageException(e.getMessage(), e);
						}
						System.out.println("SSS Uploading to storage END ["
								+ System.currentTimeMillis() + "]");

					} else {
						// Throw exception if the digest of received file
						// doesn't equal to the DB record
						throw new DataVerificationException(
								"Not Original Object");
					}

				}
			}
		} catch (SQLException | ClassNotFoundException | ParseException e) {
			LogUtil.logException(e);
		} catch (StorageException e) {
			LogUtil.logException(e);
		} catch (SecurityException e) {
			LogUtil.logException(e);
		} catch (IOException e) {
			LogUtil.logException(e);
		} catch (UdtException | FtpException | HttpException e) {
			LogUtil.logException(e);
		} catch (DataVerificationException e) {
			LogUtil.logException(e);
		}
		return new Protocol(null, null, null, null, null, null, null);
	}

	@Override
	public AdditionalInformation pre_execute(String protocol,
			HashMap<String, String> info, String systemType)
			throws ParserConfigurationException, SAXException, IOException,
			Exception {
		AdditionalInformation additional = new AdditionalInformation();
		Set<Entry<String, String>> set = info.entrySet();
		Iterator<Entry<String, String>> i = set.iterator();
		while (i.hasNext()) {
			Entry<String, String> addInfo = i.next();
			if (addInfo.getKey().equals("TransferMethod"))
				continue;
			additional.addTags(addInfo.getKey().toString(), addInfo.getValue()
					.toString());
		}

		return additional;
	}

	@Override
	public void initialization() {
		this.generalInformation = protocol.getGeneralInformation();
		this.responseInformation = protocol.getResponseInformation();
		this.additionalInformation = protocol.getAdditionalInformation();
	}

	private String[] fileRetrieve(String transferProtocol) throws UdtException,
			FtpException, HttpException, SecurityException, IOException,
			DataVerificationException {
		String ip = this.additionalInformation.getValue("IP");
		int port = Integer
				.parseInt(this.additionalInformation.getValue("Port"));
		String path = this.additionalInformation.getValue("Path");
		String[] pathArray = path.split(";");
		String objectName = this.responseInformation.getValue("ObjectName");
		String[] objectArray = objectName.split(";");
		String digest = this.additionalInformation.getValue("DataDigest");
		String[] digestArray = digest.split(";");
		String sLevel = this.additionalInformation.getValue("DataSecurity");
		String[] tmpPath = new String[objectArray.length];
		String digestCheck = null;
		// Download times
		int i = 10;
		for (int n = 0; n < objectArray.length; n++) {
			tmpPath[n] = Common.RETRIEVE_PATH + objectArray[n];
			// Start retrieving
			do {
				File file = new File(tmpPath[n]);
				if (file.exists() && file.isFile()) {
					FileUtils.forceDelete(file);
				}
				if (transferProtocol.equalsIgnoreCase("UDT")) {
					Udt.download(ip, port, pathArray[n], tmpPath[n]);
				} else if (transferProtocol.equalsIgnoreCase("FTPS")) {
					pathArray[n] = FilenameUtils.getName(pathArray[n]);
					String username = "intercloud";
					String password = "p@ssw0rd";
					boolean activeMode = false;
					boolean anonymous = false;
					boolean ssl = true;
					Ftp.download(ip, port, username, password, pathArray[n],
							tmpPath[n], activeMode, anonymous, ssl);
				} else if (transferProtocol.equalsIgnoreCase("FTP")) {
					pathArray[n] = FilenameUtils.getName(pathArray[n]);
					String username = "intercloud";
					String password = "p@ssw0rd";
					boolean activeMode = true;
					boolean anonymous = true;
					boolean ssl = false;
					Ftp.download(ip, port, username, password, pathArray[n],
							tmpPath[n], activeMode, anonymous, ssl);
				} else if (transferProtocol.equalsIgnoreCase("HTTP")
						|| transferProtocol.equalsIgnoreCase("HTTPS")) {
					Http.download(pathArray[n], tmpPath[n]);
				}
				// Decrement of download times
				i--;
				// If security level is "Shared", decrypt before do digest
				if (sLevel.equalsIgnoreCase("Shared")) {
					String deKey = deKey(this.additionalInformation
							.getValue("SharedKey"));
					tmpPath[n] = rDeFile(tmpPath[n], deKey);
				}
				digestCheck = getFileDigest(tmpPath[n]);
			} while (!digestCheck.equalsIgnoreCase(digestArray[n]) && i >= 0);

			if (i < 0) {
				throw new DataVerificationException("Retrieved Object is wrong");
			}
		}

		return tmpPath;
	}

	private String getFileDigest(String path) throws IOException,
			SecurityException {
		System.out.println("DDD Digest calculation START ["
				+ System.currentTimeMillis() + "]");
		String d = Digest.digestFile(path);
		System.out.println("DDD Digest calculation END ["
				+ System.currentTimeMillis() + "]");
		return d;
	}

	private String decryptFile(String encryptedFile) throws IOException,
			SecurityException {
		String keyPath = System.getProperty("user.dir") + "/Key/private.pem";
		String name = this.responseInformation.getValue("ObjectName");
		String filePath = Common.RETRIEVE_PATH + name;
		String decPath = filePath + "_dec";
		Decryption.copy(Cipher.DECRYPT_MODE, encryptedFile, decPath, keyPath);
		new File(filePath).delete();
		FileUtils.moveFile(new File(decPath), new File(filePath));
		return filePath;
	}

	private String decryptMsg(String msg, String objectName)
			throws IOException, SecurityException {
		String plainText = null;
		int length = msg.length();
		byte[] data = new byte[length / 2];
		for (int i = 0; i < length; i += 2) {
			data[i / 2] = (byte) ((Character.digit(msg.charAt(i), 16) << 4) + Character
					.digit(msg.charAt(i + 1), 16));
		}
		String keyPath = System.getProperty("user.dir") + "/Key/private.pem";
		byte[] p = Decryption.decrypt(data,
				Encryption.makeKey(keyPath, objectName));
		plainText = new String(p, "UTF-8");
		return plainText;
	}

	/**
	 * Decrypt shared key
	 * 
	 * @param key
	 * @param objectName
	 * @return
	 * @throws SecurityException
	 * @throws IOException
	 * @throws Exception
	 */
	private String deKey(String key) throws SecurityException, IOException {
		String plainText = null;
		byte[] data = CertificateUtil.decryptBASE64(key);
		String keyPath = System.getProperty("user.dir") + "/Key/private.pem";
		byte[] p = RSA.decrypt(CertificateUtil.getPrivateKey(keyPath), data);
		plainText = new String(p, "UTF-8");
		return plainText;
	}

	/**
	 * Decrypt shared file
	 * 
	 * @param filePath
	 * @param password
	 * @return
	 * @throws SecurityException
	 * @throws IOException
	 */
	private String rDeFile(String filePath, String password)
			throws IOException, SecurityException {
		String name = FilenameUtils.getName(filePath);
		String outPath = Common.RETRIEVE_PATH + name;
		String decPath = outPath + "_dec";
		BufferedInputStream is = new BufferedInputStream(new FileInputStream(
				filePath));
		BufferedOutputStream os = new BufferedOutputStream(
				new FileOutputStream(decPath));
		Decryption.decrypt(is, os, password);
		new File(filePath).delete();
		FileUtils.moveFile(new File(decPath), new File(filePath));
		return outPath;
	}

	/**
	 * Decrypt shared string
	 * 
	 * @param msg
	 * @param password
	 * @return
	 * @throws SecurityException
	 * @throws IOException
	 */
	private String rDeString(String msg, String password) throws IOException,
			SecurityException {
		int length = msg.length();
		byte[] data = new byte[length / 2];
		for (int i = 0; i < length; i += 2) {
			data[i / 2] = (byte) ((Character.digit(msg.charAt(i), 16) << 4) + Character
					.digit(msg.charAt(i + 1), 16));
		}
		ByteArrayInputStream bis = new ByteArrayInputStream(data);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		Decryption.decrypt(bis, bos, password);
		byte b[] = bos.toByteArray();
		return new String(b, "UTF-8");
	}
}
