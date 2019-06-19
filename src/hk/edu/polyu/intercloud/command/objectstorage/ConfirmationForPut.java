package hk.edu.polyu.intercloud.command.objectstorage;

import hk.edu.polyu.intercloud.command.Command;
import hk.edu.polyu.intercloud.common.Common;
import hk.edu.polyu.intercloud.model.protocol.AdditionalInformation;
import hk.edu.polyu.intercloud.model.protocol.GeneralInformation;
import hk.edu.polyu.intercloud.model.protocol.Protocol;
import hk.edu.polyu.intercloud.model.protocol.ResponseInformation;
import hk.edu.polyu.intercloud.util.DatabaseUtil;
import hk.edu.polyu.intercloud.util.LogUtil;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

/**
 * Command ConfirmationForPut
 * 
 * @author harry
 *
 */
public class ConfirmationForPut implements Command {

	private Protocol protocol;
	private GeneralInformation generalInformation;
	private ResponseInformation responseInformation;
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
		// Overwrite
		// String overwrite = this.additionalInformation.getValue("Overwrite");
		// String storageCloud = this.generalInformation.getFrom();
		// String digest = this.additionalInformation.getValue("DataDigest");
		String objectName = this.responseInformation.getValue("ObjectName");
		try {
			DatabaseUtil.updateStatusOwn(DatabaseUtil.status_2,
					this.protocol.getId());
		} catch (Exception e) {
			LogUtil.logException(e);
		}
		// Delete file
		File file = new File(Common.DOWNLOAD_PATH + objectName);
		file.delete();
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

}
