package hk.edu.polyu.intercloud.command.vm;

import hk.edu.polyu.intercloud.command.Command;
import hk.edu.polyu.intercloud.common.Common;
import hk.edu.polyu.intercloud.connector.hyperv.HyperVIntercloud;
import hk.edu.polyu.intercloud.connector.hyperv.HyperVIntercloudException;
import hk.edu.polyu.intercloud.connector.hyperv.VM;
import hk.edu.polyu.intercloud.exceptions.DatabaseException;
import hk.edu.polyu.intercloud.exceptions.IntercloudException;
import hk.edu.polyu.intercloud.exceptions.SecurityException;
import hk.edu.polyu.intercloud.exceptions.VirtualMachineException;
import hk.edu.polyu.intercloud.model.protocol.AdditionalInformation;
import hk.edu.polyu.intercloud.model.protocol.ExceptionInformation;
import hk.edu.polyu.intercloud.model.protocol.ExceptionProtocol;
import hk.edu.polyu.intercloud.model.protocol.GeneralInformation;
import hk.edu.polyu.intercloud.model.protocol.Protocol;
import hk.edu.polyu.intercloud.model.protocol.RequestInformation;
import hk.edu.polyu.intercloud.model.protocol.ResponseInformation;
import hk.edu.polyu.intercloud.security.Digest;
import hk.edu.polyu.intercloud.util.DatabaseUtil;
import hk.edu.polyu.intercloud.util.LogUtil;

import java.io.File;
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

import org.apache.commons.io.FileUtils;
import org.xml.sax.SAXException;

import com.vmware.vim25.mo.samples.VMList;
import com.vmware.vim25.mo.samples.VMPoweroff;
import com.vmware.vim25.mo.samples.ovf.ExportOvfToLocal;
import com.vmware.vim25.mo.samples.ovf.GetVMDetails_api;

/**
 * 
 * @author Kate
 *
 */
public class GetVM implements Command {
	private Protocol protocol;
	private GeneralInformation generalInformation;
	private RequestInformation requestInformation;
	private AdditionalInformation additionalInformation;
	private Map<String, String> details;
	private VM vm = new VM();

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
		try {
			initialization();
			List<String> list = new ArrayList<>();
			String cloudType = Common.my_service_providers.get("VM");
			if (o.size() != 1) {
				return null;
			}
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
				// create a file for the vm
				boolean success = (new File(Common.DOWNLOAD_PATH + vmname
						+ File.separator)).mkdirs();
				ExportOvfToLocal a = new ExportOvfToLocal();
				System.out.println(Common.DOWNLOAD_PATH + vmname
						+ File.separator);
				VMPoweroff off = new VMPoweroff();
				off.VMPoweroff_execute(vmname);
				list = a.ExportOvfToLocal_execute(vmname, Common.DOWNLOAD_PATH
						+ vmname + File.separator);
				GetVMDetails_api vm = new GetVMDetails_api();
				details = vm.GetVMDetails_execute(vmname);
				System.out.print("xxxxxxxxxxxxxxxxxxxxx"
						+ details.get("MemorySize"));
			} else if (cloudType.equalsIgnoreCase("hyper-v")) {
				boolean success = (new File(Common.DOWNLOAD_PATH + vmname
						+ File.separator)).mkdirs();
				if (!success) {
					File f = new File(Common.DOWNLOAD_PATH + vmname
							+ File.separator);
					FileUtils.deleteDirectory(f);
					(new File(Common.DOWNLOAD_PATH + vmname + File.separator))
							.mkdirs();
				}
				HyperVIntercloud hyper = new HyperVIntercloud();
				hyper.shutdownVM(vmname, true);
				vm = hyper.getVMDetails(vmname);
				list = hyper.getVM(vmname, Common.DOWNLOAD_PATH + vmname
						+ File.separator);
			}

