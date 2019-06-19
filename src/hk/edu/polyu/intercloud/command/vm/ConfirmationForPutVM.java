package hk.edu.polyu.intercloud.command.vm;

import hk.edu.polyu.intercloud.command.Command;
import hk.edu.polyu.intercloud.model.protocol.AdditionalInformation;
import hk.edu.polyu.intercloud.model.protocol.Protocol;
import hk.edu.polyu.intercloud.util.DatabaseUtil;
import hk.edu.polyu.intercloud.util.LogUtil;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

/**
 * 
 * @author Kate
 *
 */
public class ConfirmationForPutVM implements Command {
	private Protocol protocol;

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
			DatabaseUtil.updateOwnVMTable(this.protocol.getId(), this.protocol
					.getResponseInformation().getValue("VMName"));
		} catch (ClassNotFoundException | SQLException e) {
			LogUtil.logException(e);
			return new Protocol(null, null, null, null, null, null, null);
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
	}

}
