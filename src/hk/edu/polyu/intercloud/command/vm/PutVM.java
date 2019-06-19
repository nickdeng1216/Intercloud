package hk.edu.polyu.intercloud.command.vm;

import hk.edu.polyu.intercloud.command.Command;
import hk.edu.polyu.intercloud.common.Common;
import hk.edu.polyu.intercloud.connector.hyperv.HyperVIntercloud;
import hk.edu.polyu.intercloud.connector.hyperv.HyperVIntercloudException;
import hk.edu.polyu.intercloud.connector.hyperv.VHD;
import hk.edu.polyu.intercloud.connector.hyperv.VM;
import hk.edu.polyu.intercloud.exceptions.DatabaseException;
import hk.edu.polyu.intercloud.exceptions.IntercloudException;
import hk.edu.polyu.intercloud.exceptions.VirtualMachineException;
import hk.edu.polyu.intercloud.fileserver.client.Http;
import hk.edu.polyu.intercloud.fileserver.exceptions.HttpException;
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
import hk.edu.polyu.intercloud.util.VHDUtil;

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
import org.apache.commons.io.FilenameUtils;
import org.xml.sax.SAXException;

import com.vmware.vim25.mo.samples.VMPoweroff;
import com.vmware.vim25.mo.samples.ovf.ExportOvfToLocal;
import com.vmware.vim25.mo.samples.ovf.GetVMDetails_api;
import com.vmware.vim25.mo.samples.ovf.ImportLocalOvfVApp;

/**
 * 
 * @author Kate
 *
 */
public class PutVM implements Command {
	private Protocol protocol;
	private GeneralInformation generalInformation;
	private RequestInformation requestInformation;
	private ResponseInformation responseInformation;
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

