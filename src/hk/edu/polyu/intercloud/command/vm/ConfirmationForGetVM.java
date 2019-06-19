package hk.edu.polyu.intercloud.command.vm;

import hk.edu.polyu.intercloud.command.Command;
import hk.edu.polyu.intercloud.common.Common;
import hk.edu.polyu.intercloud.connector.hyperv.HyperVIntercloud;
import hk.edu.polyu.intercloud.connector.hyperv.HyperVIntercloudException;
import hk.edu.polyu.intercloud.connector.hyperv.VHD;
import hk.edu.polyu.intercloud.exceptions.IntercloudException;
import hk.edu.polyu.intercloud.fileserver.client.Http;
import hk.edu.polyu.intercloud.fileserver.exceptions.HttpException;
import hk.edu.polyu.intercloud.model.protocol.AdditionalInformation;
import hk.edu.polyu.intercloud.model.protocol.GeneralInformation;
import hk.edu.polyu.intercloud.model.protocol.Protocol;
import hk.edu.polyu.intercloud.model.protocol.ResponseInformation;
import hk.edu.polyu.intercloud.util.LogUtil;
import hk.edu.polyu.intercloud.util.PropertiesReader;
import hk.edu.polyu.intercloud.util.VHDUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.FilenameUtils;

import com.vmware.vim25.mo.samples.ovf.ImportLocalOvfVApp;

/**
 * 
 * @author Kate
 *
 */
public class ConfirmationForGetVM implements Command {
	private Protocol protocol;
	private GeneralInformation generalInformation;
	// private RequestInformation requestInformation;
	private ResponseInformation responseInformation;
	private AdditionalInformation additionalInformation;
	private PropertiesReader propertiesUtil;
	private String vmname;
	private String configfile;

	@Override
	public void setProtocol(Protocol protocol) {

		this.protocol = protocol;
	}

	@Override
	public Protocol getProtocol() {

		return this.protocol;
	}

	/**
	 * This method converts disk format and import to Hypervisors. There are 4
	 * situations (vhdx-vmdk/vmdk-vhdx/vhdx-vhdx/vmdk-vmdk). Two of them need
	 * convert format.
	 * 
	 * @List<Object> o is null.List o is set for future use
	 * 
	 * @return return protocol object
	 */
	@Override
	public Protocol execute(List<Object> o) {
		try {
			String cloudType = Common.my_service_providers.get("VM");
			String paths = this.protocol.getAdditionalInformation().getValue(
					"Path");
			String ip = this.protocol.getAdditionalInformation().getValue("IP");
			String transferMethod = this.protocol.getResponseInformation()
					.getValue("TransferMethod");
			String vmname = this.protocol.getResponseInformation().getValue(
					"VMName");
			String vmversion = this.protocol.getResponseInformation().getValue(
					"VMVersion");
			String new_vmname = vmname + "_" + vmversion;
			int port = Integer.parseInt(this.protocol
					.getAdditionalInformation().getValue("Port"));
			System.out.println(paths);
			String path[] = paths.split(";");

			if (o.size() != 1) {
				return null;
			}

			// Retrieving path
			File[] tmp = new File[path.length];

			// Form new path for retrieving
			for (int i = 0; i < path.length; i++) {
				String baseName = FilenameUtils.getBaseName(path[i]);
				String extension = FilenameUtils.getExtension(path[i]);
				tmp[i] = new File(Common.RETRIEVE_PATH + vmversion + "/"
						+ vmname + "/" + baseName + "." + extension);
				System.out.println("------" + tmp[i].getPath() + "------");

			}
			// Retrieve files
			String[] pathRetrieve = new String[path.length];

			for (int j = 0; j < path.length; j++) {
				// Get retrieve path and digest
				String result = fileRetrieve(path[j], tmp[j].getPath());
				pathRetrieve[j] = result;
			}

			// Convert format we have 4 situations. 2 of 4 situations need
			// convert format.

			System.out.println("-----New VM name is " + new_vmname);
			if (cloudType.equalsIgnoreCase("vmware")) {
				List<String> list = new ArrayList<>(path.length);
				// list has disks and configfile has ovf file
				for (int i = 0; i < path.length; i++) {
					if (FilenameUtils.getExtension(tmp[i].getPath())
							.equalsIgnoreCase("vmdk")
							|| FilenameUtils.getExtension(tmp[i].getPath())
									.equalsIgnoreCase("vhdx")) {

						list.add(tmp[i].getPath());
					} else if (FilenameUtils.getExtension(tmp[i].getPath())
							.equalsIgnoreCase("ovf")) {
						configfile = tmp[i].getPath();
					}
				}

				// Case 1 VMware to VMware
				for (int i = 0; i < path.length; i++) {
					if (FilenameUtils.getExtension(tmp[i].getPath())
							.equalsIgnoreCase("vmdk")) {
						ImportLocalOvfVApp a = new ImportLocalOvfVApp();
						a.ImportLocalOvfVApp_execute(new_vmname, configfile);
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
									+ "/retrieve/" + this.protocol.getId()
									+ "/" + baseName + ".vhdx";
							VHDUtil.convertHD(v.getPath(), topath, "vhdx");
							v.setPath(topath);
						}
					}
				}

				HyperVIntercloud h = new HyperVIntercloud();
				h.newVM(new_vmname, Integer.valueOf(cpu), memorysizeGB, vhds,
						null, null, Integer.valueOf(numethernetcards), false);
			}
		} catch (HttpException e) {
			LogUtil.logException(e);
		} catch (HyperVIntercloudException e) {
			LogUtil.logException(e);
		} catch (IntercloudException e) {
			LogUtil.logException(e);
		} catch (Exception e) {
			LogUtil.logException(e);
		}
		return new Protocol(null, null, null, null, null, null, null);
	}

	@Override
	public void initialization() {
		this.generalInformation = protocol.getGeneralInformation();
		this.responseInformation = protocol.getResponseInformation();
		this.additionalInformation = protocol.getAdditionalInformation();
	}

	@Override
	public AdditionalInformation pre_execute(String protocol,
			HashMap<String, String> info, String systemType) {
		return null;
	}

	private String fileRetrieve(String path, String tmpPath)
			throws HttpException {
		Http.download(path, tmpPath);
		return tmpPath;
	}

}
