package hk.edu.polyu.intercloud.command;

import hk.edu.polyu.intercloud.model.protocol.AdditionalInformation;
import hk.edu.polyu.intercloud.model.protocol.ExceptionProtocol;
import hk.edu.polyu.intercloud.model.protocol.Protocol;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

/**
 * 
 * @author harry
 *
 */

public class ExceptionCommand implements Command {

	private ExceptionProtocol protocol;

	// private GeneralInformation generalInformation;
	// private ExceptionInformation exceptionInformation;
	// private AdditionalInformation additioanlInformation;

	@Override
	public void setProtocol(Protocol protocol) {
		this.protocol = (ExceptionProtocol) protocol;
	}

	@Override
	public Protocol getProtocol() {
		return protocol;
	}

	@Override
	public Protocol execute(List<Object> o) {
		return generateProtocol();
	}

	@Override
	public void initialization() {
	}

	public Protocol generateProtocol() {
		return new Protocol(null, null, null, null, null, null, null);
	}

	@Override
	public AdditionalInformation pre_execute(String protocol,
			HashMap<String, String> info, String systemType)
			throws ParserConfigurationException, SAXException, IOException,
			Exception {
		return null;
	}

}
