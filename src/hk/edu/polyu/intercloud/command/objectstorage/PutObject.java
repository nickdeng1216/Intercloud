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
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.crypto.Cipher;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.xml.sax.SAXException;

/**
 * Command PutObject
 * 
 * @author harry
 * @since 0.1
 * @version 0.1
 */

public class PutObject implements Command {
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
		initialization();
		try {
			// Check necessity of upload
			boolean contain = DatabaseUtil
					.checkDuplicateOthersAll(this.additionalInformation
							.getValue("DataDigest"));
			String cloudType = Common.my_service_providers.get("ObjectStorage");
			propertiesUtil = new PropertiesReader(cloudType);
			// Overwrite attribute
			String overwrite = additionalInformation.getValue("Overwrite");

			if (!contain) {
				String transferMethod = this.requestInformation
						.getValue("TransferMethod");
				if (transferMethod.equalsIgnoreCase("Embedded")) {
					String digestofData = null;
					String objectNameUpload = null;
					String objectName = this.requestInformation
							.getValue("ObjectName");
					String[] objectArray = objectName.split(";");
					String data = this.additionalInformation.getValue("Data");
					String[] dataArray = data.split(";");
					String digest = this.additionalInformation
							.getValue("DataDigest");
					String[] digestArray = digest.split(";");
					String bucket = propertiesUtil.getBucket();

					for (int i = 0; i < objectArray.length; i++) {
						// Base64 decode
						byte[] decodeBytes = Base64.getDecoder().decode(
								dataArray[i].getBytes());
						dataArray[i] = new String(decodeBytes);
						// Data Security Level == "Shared"
						if (this.additionalInformation.getValue("DataSecurity")
								.equalsIgnoreCase("Shared")) {
							String dekey = deKey(this.additionalInformation
									.getValue("SharedKey"));
							dataArray[i] = rDeString(dataArray[i], dekey);
						}
						// Digest on data
						digestofData = Digest.digestString(dataArray[i]);
						// Use digest as objectName
						objectNameUpload = digestofData;
						String path = System.getProperty("user.dir")
								+ "/download/" + objectNameUpload;
						File file = new File(path);
						if (file.exists() && file.isFile()) {
							FileUtils.forceDelete(file);
						}
						file.createNewFile();
						FileWriter fw = new FileWriter(file.getAbsoluteFile());
						BufferedWriter bw = new BufferedWriter(fw);
						bw.write(dataArray[i]);
						bw.close();

						// Verify data
						if (!digestofData.equalsIgnoreCase(digestArray[i])) {
							throw new DataVerificationException(
									"Retrieved Object is wrong");
						}

						try {
							if (cloudType.equalsIgnoreCase("amazon")) {
								Upload upload = new Upload();
								upload.amazondo(path);
							} else if (cloudType.equalsIgnoreCase("azure")) {
								AzureStorageIntercloud a = new AzureStorageIntercloud();
								a.uploadFile(bucket, path, objectNameUpload,
										true);
							} else if (cloudType
									.equalsIgnoreCase("googlecloud")) {
								GoogleStorageIntercloud g = new GoogleStorageIntercloud();
								g.uploadFile(bucket, path);
							} else if (cloudType
									.equalsIgnoreCase("centurylink")) {
								CtlStorageForIntercloud c = new CtlStorageForIntercloud();
								c.putObject(bucket, objectNameUpload, new File(
										path));
							} else if (cloudType.equalsIgnoreCase("minio")) {
								MinioForIntercloud minio = new MinioForIntercloud();
								minio.putObject(path, bucket, objectNameUpload);
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

						// Delete file
						file.delete();

						if (overwrite.equalsIgnoreCase("True")) {
							// Delete previous versions
							String[] deleteName = DatabaseUtil
									.getDigestofObject(
											this.generalInformation.getFrom(),
											objectArray[i]);
							for (int m = 0; m < deleteName.length; m++) {
								try {
									if (cloudType.equalsIgnoreCase("amazon")) {
										Delete d = new Delete();
										d.amazondo(deleteName[m]);
									} else if (cloudType
											.equalsIgnoreCase("azure")) {
										AzureStorageIntercloud a = new AzureStorageIntercloud();
										a.deleteFile(bucket, deleteName[m]);
									} else if (cloudType
											.equalsIgnoreCase("googlecloud")) {
										GoogleStorageIntercloud g = new GoogleStorageIntercloud();
										g.deleteFile(bucket, deleteName[m]);
									} else if (cloudType
											.equalsIgnoreCase("centurylink")) {
										CtlStorageForIntercloud c = new CtlStorageForIntercloud();
										c.removeObject(bucket, deleteName[m]);
									} else if (cloudType
											.equalsIgnoreCase("minio")) {
										MinioForIntercloud minio = new MinioForIntercloud();
										minio.removeObject(bucket,
												deleteName[m]);
									} else if (cloudType
											.equalsIgnoreCase("openstack")) {
										SwiftForIntercloud a = new SwiftForIntercloud();
										a.delete(bucket, deleteName[m]);
									} else {
										throw new StorageException(
												"Gateway does not support this vendor.");
									}
								} catch (Exception e) {
									e.printStackTrace();
									throw new StorageException(e.getMessage(),
											e);
								}

							}

							if (!DatabaseUtil.checkDuplicateOthers(digest,
									this.generalInformation.getFrom())) {
								// DB Insert
								DatabaseUtil.insertOthersObjectTable(
										this.protocol.getId(), digestArray[i],
										this.generalInformation.getFrom(),
										objectArray[i], " ", digestArray[i],
										this.additionalInformation
												.getValue("DataSecurity"));
							} else {
								// DB Update
								DatabaseUtil.updateOthersObjectTable2(
										this.protocol.getId(),
										this.generalInformation.getFrom(),
										digestArray[i], objectArray[i],
										digestArray[i]);
							}

						} else {
							// DB Insert
							DatabaseUtil.insertOthersObjectTable(this.protocol
									.getId(), digestArray[i],
									this.generalInformation.getFrom(),
									objectArray[i], " ", digestArray[i],
									this.additionalInformation
											.getValue("DataSecurity"));
						}
					}

					// Return response protocol
					return this.generateProtocol(
							this.additionalInformation.getValue("DataDigest"),
							overwrite);

				} else if (!transferMethod.equalsIgnoreCase("Embedded")) {
					/*-
					 *  if transferMethod doesn't equal "Embedded", 
					 *  program will start corresponding file transfer method according to "transferMethod".
					 */

					// Digest of file uploaded to cloud
					String digest_uploadFile = null;
					String objectName_upload = null;
					String objectName = this.requestInformation
							.getValue("ObjectName");
					String[] objectArray = objectName.split(";");
					String path = this.additionalInformation.getValue("Path");
					// After retrieving, the local path of file
					String[] path_uploadFile = null;
					String bucket = propertiesUtil.getBucket();
					path_uploadFile = fileRetrieve(transferMethod);

					for (int i = 0; i < objectArray.length; i++) {
						// Digest on upload file
						System.out.println("SSS Digest calculation START ["
								+ System.currentTimeMillis() + "]");
						digest_uploadFile = Digest
								.digestFile(path_uploadFile[i]);
						System.out.println("SSS Digest calculation END ["
								+ System.currentTimeMillis() + "]");

						// Use digest as objectNameUpload
						// nick 31/01/2019 17:13
						// objectName_upload = digest_uploadFile;
						objectName_upload = objectName;
						// Upload file to cloud
						System.out.println("SSS Uploading to storage START ["
								+ System.currentTimeMillis() + "]");
						try {
							if (cloudType.equalsIgnoreCase("amazon")) {
								Upload upload = new Upload();
								upload.amazondo(path_uploadFile[i]);
							} else if (cloudType.equalsIgnoreCase("azure")) {
								AzureStorageIntercloud azure = new AzureStorageIntercloud();
								azure.uploadFile(bucket, path_uploadFile[i],
										objectName_upload, false);
							} else if (cloudType
									.equalsIgnoreCase("centurylink")) {
								CtlStorageForIntercloud c = new CtlStorageForIntercloud();
								c.putObject(bucket, objectName_upload,
										new File(path_uploadFile[i]));
							} else if (cloudType
									.equalsIgnoreCase("googlecloud")) {
								GoogleStorageIntercloud g = new GoogleStorageIntercloud();
								g.uploadFile(bucket, path_uploadFile[i]);
							} else if (cloudType.equalsIgnoreCase("minio")) {
								MinioForIntercloud minio = new MinioForIntercloud();
								minio.putObject(path_uploadFile[i], bucket,
										objectName_upload);
							} else if (cloudType.equalsIgnoreCase("openstack")) {
								SwiftForIntercloud a = new SwiftForIntercloud();
								a.upload(bucket, path_uploadFile[i]);
							} else {
								throw new StorageException(
										"Gateway does not support this vendor.");
							}
						} catch (Exception e) {
							throw new StorageException(e.getMessage(), e);
						}
						System.out.println("SSS Uploading to storage END ["
								+ System.currentTimeMillis() + "]");
						// Delete file
						new File(path_uploadFile[i]).delete();
						if (overwrite.equalsIgnoreCase("True")) {
							// Delete previous versions
							String[] deleteName = DatabaseUtil
									.getDigestofObject(
											this.generalInformation.getFrom(),
											objectArray[i]);
							for (int m = 0; m < deleteName.length; m++) {
								try {
									if (cloudType.equalsIgnoreCase("amazon")) {
										Delete d = new Delete();
										d.amazondo(deleteName[m]);
									} else if (cloudType
											.equalsIgnoreCase("azure")) {
										AzureStorageIntercloud a = new AzureStorageIntercloud();
										a.deleteFile(bucket, deleteName[m]);
									} else if (cloudType
											.equalsIgnoreCase("googlecloud")) {
										GoogleStorageIntercloud g = new GoogleStorageIntercloud();
										g.deleteFile(bucket, deleteName[m]);
									} else if (cloudType
											.equalsIgnoreCase("centurylink")) {
										CtlStorageForIntercloud c = new CtlStorageForIntercloud();
										c.removeObject(bucket, deleteName[m]);
									} else if (cloudType
											.equalsIgnoreCase("minio")) {
										MinioForIntercloud minio = new MinioForIntercloud();
										minio.removeObject(bucket,
												deleteName[m]);
									} else if (cloudType
											.equalsIgnoreCase("openstack")) {
										SwiftForIntercloud a = new SwiftForIntercloud();
										a.delete(bucket, deleteName[m]);
									} else {
										throw new StorageException(
												"Gateway does not support this vendor.");
									}
								} catch (Exception e) {
									throw new StorageException(e.getMessage(),
											e);
								}
							}
							if (!DatabaseUtil.checkDuplicateOthers(
									digest_uploadFile,
									this.generalInformation.getFrom())) {
								// DB Insert
								DatabaseUtil.insertOthersObjectTable(
										this.protocol.getId(),
										objectName_upload,
										this.generalInformation.getFrom(),
										objectArray[i], " ", digest_uploadFile,
										this.additionalInformation
												.getValue("DataSecurity"));
							} else {
								// DB Update
								DatabaseUtil.updateOthersObjectTable2(
										this.protocol.getId(),
										this.generalInformation.getFrom(),
										objectName_upload, objectArray[i],
										digest_uploadFile);
							}

						} else {
							// DB Insert
							DatabaseUtil.insertOthersObjectTable(this.protocol
									.getId(), objectName_upload,
									this.generalInformation.getFrom(),
									objectArray[i], " ", digest_uploadFile,
									this.additionalInformation
											.getValue("DataSecurity"));
						}
					}
					// Return response protocol
					return this.generateProtocol(digest_uploadFile, overwrite);
				}

			} else {
				// No need to upload file
				if (DatabaseUtil.checkDuplicateCloud(
						this.requestInformation.getValue("ObjectName"),
						this.additionalInformation.getValue("DataDigest"),
						this.generalInformation.getFrom())) {
					System.out.println("Duplicated files");
				} else {
					DatabaseUtil
							.insertOthersObjectTable(this.protocol.getId(),
									this.additionalInformation
											.getValue("DataDigest"),
									this.generalInformation.getFrom(),
									this.requestInformation
											.getValue("ObjectName"), " ",
									this.additionalInformation
											.getValue("DataDigest"),
									this.additionalInformation
											.getValue("DataSecurity"));
				}
				return this.generateProtocol(
						this.additionalInformation.getValue("DataDigest"),
						overwrite);
			}
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
		} catch (DataVerificationException e) {
			LogUtil.logException(e);
			return this.generateException("6",
					DataVerificationException.class.getSimpleName(),
					e.getMessage());
		} catch (UdtException | FtpException | HttpException e) {
			LogUtil.logException(e);
			return this.generateException("0",
					IntercloudException.class.getSimpleName(), e.getMessage());
		}
		return null;
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
		String storage_object_name = " ";
		String id = info.get("ID");
		String storageCloud = info.get("StorageCloud");
		String sLevel = info.get("DataSecurity");
		String objectName = info.get("ObjectName");
		String tsfMethod = info.get("TransferMethod");
		String overwrite = info.get("Overwrite");
		String[] objects = objectName.split(";");
		String[] path = new String[objects.length];
		String[] data = new String[objects.length];
		String[] digest = new String[objects.length];
		String sharedKey = null;
		String esKey = null;
		AdditionalInformation additional = new AdditionalInformation();

		long startTime = System.currentTimeMillis();
		LogUtil.logPerformance("Download from storage START", objectName,
				startTime, 0L);

		// Generate random key
		if (sLevel.equalsIgnoreCase("Shared")) {
			sharedKey = Encryption.makeKeyShared();
			// Encrypt shared key
			esKey = enKey(sharedKey, storageCloud);
		}

		// Download, Digest...
		for (int i = 0; i < objects.length; i++) {
			if (cloudType.equalsIgnoreCase("amazon")) {
				Download download = new Download();
				path[i] = download.amazondo(objects[i], Common.DOWNLOAD_PATH
						+ objects[i]);
				// fileTest(path_result[i]);
			} else if (cloudType.equalsIgnoreCase("azure")) {
				AzureStorageIntercloud a = new AzureStorageIntercloud();
				path[i] = a.downloadFile(bucket, objects[i],
						Common.DOWNLOAD_PATH + objects[i]);
				// fileTest(path_result[i]);
			} else if (cloudType.equalsIgnoreCase("googlecloud")) {
				GoogleStorageIntercloud g = new GoogleStorageIntercloud();
				path[i] = g.downloadFile(bucket, objects[i],
						Common.DOWNLOAD_PATH);
				// fileTest(path_result[i]);
			} else if (cloudType.equalsIgnoreCase("centurylink")) {
				CtlStorageForIntercloud c = new CtlStorageForIntercloud();
				path[i] = Common.DOWNLOAD_PATH + objects[i];
				c.getObject(bucket, objects[i], new File(path[i]));
			} else if (cloudType.equalsIgnoreCase("minio")) {
				MinioForIntercloud minio = new MinioForIntercloud();
				minio.getObject(bucket, objects[i], Common.DOWNLOAD_PATH
						+ objects[i]);
				path[i] = Common.DOWNLOAD_PATH + objects[i];
			} else if (cloudType.equalsIgnoreCase("openstack")) {
				SwiftForIntercloud a = new SwiftForIntercloud();
				a.download(bucket, objects[i], System.getProperty("user.dir")
						+ "/download/" + objects[i]);

				path[i] = Common.DOWNLOAD_PATH + objects[i];
			}

			System.out.println("SSS Downloading from storage END ["
					+ System.currentTimeMillis() + "]");

			// Judge data security level
			if (sLevel.equalsIgnoreCase("Public")) {
				// Judge different transfer methods
				if (tsfMethod.equalsIgnoreCase("Embedded")) {
					data[i] = FileUtils.readFileToString(new File(path[i]),
							StandardCharsets.UTF_8);
					digest[i] = Digest.digestString(data[i]);
					// Use Base64 encoding for embedded data
					byte[] encodeBytes = Base64.getEncoder().encode(
							data[i].getBytes());
					data[i] = new String(encodeBytes);
				} else if (tsfMethod.equalsIgnoreCase("FTPS")
						|| tsfMethod.equalsIgnoreCase("FTP")
						|| tsfMethod.equalsIgnoreCase("UDT")
						|| tsfMethod.equalsIgnoreCase("HTTP")
						|| tsfMethod.equalsIgnoreCase("HTTPS")) {
					// Digest of file
					digest[i] = Digest.digestFile(path[i]);
					if (tsfMethod.equalsIgnoreCase("HTTP")) {
						path[i] = "http://" + Common.my_ip + ":"
								+ Common.HTTP_PORT + "/" + objects[i];
					} else if (tsfMethod.equalsIgnoreCase("HTTPS")) {
						path[i] = "https://" + Common.my_ip + ":"
								+ Common.HTTPS_PORT + "/" + objects[i];
					}
				} else {
					throw new UnsupportedTransferMethodException(
							"Gateway dose not support this trasfer method");
				}
			} else if (sLevel.equalsIgnoreCase("Private")) {
				if (tsfMethod.equalsIgnoreCase("Embedded")) {
					data[i] = FileUtils.readFileToString(new File(path[i]),
							StandardCharsets.UTF_8);
					data[i] = encryptString(data[i], objects[i]);
					digest[i] = Digest.digestString(data[i]);
					byte[] encodeBytes = Base64.getEncoder().encode(
							data[i].getBytes());
					data[i] = new String(encodeBytes);
				} else if (tsfMethod.equalsIgnoreCase("FTPS")
						|| tsfMethod.equalsIgnoreCase("FTP")
						|| tsfMethod.equalsIgnoreCase("UDT")
						|| tsfMethod.equalsIgnoreCase("HTTP")
						|| tsfMethod.equalsIgnoreCase("HTTPS")) {
					// Encrypt downloaded file
					System.out.println("EEE Encryption START ["
							+ System.currentTimeMillis() + "]");
					path[i] = encryptFile(path[i]);
					System.out.println("EEE Encryption END ["
							+ System.currentTimeMillis() + "]");
					// Digest of downloaded file
					System.out.println("DDD Digest calculation START ["
							+ System.currentTimeMillis() + "]");
					digest[i] = Digest.digestFile(path[i]);
					System.out.println("DDD Digest calculation END ["
							+ System.currentTimeMillis() + "]");

					if (tsfMethod.equalsIgnoreCase("HTTP")) {
						path[i] = "http://" + Common.my_ip + ":"
								+ Common.HTTP_PORT + "/" + objects[i];
					} else if (tsfMethod.equalsIgnoreCase("HTTPS")) {
						path[i] = "https://" + Common.my_ip + ":"
								+ Common.HTTPS_PORT + "/" + objects[i];
					}
				} else {
					throw new UnsupportedTransferMethodException(
							"Gateway dose not support this trasfer method");
				}
			} else if (sLevel.equalsIgnoreCase("Shared")) {
				if (tsfMethod.equalsIgnoreCase("Embedded")) {
					data[i] = FileUtils.readFileToString(new File(path[i]),
							StandardCharsets.UTF_8);
					digest[i] = Digest.digestString(data[i]);
					data[i] = rEnString(data[i], sharedKey);
					byte[] encodeBytes = Base64.getEncoder().encode(
							data[i].getBytes());
					data[i] = new String(encodeBytes);
				} else if (tsfMethod.equalsIgnoreCase("FTPS")
						|| tsfMethod.equalsIgnoreCase("FTP")
						|| tsfMethod.equalsIgnoreCase("UDT")
						|| tsfMethod.equalsIgnoreCase("HTTP")
						|| tsfMethod.equalsIgnoreCase("HTTPS")) {
					// Digest of downloaded file
					digest[i] = Digest.digestFile(path[i]);
					// Then encrypt this file
					path[i] = rEnFile(path[i], sharedKey);

					if (tsfMethod.equalsIgnoreCase("HTTP")) {
						path[i] = "http://" + Common.my_ip + ":"
								+ Common.HTTP_PORT + "/" + objects[i];
					} else if (tsfMethod.equalsIgnoreCase("HTTPS")) {
						path[i] = "https://" + Common.my_ip + ":"
								+ Common.HTTPS_PORT + "/" + objects[i];
					}
				} else {
					throw new UnsupportedTransferMethodException(
							"Gateway dose not support this trasfer method");
				}
			}
		}

		// Generate AdditionalInformation
		StringBuilder digestString = new StringBuilder();
		if (tsfMethod.equalsIgnoreCase("Embedded")) {
			StringBuilder dataString = new StringBuilder();
			for (int i = 0; i < objects.length; i++) {
				dataString.append(data[i]).append(";");
				digestString.append(digest[i]).append(";");
			}
			additional
					.addTags(
							"Data",
							dataString.toString().substring(0,
									dataString.length() - 1));
			additional.addTags(
					"DataDigest",
					digestString.toString().substring(0,
							digestString.length() - 1));
		} else {
			StringBuilder pathString = new StringBuilder();
			for (int i = 0; i < objects.length; i++) {
				digestString.append(digest[i]).append(";");
				pathString.append(path[i]).append(";");
			}
			additional.addTags(
					"DataDigest",
					digestString.toString().substring(0,
							digestString.length() - 1));
			additional
					.addTags(
							"Path",
							pathString.toString().substring(0,
									pathString.length() - 1));
			additional.addTags("IP", Common.my_ip);

			if (tsfMethod.equalsIgnoreCase("FTP")) {
				additional.addTags("Port", String.valueOf(Common.FTP_PORT));
			} else if (tsfMethod.equalsIgnoreCase("FTPS")) {
				additional.addTags("Port", String.valueOf(Common.FTPS_PORT));
			} else if (tsfMethod.equalsIgnoreCase("UDT")) {
				additional.addTags("Port", String.valueOf(Common.UDT_PORT));
			} else if (tsfMethod.equalsIgnoreCase("HTTP")) {
				additional.addTags("Port", String.valueOf(Common.HTTP_PORT));
			} else if (tsfMethod.equalsIgnoreCase("HTTPS")) {
				additional.addTags("Port", String.valueOf(Common.HTTPS_PORT));
			}
		}

		if (sLevel.equalsIgnoreCase("Shared")) {
			additional.addTags("SharedKey", esKey);
		}

		additional.addTags("DataSecurity", sLevel);
		additional.addTags("DataDigestAlgorithm",
				info.get("DataDigestAlgorithm"));
		additional.addTags("Encoding", info.get("Encoding"));
		additional.addTags("Overwrite", overwrite);

		// DB Operation
		for (int i = 0; i < objects.length; i++) {
			if (overwrite.equalsIgnoreCase("True")) {
				if (!DatabaseUtil.checkDuplicateOwn(digest[i], storageCloud)) {
					DatabaseUtil.insertOwnObjectTable(id, objects[i],
							storageCloud, storage_object_name, digest[i],
							sLevel);
				} else {
					DatabaseUtil.updateOwnObjects(id, storageCloud, objects[i],
							digest[i]);
				}
			} else {
				DatabaseUtil.insertOwnObjectTable(id, objects[i], storageCloud,
						storage_object_name, digest[i], sLevel);
			}
		}

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
	 * @throws SecurityException
	 * @throws IOException
	 */
	private String encryptFile(String filePath) throws IOException,
			SecurityException {
		String encryptPath = filePath + "_enc";
		String keyPath = System.getProperty("user.dir") + "/Key/private.pem";
		Encryption.copy(Cipher.ENCRYPT_MODE, filePath, encryptPath, keyPath);
		new File(filePath).delete();
		FileUtils.moveFile(new File(encryptPath), new File(filePath));
		return filePath;
	}

	/**
	 * Encrypt data in embedded transmit
	 * 
	 * @param msg
	 * @param objName
	 * @return
	 * @throws SecurityException
	 * @throws IOException
	 */
	private String encryptString(String msg, String objName)
			throws IOException, SecurityException {
		String keyPath = System.getProperty("user.dir") + "/Key/private.pem";
		byte b[] = Encryption
				.encrypt(msg, Encryption.makeKey(keyPath, objName));
		StringBuffer strBuffer = new StringBuffer();
		for (int i = 0; i < b.length; i++) {
			strBuffer.append(Integer.toString((b[i] & 0xff) + 0x100, 16)
					.substring(1));
		}
		return strBuffer.toString();
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
		String decryptPath = outPath + "_dec";
		BufferedInputStream is = new BufferedInputStream(new FileInputStream(
				filePath));
		BufferedOutputStream os = new BufferedOutputStream(
				new FileOutputStream(decryptPath));
		Decryption.decrypt(is, os, password);
		new File(filePath).delete();
		FileUtils.moveFile(new File(decryptPath), new File(filePath));
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
		String plainText = null;
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
		plainText = new String(b, "UTF-8");
		return plainText;
	}

	/**
	 * Encrypt shared key
	 * 
	 * @param rKey
	 * @param objName
	 * @return
	 * @throws SecurityException
	 * @throws IOException
	 */
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
	 */
	private String deKey(String key) throws IOException, SecurityException {
		String plainText = null;
		byte[] data = CertificateUtil.decryptBASE64(key);
		String keyPath = System.getProperty("user.dir") + "/Key/private.pem";
		byte[] p = RSA.decrypt(CertificateUtil.getPrivateKey(keyPath), data);
		plainText = new String(p, "UTF-8");
		return plainText;
	}

	/**
	 * Used for check of download file
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
	 */
	private String[] fileRetrieve(String transferProtocol) throws UdtException,
			FtpException, HttpException, IOException, SecurityException,
			DataVerificationException {
		// Target IP and Port
		String ip = this.additionalInformation.getValue("IP");
		int port = Integer
				.parseInt(this.additionalInformation.getValue("Port"));
		String[] path = this.additionalInformation.getValue("Path").split(";");
		String sLevel = this.additionalInformation.getValue("DataSecurity");
		String[] dataDigest = this.additionalInformation.getValue("DataDigest")
				.split(";");
		String[] objectName = this.requestInformation.getValue("ObjectName")
				.split(";");
		// Retrieve to tmpPath
		String[] tmpPath = new String[path.length];
		// Download times
		int i = 10;
		for (int n = 0; n < path.length; n++) {
			// nick
			// getClass()tmpPath[n] = Common.RETRIEVE_PATH + dataDigest[n];
			tmpPath[n] = Common.RETRIEVE_PATH + objectName[n];

			// Start retrieving
			File file = new File(tmpPath[n]);
			if (file.exists() && file.isFile()) {
				FileUtils.forceDelete(new File(tmpPath[n]));
			}
			do {
				if (transferProtocol.equalsIgnoreCase("UDT")) {
					Udt.download(ip, port, path[n], tmpPath[n]);
				} else if (transferProtocol.equalsIgnoreCase("FTPS")) {
					path[n] = FilenameUtils.getName(path[n]);
					String username = "intercloud";
					String password = "p@ssw0rd";
					boolean activeMode = false;
					boolean anonymous = false;
					boolean ssl = true;
					Ftp.download(ip, port, username, password, path[n],
							tmpPath[n], activeMode, anonymous, ssl);
				} else if (transferProtocol.equalsIgnoreCase("FTP")) {
					path[n] = FilenameUtils.getName(path[n]);
					String username = "intercloud";
					String password = "p@ssw0rd";
					boolean activeMode = true;
					boolean anonymous = true;
					boolean ssl = false;
					Ftp.download(ip, port, username, password, path[n],
							tmpPath[n], activeMode, anonymous, ssl);
				} else if (transferProtocol.equalsIgnoreCase("HTTP")
						|| transferProtocol.equalsIgnoreCase("HTTPS")) {
					Http.download(path[n], tmpPath[n]);
				}
				// Decrement of download times
				i--;
				// If security level is "Shared", decrypt before doing digest
				if (sLevel.equalsIgnoreCase("Shared")) {
					String deKey = deKey(this.additionalInformation
							.getValue("SharedKey"));
					tmpPath[n] = rDeFile(tmpPath[n], deKey);
				}
			} while (!Digest.digestFile(tmpPath[n]).equalsIgnoreCase(
					dataDigest[n])
					&& i >= 0);

			if (i < 0) {
				throw new DataVerificationException("Retrieved Object is wrong");
			}
		}

		return tmpPath;
	}

	/**
	 * Generate response protocol
	 * 
	 * @param digest
	 * @param security_tags
	 * @return
	 */
	private Protocol generateProtocol(String digest, String overwrite) {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date date = new Date();
		String[] dateTime = dateFormat.format(date).split("\\s+");
		GeneralInformation generalInformation = new GeneralInformation(
				this.protocol.getGeneralInformation().getTo(), this.protocol
						.getGeneralInformation().getFrom(), dateTime[0],
				dateTime[1]);

		RequestInformation requestInformation = null;

		ResponseInformation responseInformation = new ResponseInformation();
		responseInformation.setCommand("ConfirmationForPut");
		responseInformation.setService("ObjectStorage");
		responseInformation.addTags("ObjectName",
				this.requestInformation.getValue("ObjectName"));

		AdditionalInformation additionalInformation = new AdditionalInformation();
		additionalInformation.addTags("DataDigestAlgorithm", "SHA256");
		additionalInformation.addTags("DataDigest", digest);
		additionalInformation.addTags("Overwrite", overwrite);

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
		exceptionInformation.addTags("Command", "PutObject");
		exceptionInformation.addTags("Code", code);
		exceptionInformation.addTags("Type", type);
		exceptionInformation.addTags("Message", message);

		AdditionalInformation additionalInformation = new AdditionalInformation();

		return new ExceptionProtocol(this.protocol.getProtocolVersion(),
				this.protocol.getId(), generalInformation,
				exceptionInformation, additionalInformation);
	}
}