		try {
			initialization();
			String cloudType = Common.my_service_providers.get("VM");
			String paths = this.protocol.getAdditionalInformation().getValue(
					"Path");
			String owner_vmname = this.protocol.getRequestInformation()
					.getValue("VMName");
			String path[] = paths.split(";");
			String cofigfile = null;
			if (o.size() != 1) {
				return null;
			}

			// Retrieving path
			File[] tmp = new File[path.length];
			File file = new File(Common.RETRIEVE_PATH + owner_vmname);
			if (file.exists())
				FileUtils.deleteDirectory(file);
			String vmname = Long.toString(System.currentTimeMillis());

			// Form new path for retrieving
			for (int i = 0; i < path.length; i++) {
				String baseName = FilenameUtils.getBaseName(path[i]);
				String extension = FilenameUtils.getExtension(path[i]);
				tmp[i] = new File(Common.RETRIEVE_PATH + owner_vmname + "/"
						+ baseName + "." + extension);
				System.out.println("------" + tmp[i].getPath() + "------");

			}
			// Retrieve files
			String[] pathRetrieve = new String[path.length];

			for (int j = 0; j < path.length; j++) {
				// Get retrieve path and digest
				String[] result = fileRetrieve(path[j], tmp[j].getPath());
				pathRetrieve[j] = result[0];
			}

			// Convert format we have 4 situations. 2 of 4 situations need
			// convert format.
			if (cloudType.equalsIgnoreCase("vmware")) {

				ArrayList<String> list = new ArrayList<>(path.length);
				// list has disks and configfile has ovf file
				for (int i = 0; i < path.length; i++) {
					if (FilenameUtils.getExtension(tmp[i].getPath())
							.equalsIgnoreCase("vmdk")
							|| FilenameUtils.getExtension(tmp[i].getPath())
									.equalsIgnoreCase("vhdx")) {

						list.add(tmp[i].getPath());
					} else if (FilenameUtils.getExtension(tmp[i].getPath())
							.equalsIgnoreCase("ovf")) {
						cofigfile = tmp[i].getPath();
					}
				}

				// Case 1 VMware to VMware
				for (int i = 0; i < path.length; i++) {
					if (FilenameUtils.getExtension(tmp[i].getPath())
							.equalsIgnoreCase("vmdk")) {
						ImportLocalOvfVApp a = new ImportLocalOvfVApp();
						a.ImportLocalOvfVApp_execute(vmname, cofigfile);
					}
				}

				// Case 2 Hyper-V to VMware
				// TODO ...

			} else if (cloudType.equalsIgnoreCase("Hyper-v")) {

				String numethernetcards = this.protocol
						.getAdditionalInformation()
						.getValue("NumEthernetCards");
				String cpu = this.protocol.getAdditionalInformation().getValue(
						"CPU");
				String memorysize = this.protocol.getAdditionalInformation()
						.getValue("MemorySize");
				int memorysizeGB = Integer.valueOf(memorysize) / 1024;

				List<VHD> vhds = new ArrayList<VHD>();

				// may have many disks
				for (int i = 0; i < path.length; i++) {
					if (FilenameUtils.getExtension(tmp[i].getPath())
							.equalsIgnoreCase("vmdk")
							|| FilenameUtils.getExtension(tmp[i].getPath())
									.equalsIgnoreCase("vhdx")) {
						VHD v = new VHD(tmp[i].getPath(), 5);
						vhds.add(v);
					}
				}

				// Case 3 Hyper-V to Hyper-V
				// No need to convert

				// Case 4 VMware to Hyper-V
				for (int i = 0; i < path.length; i++) {
					if (FilenameUtils.getExtension(tmp[i].getPath())
							.equalsIgnoreCase("vmdk")) {
						for (VHD v : vhds) {
							String baseName = FilenameUtils.getBaseName(v
									.getPath());
							String extension = FilenameUtils.getExtension(v
									.getPath());
							String topath;
							topath = System.getProperty("user.dir")
									+ File.separator + "retrieve"
									+ File.separator + this.protocol.getId()
									+ File.separator + baseName + ".vhdx";
							VHDUtil.convertHD(v.getPath(), topath, "vhdx");
							v.setPath(topath);
						}
					}
				}
				HyperVIntercloud h = new HyperVIntercloud();
				h.newVM(vmname, Integer.valueOf(cpu), memorysizeGB, vhds, null,
						null, Integer.valueOf(numethernetcards), false);
			}
			DatabaseUtil.insertOthersVMTable(this.protocol.getId(), vmname,
					this.protocol.getGeneralInformation().getFrom(),
					owner_vmname);
			File f = new File(Common.RETRIEVE_PATH + vmname + File.separator);
			f.delete();
			return generateProtocol(vmname, owner_vmname);
		} catch (IOException | HttpException | IntercloudException e) {
			LogUtil.logException(e);
			return this.generateException("0",
					IntercloudException.class.getSimpleName(), e.getMessage());
		} catch (HyperVIntercloudException e) {
			LogUtil.logException(e);
			return this.generateException("199",
					VirtualMachineException.class.getSimpleName(),
					e.getMessage());
		} catch (ClassNotFoundException | SQLException e) {
			LogUtil.logException(e);
			return this.generateException("2",
					DatabaseException.class.getSimpleName(), e.getMessage());
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
		Map details = null;

		List<String> list = new ArrayList<String>();
		VM vm = new VM();
		String cloudType = Common.my_service_providers.get("VM");
		String vmName = info.get("VMName");
		String transferMethod = info.get("TransferMethod");

		if (cloudType.equalsIgnoreCase("vmware")) {
			// create a file for the vm
			boolean success = (new File(Common.DOWNLOAD_PATH + vmName
					+ File.separator)).mkdirs();
			if (!success) {
				File f = new File(Common.DOWNLOAD_PATH + vmName
						+ File.separator);
				FileUtils.deleteDirectory(f);
				(new File(Common.DOWNLOAD_PATH + vmName + File.separator))
						.mkdirs();
			}

			ExportOvfToLocal a = new ExportOvfToLocal();
			System.out.println(Common.DOWNLOAD_PATH + vmName + File.separator);
			VMPoweroff off = new VMPoweroff();
			off.VMPoweroff_execute(vmName);

			list = a.ExportOvfToLocal_execute(vmName, Common.DOWNLOAD_PATH
					+ vmName + File.separator);

			GetVMDetails_api details_api = new GetVMDetails_api();
			details = details_api.GetVMDetails_execute(vmName);
			System.out.print("xxxxxxxxxxxxxxxxxxxxx"
					+ details.get("MemorySize"));

		} else if (cloudType.equalsIgnoreCase("hyper-v")) {
			boolean success = (new File(Common.DOWNLOAD_PATH + vmName
					+ File.separator)).mkdirs();
			if (!success) {
				File f = new File(Common.DOWNLOAD_PATH + vmName
						+ File.separator);
				FileUtils.deleteDirectory(f);
				(new File(Common.DOWNLOAD_PATH + vmName + File.separator))
						.mkdirs();
			}
			HyperVIntercloud hyper = new HyperVIntercloud();
			hyper.shutdownVM(vmName, true);
			vm = hyper.getVMDetails(vmName);
			list = hyper.getVM(vmName, Common.DOWNLOAD_PATH + vmName
					+ File.separator);

		}

		// make path and digest
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
					&& !(list.get(i).endsWith(".vhd") || list.get(i).endsWith(
							".vhdx"))) {
				continue;
			}
			digest[i] = Digest.digestFile(list.get(i));
			String part2 = list.get(i).replace(Common.DOWNLOAD_PATH, "")
					.replace("\\", "/").replace("//", "/");
			if (part2.startsWith("/")) {
				part2 = part2.substring(1);
			}
			if (cloudType.equalsIgnoreCase("vmware")) {
				path += part1 + Common.my_ip + ":" + port + "/" + part2 + ";";
			} else if (cloudType.equalsIgnoreCase("hyper-v")) {
				path += part1 + Common.my_ip + ":" + port + "/" + vmName + "/"
						+ part2 + ";";
			}
		}
		if (path.endsWith(";")) {
			path = path.substring(0, path.length() - 1);
		}

		// make additional tag
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
			additionalInformation.addTags("GuestOS", details.get("GuestOS")
					.toString());
			additionalInformation.addTags("NumEthernetCards",
					details.get("NumEthernetCards").toString());
		} else if (cloudType.equalsIgnoreCase("hyper-v")) {

			additionalInformation.addTags("MemorySize",
					String.valueOf(vm.getMemoryB() / 1024 / 1024));
			additionalInformation.addTags("CPU",
					String.valueOf(vm.getNumberOfCPUCores()));
			additionalInformation
					.addTags("GuestOS", String.valueOf(vm.getOs()));
			additionalInformation.addTags("NumEthernetCards",
					String.valueOf(vm.getNetworkAdapters().size()));
		}
		return additionalInformation;
	}

	@Override
	public void initialization() {
		this.generalInformation = protocol.getGeneralInformation();
		this.requestInformation = protocol.getRequestInformation();
		this.additionalInformation = protocol.getAdditionalInformation();
	}

	private String[] fileRetrieve(String path, String tmpPath)
			throws HttpException {
		Http.download(path, tmpPath);
		return new String[] { tmpPath };
	}

	private Protocol generateProtocol(String name, String name_original) {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date date = new Date();
		String[] dateTime = dateFormat.format(date).split("\\s+");
		GeneralInformation generalInformation = new GeneralInformation(
				this.protocol.getGeneralInformation().getTo(), this.protocol
						.getGeneralInformation().getFrom(), dateTime[0],
				dateTime[1]);

		RequestInformation requestInformation = null;

		ResponseInformation responseInformation = new ResponseInformation();
		responseInformation.setCommand("ConfirmationForPutVM");
		responseInformation.setService("VM");
		responseInformation.addTags("VMName", name);

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
		exceptionInformation.addTags("Command", "GetVMDetails");
		exceptionInformation.addTags("Code", code);
		exceptionInformation.addTags("Type", type);
		exceptionInformation.addTags("Message", message);

		AdditionalInformation additionalInformation = new AdditionalInformation();

		return new ExceptionProtocol(this.protocol.getProtocolVersion(),
				this.protocol.getId(), generalInformation,
				exceptionInformation, additionalInformation);
	}

}
