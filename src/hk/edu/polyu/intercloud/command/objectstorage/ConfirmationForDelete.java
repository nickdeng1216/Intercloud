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
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

public class ConfirmationForDelete implements Command {
	private Protocol protocol;
	private GeneralInformation generalInformation;
	private ResponseInformation responseInformation;
	private AdditionalInformation additionalInformation;

	public ConfirmationForDelete() {
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
		initialization();
		try {
			DatabaseUtil.deleteOwnObjectTable(
					this.additionalInformation.getValue("DataDigest"),
					this.responseInformation.getValue("ObjectName"),
					this.generalInformation.getFrom());
		} catch (SQLException | ClassNotFoundException e) {
			LogUtil.logException(e);
		}
		return new Protocol(null, null, null, null, null, null, null);
	}

	@Override
	public AdditionalInformation pre_execute(String protocol,
			HashMap<String, String> info, String systemType)
			throws ParserConfigurationException, SAXException, IOException,
			Exception {
		return null;
	}

	@Override
	public void initialization() {
		this.generalInformation = protocol.getGeneralInformation();
		this.responseInformation = protocol.getResponseInformation();
		this.additionalInformation = protocol.getAdditionalInformation();
	}

}
