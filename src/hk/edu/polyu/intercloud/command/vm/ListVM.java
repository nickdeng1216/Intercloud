package hk.edu.polyu.intercloud.command.vm;

import hk.edu.polyu.intercloud.command.Command;
import hk.edu.polyu.intercloud.common.Common;
import hk.edu.polyu.intercloud.connector.hyperv.HyperVIntercloud;
import hk.edu.polyu.intercloud.connector.hyperv.HyperVIntercloudException;
import hk.edu.polyu.intercloud.exceptions.DatabaseException;
import hk.edu.polyu.intercloud.exceptions.VirtualMachineException;
import hk.edu.polyu.intercloud.model.protocol.AdditionalInformation;
import hk.edu.polyu.intercloud.model.protocol.ExceptionInformation;
import hk.edu.polyu.intercloud.model.protocol.ExceptionProtocol;
import hk.edu.polyu.intercloud.model.protocol.GeneralInformation;
import hk.edu.polyu.intercloud.model.protocol.Protocol;
import hk.edu.polyu.intercloud.model.protocol.RequestInformation;
import hk.edu.polyu.intercloud.model.protocol.ResponseInformation;
import hk.edu.polyu.intercloud.util.DatabaseUtil;
import hk.edu.polyu.intercloud.util.LogUtil;

import java.io.IOException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.vmware.vim25.mo.samples.VMList;

/**
 * 
 * @author Kate
 *
 */
public class ListVM implements Command {
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
		return protocol;
	}

	@Override
	public Protocol execute(List<Object> o) {
		initialization();
		try {
			List<String> list = new ArrayList<>();
			// 1. Check the DB
			Map<Long, String> ownerVMsDB = DatabaseUtil
					.getVMOthersMap(this.generalInformation.getFrom());
			// 2. Check the hypervisor
			List<String> ownerVMsHV = new ArrayList<>();
			String cloudType = Common.my_service_providers.get("VM");
			if (cloudType.equalsIgnoreCase("vmware")) {
				VMList vmware = new VMList();
				ownerVMsHV = vmware.VMList_execute();
			} else if (cloudType.equalsIgnoreCase("hyper-v")) {
				HyperVIntercloud hyper = new HyperVIntercloud();
				ownerVMsHV = hyper.getVMList();
			}
			// 3. If both DB and hypervisor contain, then add
			for (long ownerVM : ownerVMsDB.keySet()) {
				if (ownerVMsHV.contains(String.valueOf(ownerVM))) {
					list.add("\"" + ownerVMsDB.get(ownerVM) + "\":" + ownerVM);
				}
			}
			return this.generateProtocol(list);
		} catch (ClassNotFoundException | SQLException e) {
			LogUtil.logException(e);
			return this.generateException("2",
					DatabaseException.class.getSimpleName(), e.getMessage());
		} catch (HyperVIntercloudException e) {
			LogUtil.logException(e);
			return this.generateException("199",
					VirtualMachineException.class.getSimpleName(),
					e.getMessage());
		} catch (Exception e) {
			LogUtil.logException(e);
			return this.generateException("199",
					VirtualMachineException.class.getSimpleName(),
					e.getMessage());
		}
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
		this.requestInformation = protocol.getRequestInformation();
		this.additionalInformation = protocol.getAdditionalInformation();
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
		exceptionInformation.addTags("Command", "GetVMList");
		exceptionInformation.addTags("Code", code);
		exceptionInformation.addTags("Type", type);
		exceptionInformation.addTags("Message", message);

		AdditionalInformation additionalInformation = new AdditionalInformation();

		return new ExceptionProtocol(this.protocol.getProtocolVersion(),
				this.protocol.getId(), generalInformation,
				exceptionInformation, additionalInformation);
	}

	private Protocol generateProtocol(List<String> list) {
		String namelist = "";
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date date = new Date();
		String[] dateTime = dateFormat.format(date).split("\\s+");
		GeneralInformation generalInformation = new GeneralInformation(
				this.protocol.getGeneralInformation().getTo(), this.protocol
						.getGeneralInformation().getFrom(), dateTime[0],
				dateTime[1]);

		RequestInformation requestInformation = null;

		ResponseInformation responseInformation = new ResponseInformation();
		responseInformation.setCommand("VMList");
		responseInformation.setService("VM");
		for (int i = 0; i < list.size(); i++) {
			if (i == list.size() - 1)
				namelist += list.get(i);
			else
				namelist += list.get(i) + ",";
		}
		responseInformation.addTags("VMName", namelist);

		AdditionalInformation additionalInformation = new AdditionalInformation();

		String body = "";

		Protocol protocol = new Protocol(this.protocol.getProtocolVersion(),
				this.protocol.getId(), generalInformation, requestInformation,
				responseInformation, additionalInformation, null);

		return protocol;
	}

}
