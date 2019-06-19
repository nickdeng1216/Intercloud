package hk.edu.polyu.intercloud.command.dns;

import hk.edu.polyu.intercloud.command.Command;
import hk.edu.polyu.intercloud.common.Common;
import hk.edu.polyu.intercloud.model.protocol.AdditionalInformation;
import hk.edu.polyu.intercloud.model.protocol.ExceptionInformation;
import hk.edu.polyu.intercloud.model.protocol.ExceptionProtocol;
import hk.edu.polyu.intercloud.model.protocol.GeneralInformation;
import hk.edu.polyu.intercloud.model.protocol.Protocol;
import hk.edu.polyu.intercloud.model.protocol.RequestInformation;
import hk.edu.polyu.intercloud.model.protocol.ResponseInformation;
import hk.edu.polyu.intercloud.util.DNSUtil;
import hk.edu.polyu.intercloud.util.LogUtil;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

public class DeleteAllRecord implements Command {
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
		String domainName = this.protocol.getGeneralInformation().getFrom();
		String domain = "";
		String hostName = "";

		try {
			if (!Common.my_role.equalsIgnoreCase("Exchange")) {
				return generateException(this.protocol.getProtocolVersion(),
						"DomainNameServerException",
						"DNS service is not supported");
			}

			String[] parts = domainName.split("\\.");
			if (parts.length > 1) {
				hostName = parts[0];
				domain = domainName.substring(parts[0].length() + 1,
						domainName.length());
			}

			if (!domain.equalsIgnoreCase(Common.my_name)
					|| !Common.my_friends.containsKey(domainName)
					|| !Common.my_friends.get(domainName).getRole()
							.equalsIgnoreCase("Cloud")) {
				return generateException(this.protocol.getProtocolVersion(),
						"DomainNameServerException", "Unknown Cloud");
			}

			DNSUtil.deleteAllRecord(domain, hostName);

			ArrayList<String[]> newRecords = DNSUtil.listIntercloudTXTRecords(
					domain, hostName);
			return this.generateProtocol(newRecords);

		} catch (Exception e) {
			LogUtil.logException(e);
			return generateException(this.protocol.getProtocolVersion(),
					"DomainNameServerException", e.getMessage());
		}
	}

	@Override
	public AdditionalInformation pre_execute(String protocol,
			HashMap<String, String> info, String systemType)
			throws ParserConfigurationException, SAXException, IOException,
			Exception {
		AdditionalInformation additional = new AdditionalInformation();
		return additional;
	}

	@Override
	public void initialization() {

	}

	private Protocol generateProtocol(ArrayList<String[]> dnsRecord)
			throws Exception {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date date = new Date();
		String[] dateTime = dateFormat.format(date).split("\\s+");

		GeneralInformation generalInformation = new GeneralInformation(
				this.protocol.getGeneralInformation().getTo(), this.protocol
						.getGeneralInformation().getFrom(), dateTime[0],
				dateTime[1]);

		RequestInformation requestInformation = null;

		ResponseInformation responseInformation = new ResponseInformation();
		responseInformation.setCommand("Acknowledgement");
		responseInformation.setService("DNS");
		for (int i = 0; i < dnsRecord.size(); i++) {
			responseInformation.addTags(dnsRecord.get(i)[0],
					dnsRecord.get(i)[1]);
		}

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
		exceptionInformation.addTags("Command", "DeleteAllRecord");
		exceptionInformation.addTags("Code", code);
		exceptionInformation.addTags("Type", type);
		exceptionInformation.addTags("Message", message);

		AdditionalInformation additionalInformation = new AdditionalInformation();

		return new ExceptionProtocol(this.protocol.getProtocolVersion(),
				this.protocol.getId(), generalInformation,
				exceptionInformation, additionalInformation);
	}

}
