package hk.edu.polyu.intercloud.command.objectstorage;

import hk.edu.polyu.intercloud.aws.Delete;
import hk.edu.polyu.intercloud.azurestorage.AzureStorageIntercloud;
import hk.edu.polyu.intercloud.command.Command;
import hk.edu.polyu.intercloud.common.Common;
import hk.edu.polyu.intercloud.ctlstorage.CtlStorageForIntercloud;
import hk.edu.polyu.intercloud.exceptions.DatabaseException;
import hk.edu.polyu.intercloud.exceptions.IntercloudException;
import hk.edu.polyu.intercloud.exceptions.NoSuchDataException;
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
import hk.edu.polyu.intercloud.swift.SwiftForIntercloud;
import hk.edu.polyu.intercloud.util.DatabaseUtil;
import hk.edu.polyu.intercloud.util.LogUtil;
import hk.edu.polyu.intercloud.util.PropertiesReader;

import java.io.IOException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

public class DeleteObject implements Command {
	private Protocol protocol;
	private GeneralInformation generalInformation;
	private RequestInformation requestInformation;
	private AdditionalInformation additionalInformation;
	private PropertiesReader propertiesUtil;

	public DeleteObject() {
	}

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
			String objectDelete = null;
			initialization();
			cloudType = Common.my_service_providers.get("ObjectStorage");
			propertiesUtil = new PropertiesReader(cloudType);

			String bucketName = this.propertiesUtil.getBucket();
			String ownercloud = this.generalInformation.getFrom();
			String objectName = this.requestInformation.getValue("ObjectName");
			String objectDigest = this.additionalInformation
					.getValue("DataDigest");

			// Whether delete this object
			if (DatabaseUtil.countOthersDigest(objectDigest)) {

				objectDelete = DatabaseUtil.findOthersObject(ownercloud,
						objectDigest);
				if (objectDelete == null) {
					throw new NoSuchDataException(
							"Object with specified digest cannot be found");
				}

				try {
					if (cloudType.equalsIgnoreCase("amazon")) {
						Delete d = new Delete();
						d.amazondo(objectDelete);
					} else if (cloudType.equalsIgnoreCase("azure")) {
						AzureStorageIntercloud a = new AzureStorageIntercloud();
						a.deleteFile(bucketName, objectDelete);
					} else if (cloudType.equalsIgnoreCase("googlecloud")) {
						GoogleStorageIntercloud g = new GoogleStorageIntercloud();
						g.deleteFile(bucketName, objectDelete);
					} else if (cloudType.equalsIgnoreCase("centurylink")) {
						CtlStorageForIntercloud c = new CtlStorageForIntercloud();
						c.removeObject(bucketName, objectDelete);
					} else if (cloudType.equalsIgnoreCase("minio")) {
						MinioForIntercloud minio = new MinioForIntercloud();
						minio.removeObject(bucketName, objectDelete);
					} else if (cloudType.equalsIgnoreCase("openstack")) {
						SwiftForIntercloud a = new SwiftForIntercloud();
						a.delete(bucketName, objectDelete);
					} else {
						throw new StorageException(
								"Gateway does not support this vendor.");
					}
				} catch (Exception e) {
					throw new StorageException(e.getMessage(), e);
				}
			}
			// Delete records in OS_OTHERS_OBJECTS
			DatabaseUtil.deleteOthersObjectTable(ownercloud, objectDigest, objectName);
			return this.generateProtocol();
		} catch (StorageException e) {
			LogUtil.logException(e);
			return this.generateException("3",
					StorageException.class.getSimpleName(), e.getMessage());
		} catch (ClassNotFoundException | SQLException e) {
			LogUtil.logException(e);
			return this.generateException("2",
					DatabaseException.class.getSimpleName(), e.getMessage());
		} catch (NoSuchDataException e) {
			LogUtil.logException(e);
			return this.generateException("7",
					NoSuchDataException.class.getSimpleName(),
					"Object with specified digest cannot be found");
		} catch (IOException e) {
			LogUtil.logException(e);
			return this.generateException("0",
					IntercloudException.class.getSimpleName(), e.getMessage());
		}
	}

	@Override
	public AdditionalInformation pre_execute(String protocol,
			HashMap<String, String> info, String systemType)
			throws ParserConfigurationException, SAXException, IOException,
			Exception {
		AdditionalInformation additionalInformation = new AdditionalInformation();
		additionalInformation.addTags(
				"DataDigest",
				DatabaseUtil.getDigestOwn(info.get("ObjectName"),
						info.get("ToCloud")));
		additionalInformation.addTags("DataDigestAlgorithm",
				info.get("DataDigestAlgorithm"));
		return additionalInformation;
	}

	@Override
	public void initialization() {
		this.generalInformation = protocol.getGeneralInformation();
		this.requestInformation = protocol.getRequestInformation();
		this.additionalInformation = protocol.getAdditionalInformation();
	}

	private Protocol generateProtocol() throws SQLException {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date date = new Date();
		String[] dateTime = dateFormat.format(date).split("\\s+");

		GeneralInformation generalInformation = new GeneralInformation(
				this.protocol.getGeneralInformation().getTo(), this.protocol
						.getGeneralInformation().getFrom(), dateTime[0],
				dateTime[1]);

		ResponseInformation responseInformation = new ResponseInformation();
		responseInformation.setCommand("ConfirmationForDelete");
		responseInformation.setService("ObjectStorage");
		responseInformation.addTags("ObjectName",
				this.requestInformation.getValue("ObjectName"));

		AdditionalInformation additionalInformation = new AdditionalInformation();
		additionalInformation.addTags("DataDigest",
				this.additionalInformation.getValue("DataDigest"));
		additionalInformation.addTags("DataDigestAlgorithm",
				this.additionalInformation.getValue("DataDigestAlgorithm"));

		Protocol protocol = new Protocol(this.protocol.getProtocolVersion(),
				this.protocol.getId(), generalInformation, null,
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
		exceptionInformation.addTags("Command", "DeleteObject");
		exceptionInformation.addTags("Code", code);
		exceptionInformation.addTags("Type", type);
		exceptionInformation.addTags("Message", message);

		AdditionalInformation additionalInformation = new AdditionalInformation();

		return new ExceptionProtocol(this.protocol.getProtocolVersion(),
				this.protocol.getId(), generalInformation,
				exceptionInformation, additionalInformation);
	}
}
