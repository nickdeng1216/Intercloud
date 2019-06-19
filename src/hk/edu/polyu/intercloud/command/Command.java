package hk.edu.polyu.intercloud.command;

import hk.edu.polyu.intercloud.model.protocol.AdditionalInformation;
import hk.edu.polyu.intercloud.model.protocol.Protocol;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

public interface Command {
	public abstract void setProtocol(Protocol protocol);

	public abstract Protocol getProtocol();

	public abstract Protocol execute(List<Object> o);

	public abstract AdditionalInformation pre_execute(String protocol,
			HashMap<String, String> info, String systemType)
			throws ParserConfigurationException, SAXException, IOException,
			Exception;

	public abstract void initialization();
}
