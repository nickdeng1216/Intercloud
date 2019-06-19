package hk.edu.polyu.intercloud.command.vm;

import hk.edu.polyu.intercloud.command.Command;
import hk.edu.polyu.intercloud.model.protocol.AdditionalInformation;
import hk.edu.polyu.intercloud.model.protocol.Protocol;

import java.util.HashMap;
import java.util.List;

/**
 * 
 * @author Kate
 *
 */
public class StatusConfirmation implements Command {

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
			HashMap<String, String> info, String systemType) {
		return null;
	}

	@Override
	public void initialization() {

	}

}