			String transferMethod = this.protocol.getRequestInformation()
					.getValue("TranferMethod");
			String digest_string = "";
			String[] digest = new String[list.size()];
			String part1 = "";
			String path = "";
			int port = 0;
			if (transferMethod.equalsIgnoreCase("HTTP")) {
				part1 = "http://";
				port = Common.HTTP_PORT;
			} else if (transferMethod.equalsIgnoreCase("HTTPS")) {
				part1 = "https://";
				port = Common.HTTPS_PORT;
			}
			for (int i = 0; i < list.size(); i++) {
				if (cloudType.equalsIgnoreCase("hyper-v")
						&& !(list.get(i).endsWith(".vhd") || list.get(i)
								.endsWith(".vhdx"))) {
					continue;
				}
				digest[i] = Digest.digestFile(list.get(i));
				String part2 = list.get(i).replace(Common.DOWNLOAD_PATH, "")
						.replace("\\", "/").replace("//", "/");
				if (part2.startsWith("/")) {
					part2 = part2.substring(1);
				}
				path += part1 + Common.my_ip + ":" + port + "/" + vmname + "/"
						+ part2 + ";";
			}
			if (path.endsWith(";")) {
				path = path.substring(0, path.length() - 1);
			}
			return this.generateProtocol(owner_vmname, vmname, transferMethod,
					cloudType, path, digest_string);
		} catch (ClassNotFoundException | SQLException e) {
			LogUtil.logException(e);
			return this.generateException("2",
					DatabaseException.class.getSimpleName(), e.getMessage());
		} catch (HyperVIntercloudException e) {
			LogUtil.logException(e);
			return this.generateException("199",
					VirtualMachineException.class.getSimpleName(),
					e.getMessage());
		} catch (IOException e) {
			LogUtil.logException(e);
			return this.generateException("0",
					IntercloudException.class.getSimpleName(), e.getMessage());
		} catch (SecurityException e) {
			LogUtil.logException(e);
			return this.generateException("0",
					SecurityException.class.getSimpleName(), e.getMessage());
		} catch (Exception e) {
			LogUtil.logException(e);
			return this.generateException("199",
					VirtualMachineException.class.getSimpleName(),
					e.getMessage());
		}

	}

	@Override
	public void initialization() {
		this.generalInformation = protocol.getGeneralInformation();
		this.requestInformation = protocol.getRequestInformation();
		this.additionalInformation = protocol.getAdditionalInformation();
	}

	public Protocol generateProtocol(String owner_vmname, String vmname,
			String transferMethod, String cloudType, String path, String digest) {

		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date date = new Date();
		String[] dateTime = dateFormat.format(date).split("\\s+");

		GeneralInformation generalInformation = new GeneralInformation(
				this.protocol.getGeneralInformation().getTo(), this.protocol
						.getGeneralInformation().getFrom(), dateTime[0],
				dateTime[1]);

		ResponseInformation responseInformation = new ResponseInformation();
		responseInformation.setService("VM");
		responseInformation.setCommand("ConfirmationForGetVM");
		responseInformation.addTags("VMName", owner_vmname);
		responseInformation.addTags("VMVersion", vmname);
		responseInformation.addTags("TransferMethod", transferMethod);

		AdditionalInformation additionalInformation = new AdditionalInformation();

		additionalInformation.addTags("IP", Common.my_ip);
		additionalInformation.addTags("Path", path);

		switch (transferMethod) {
		case "HTTP":
			additionalInformation.addTags("Port",
					Integer.toString(Common.HTTP_PORT));
			break;
		case "HTTPS":
			additionalInformation.addTags("Port",
					Integer.toString(Common.HTTPS_PORT));
			break;
		}
		if (cloudType.equalsIgnoreCase("vmware")) {

			additionalInformation.addTags("MemorySize",
					details.get("MemorySize").toString());
			additionalInformation.addTags("CPU", details.get("CPUCore")
					.toString());
			additionalInformation.addTags("NumEthernetCards",
					details.get("NumEthernetCards").toString());
		} else if (cloudType.equalsIgnoreCase("hyper-v")) {

			additionalInformation.addTags("MemorySize",
					String.valueOf(vm.getMemoryB() / 1024 / 1024));
			additionalInformation.addTags("CPU",
					String.valueOf(vm.getNumberOfCPUCores()));
			additionalInformation.addTags("NumEthernetCards",
					String.valueOf(vm.getNetworkAdapters().size()));
		}
		// additionalInformation.addTags("HardDiskSize",
		// details.get("HardDiskSize").toString());

		String body = "";
		return new Protocol(this.protocol.getProtocolVersion(),
				this.protocol.getId(), generalInformation, null,
				responseInformation, additionalInformation, body);
	}

	@Override
	public AdditionalInformation pre_execute(String protocol,
			HashMap<String, String> info, String systemType)
			throws ParserConfigurationException, SAXException, IOException,
			Exception {
		return null;
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
		exceptionInformation.addTags("Command", "GetVM");
		exceptionInformation.addTags("Code", code);
		exceptionInformation.addTags("Type", type);
		exceptionInformation.addTags("Message", message);

		AdditionalInformation additionalInformation = new AdditionalInformation();

		return new ExceptionProtocol(this.protocol.getProtocolVersion(),
				this.protocol.getId(), generalInformation,
				exceptionInformation, additionalInformation);
	}

}
