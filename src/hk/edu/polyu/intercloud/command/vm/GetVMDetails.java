package hk.edu.polyu.intercloud.command.vm;

import hk.edu.polyu.intercloud.command.Command;
import hk.edu.polyu.intercloud.common.Common;
import hk.edu.polyu.intercloud.connector.hyperv.HyperVIntercloud;
import hk.edu.polyu.intercloud.connector.hyperv.HyperVIntercloudException;
import hk.edu.polyu.intercloud.connector.hyperv.VM;
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
import com.vmware.vim25.mo.samples.ovf.GetVMDetails_api;

/**
 * 
 * @author Kate
 *
 */
public class GetVMDetails implements Command {
	private Protocol protocol;
	private GeneralInformation generalInformation;
	private RequestInformation requestInformation;
	private AdditionalInformation additionalInformation;
	Map<String, String> details = new HashMap<>();
	VM vm = new VM();

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
			String cloudType = Common.my_service_providers.get("VM");
			String owner_vmname = this.protocol.getRequestInformation()
					.getValue("VMName");
			String vmname = "";
			if (this.protocol.getRequestInformation().getKeys()
					.contains("VMVersion")) {
				// If the request has specified the version, use it.
				vmname = this.protocol.getRequestInformation().getValue(
						"VMVersion");
				System.out.println("VMVersion specified: " + vmname);
			} else {
				// Otherwise, get the latest version.
				// 1. Check the DB
				List<Long> vmNamesDB = DatabaseUtil.getLatestVMName(
						owner_vmname, this.generalInformation.getFrom());
				// 2. Check the hypervisor
				List<String> vmNamesHV = new ArrayList<>();
				if (cloudType.equalsIgnoreCase("vmware")) {
					VMList vmware = new VMList();
					vmNamesHV = vmware.VMList_execute();
				} else if (cloudType.equalsIgnoreCase("hyper-v")) {
					HyperVIntercloud hyper = new HyperVIntercloud();
					vmNamesHV = hyper.getVMList();
				}
				// 3. If both DB and hypervisor contain, then add
				for (long vmName : vmNamesDB) {
					if (vmNamesHV.contains(String.valueOf(vmName))) {
						vmname = String.valueOf(vmName);
						System.out
								.println("VMVersion not specified, use the latest: "
										+ vmname);
						break;
					}
				}
			}
			if (cloudType.equalsIgnoreCase("vmware")) {
				GetVMDetails_api api = new GetVMDetails_api();
				details = api.GetVMDetails_execute(vmname);
			} else if (cloudType.equalsIgnoreCase("hyper-v")) {
				HyperVIntercloud a = new HyperVIntercloud();
				vm = a.getVMDetails(vmname);
			}
			return this.generateProtocol(owner_vmname, vmname, cloudType);
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
		exceptionInformation.addTags("Command", "GetVMDetails");
		exceptionInformation.addTags("Code", code);
		exceptionInformation.addTags("Type", type);
		exceptionInformation.addTags("Message", message);

		AdditionalInformation additionalInformation = new AdditionalInformation();

		return new ExceptionProtocol(this.protocol.getProtocolVersion(),
				this.protocol.getId(), generalInformation,
				exceptionInformation, additionalInformation);
	}

	private Protocol generateProtocol(String owner_vmname, String vmname,
			String cloudType) {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date date = new Date();
		String[] dateTime = dateFormat.format(date).split("\\s+");
		GeneralInformation generalInformation = new GeneralInformation(
				this.protocol.getGeneralInformation().getTo(), this.protocol
						.getGeneralInformation().getFrom(), dateTime[0],
				dateTime[1]);

		RequestInformation requestInformation = null;

		ResponseInformation responseInformation = new ResponseInformation();
		responseInformation.setCommand("VMDetails");
		responseInformation.setService("VM");
		responseInformation.addTags("VMName", owner_vmname);
		responseInformation.addTags("VMVersion", vmname);

		AdditionalInformation additionalInformation = new AdditionalInformation();
		if (cloudType.equalsIgnoreCase("vmware")) {
			additionalInformation.addTags("MemorySize",
					details.get("MemorySize"));
			additionalInformation
					.addTags("CPUSocket", details.get("CPUSocket"));
			additionalInformation.addTags("CPUCore", details.get("CPUCore"));
			additionalInformation.addTags("NumEthernetCards",
					details.get("NumEthernetCards"));
			additionalInformation.addTags("GuestOS", details.get("GuestOS"));
			additionalInformation.addTags("PowerState",
					details.get("PowerState"));
		} else if (cloudType.equalsIgnoreCase("hyper-v")) {
			additionalInformation.addTags("MemorySize",
					Long.toString(vm.getMemoryB() / 1024 / 1024));
			additionalInformation.addTags("CPUSocket", "1");
			additionalInformation.addTags("CPUCore",
					Integer.toString(vm.getNumberOfCPUCores()));
			additionalInformation.addTags("NumEthernetCards",
					Integer.toString(vm.getNetworkAdapters().size()));
			additionalInformation.addTags("GuestOS", vm.getOs());
			additionalInformation.addTags("PowerState", vm.getState());

		}

		Protocol protocol = new Protocol(this.protocol.getProtocolVersion(),
				this.protocol.getId(), generalInformation, requestInformation,
				responseInformation, additionalInformation, null);

		return protocol;
	}

}
