package hk.edu.polyu.intercloud.command.objectstorage;

import hk.edu.polyu.intercloud.command.Command;
import hk.edu.polyu.intercloud.model.protocol.AdditionalInformation;
import hk.edu.polyu.intercloud.model.protocol.GeneralInformation;
import hk.edu.polyu.intercloud.model.protocol.Protocol;
import hk.edu.polyu.intercloud.model.protocol.ResponseInformation;
import hk.edu.polyu.intercloud.util.DatabaseUtil;
import hk.edu.polyu.intercloud.util.LogUtil;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

public class ConfirmationForForward implements Command {
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
		try {
			DatabaseUtil.updateStatusOwn(DatabaseUtil.status_2,
					this.protocol.getId());
		} catch (SQLException | ClassNotFoundException | ParseException e) {
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

}
