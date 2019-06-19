package hk.edu.polyu.intercloud.command.objectstorage;

import hk.edu.polyu.intercloud.aws.CheckObject;
import hk.edu.polyu.intercloud.azurestorage.AzureStorageIntercloud;
import hk.edu.polyu.intercloud.command.Command;
import hk.edu.polyu.intercloud.common.Common;
import hk.edu.polyu.intercloud.ctlstorage.CtlStorageForIntercloud;
import hk.edu.polyu.intercloud.exceptions.DatabaseException;
import hk.edu.polyu.intercloud.exceptions.IntercloudException;
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

public class ListObject implements Command {

	private Protocol protocol;
	private GeneralInformation generalInformation;
	private RequestInformation requestInformation;
	private AdditionalInformation additionalInformation;

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
			String[] objectList = DatabaseUtil
					.getObjectList(this.generalInformation.getFrom());
			String cloudType = Common.my_service_providers.get("ObjectStorage");
			PropertiesReader propertiesUtil = new PropertiesReader(cloudType);
			String bucket = propertiesUtil.getBucket();
			String objectName = "";
			String dataDigest = "";
			String updateTime = "";
			for (String object : objectList) {
				boolean exist = false;
				String details[] = object.split(";");
				try {
					if (cloudType.equalsIgnoreCase("amazon")) {
						CheckObject a = new CheckObject();
						exist = a.amazondo(details[1]);
					} else if (cloudType.equalsIgnoreCase("azure")) {
						AzureStorageIntercloud a = new AzureStorageIntercloud();
						exist = a.checkFile(bucket, details[1]);
					} else if (cloudType.equalsIgnoreCase("googlecloud")) {
						GoogleStorageIntercloud g = new GoogleStorageIntercloud();
						if (g.getMetaData(bucket, details[1]) != null) {
							exist = true;
						}
					} else if (cloudType.equalsIgnoreCase("centurylink")) {
						CtlStorageForIntercloud c = new CtlStorageForIntercloud();
						exist = c.checkObject(bucket, details[1]);
					} else if (cloudType.equalsIgnoreCase("minio")) {
						MinioForIntercloud minio = new MinioForIntercloud();
						exist = minio.checkObject(bucket, details[1]);
					} else if (cloudType.equalsIgnoreCase("openstack")) {
						SwiftForIntercloud a = new SwiftForIntercloud();
						exist = a.checkObject(bucket, object);
					} else {
						throw new StorageException(
								"Gateway does not support this vendor.");
					}
				} catch (Exception e) {
					;
				}
				if (exist) {
					objectName += (details[0] + ";");
					dataDigest += (details[1] + ";");
					updateTime += (details[2] + ";");
				}
			}
			if (!objectName.equals("")) {
				objectName = objectName.substring(0, objectName.length() - 1);
			}
			if (!dataDigest.equals("")) {
				dataDigest = dataDigest.substring(0, dataDigest.length() - 1);
			}
			if (!updateTime.equals("")) {
				updateTime = updateTime.substring(0, updateTime.length() - 1);
			}

			return this.generateProtocol(objectName, dataDigest, updateTime);
		} catch (ClassNotFoundException | SQLException e) {
			LogUtil.logException(e);
			return this.generateException("2",
					DatabaseException.class.getSimpleName(), e.getMessage());
		} catch (IOException e) {
			LogUtil.logException(e);
			return this.generateException("0",
					IntercloudException.class.getSimpleName(), e.getMessage());

		} catch (StorageException e) {
			LogUtil.logException(e);
			return this.generateException("3",
					StorageException.class.getSimpleName(), e.getMessage());
		}
	}

	@Override
	public AdditionalInformation pre_execute(String protocol,
			HashMap<String, String> info, String systemType) {
		return new AdditionalInformation();
	}

	@Override
	public void initialization() {
		this.generalInformation = protocol.getGeneralInformation();
		this.requestInformation = protocol.getRequestInformation();
		this.additionalInformation = protocol.getAdditionalInformation();
	}

	private Protocol generateProtocol(String objectName, String dataDigest,
			String updateTime) {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date date = new Date();
		String[] dateTime = dateFormat.format(date).split("\\s+");
		GeneralInformation generalInformation = new GeneralInformation(
				this.protocol.getGeneralInformation().getTo(), this.protocol
						.getGeneralInformation().getFrom(), dateTime[0],
				dateTime[1]);

		RequestInformation requestInformation = null;

		ResponseInformation responseInformation = new ResponseInformation();
		responseInformation.setCommand("ObjectList");
		responseInformation.setService("ObjectStorage");
		responseInformation.addTags("ObjectName", objectName);
		responseInformation.addTags("DataDigest", dataDigest);
		responseInformation.addTags("UpdateTime", updateTime);

		AdditionalInformation additionalInformation = new AdditionalInformation();

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
		exceptionInformation.addTags("Command", "ListObject");
		exceptionInformation.addTags("Code", code);
		exceptionInformation.addTags("Type", type);
		exceptionInformation.addTags("Message", message);

		AdditionalInformation additionalInformation = new AdditionalInformation();

		return new ExceptionProtocol(this.protocol.getProtocolVersion(),
				this.protocol.getId(), generalInformation,
				exceptionInformation, additionalInformation);
	}

}
