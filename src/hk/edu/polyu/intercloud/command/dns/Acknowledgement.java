package hk.edu.polyu.intercloud.command.dns;

import hk.edu.polyu.intercloud.command.Command;
import hk.edu.polyu.intercloud.model.protocol.AdditionalInformation;
import hk.edu.polyu.intercloud.model.protocol.Protocol;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

public class Acknowledgement implements Command {

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
		return new Protocol(null, null, null, null, null, null, null);
	}

	@Override
	public AdditionalInformation pre_execute(String protocol,
			HashMap<String, String> info, String systemType)
			throws ParserConfigurationException, SAXException, IOException,
			Exception {
		AdditionalInformation additionalInformation = new AdditionalInformation();
		return additionalInformation;
	}

	@Override
	public void initialization() {
	}

}
