package hk.edu.polyu.intercloud.connector.hyperv;

import hk.edu.polyu.intercloud.util.CmdExecutor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

class HyperVUtil {

	public void createVM(String ip, String name, int numberOfCPUCores,
			int memoryGB, List<VHD> vhds, List<VHD> newVHDs, List<String> isos,
			int numberOfNetworkAdapters) throws HyperVIntercloudException {
		System.out.println("Creating " + name + " on " + ip + "...");
		// 1. Create a VM with memory and a VHD.
		makeVM(ip, name, memoryGB);
		// 2. Set CPU.
		setCPU(ip, name, numberOfCPUCores);
		// 3. Add existing VHDs.
		if (vhds != null && vhds.size() > 0) {
			for (VHD v : vhds) {
				addHD(ip, name, v);
			}
		}
		// 4. Add new VHDs.
		if (newVHDs != null && newVHDs.size() > 0) {
			for (VHD v : newVHDs) {
				addNewHD(ip, name, v);
			}
		}
		// 5. Add new DVDs.
		if (isos != null && isos.size() > 0) {
			for (String iso : isos) {
				addDVD(ip, name, iso);
			}
		}
		// 6. Add new network adapters.
		if (numberOfNetworkAdapters > 0) {
			addNetworkAdapter(ip, name);
		}
	}

	private void makeVM(String ip, String name, int memoryGB)
			throws HyperVIntercloudException {
		if (memoryGB < 1) {
			memoryGB = 1;
		}
		long memoryB = (long) memoryGB * 1024 * 1024 * 1024;
		String command = "New-VM -ComputerName '" + ip + "' -Name '" + name
				+ "' –MemoryStartupBytes " + memoryB;
		try {
			String output = CmdExecutor.runSinglePsCmd(command);
			if (!output.contains("Operating normally")) {
				throw new HyperVIntercloudException("Unable to create " + name
						+ " on " + ip + System.getProperty("line.separator")
						+ "Output: " + output);
			}
			System.out.println(name + " on " + ip + " successfully created...");
		} catch (IOException e) {
			throw new HyperVIntercloudException("Unable to create " + name
					+ " on " + ip + System.getProperty("line.separator")
					+ "IOException: " + e.getStackTrace().toString());
		}
	}

	private void setCPU(String ip, String name, int numberOfCPUCores)
			throws HyperVIntercloudException {
		String command = "Set-VMProcessor -ComputerName '" + ip + "' -VMName '"
				+ name + "' –Count " + numberOfCPUCores;
		try {
			String output = CmdExecutor.runSinglePsCmd(command);
			if (!output.equals("")) {
				throw new HyperVIntercloudException("Unable to set the CPU of "
						+ name + " on " + ip
						+ System.getProperty("line.separator") + "Output: "
						+ output);
			}
			System.out.println("The CPU of " + name + " on " + ip
					+ " successfully set...");
		} catch (IOException e) {
			throw new HyperVIntercloudException("Unable to set the CPU of "
					+ name + " on " + ip + System.getProperty("line.separator")
					+ "IOException: " + e.getStackTrace().toString());
		}
	}

	private void addHD(String ip, String name, VHD v)
			throws HyperVIntercloudException {
		String path = "";
		String type = v.getPath().split("\\.")[v.getPath().split("\\.").length - 1];
		if (type.equalsIgnoreCase("vhd") || type.equalsIgnoreCase("vhdx")) {
			path = v.getPath();
		} else {
			throw new HyperVIntercloudException(
					"HD type must be either vhd or vhds.");
		}
		String command = "Add-VMHardDiskDrive -ComputerName '" + ip
				+ "' -VMName '" + name + "' -Path '" + path + "'";
		try {
			String output = CmdExecutor.runSinglePsCmd(command);
			if (!output.equals("")) {
				throw new HyperVIntercloudException("Unable to set the HD of "
						+ name + " on " + ip + " to " + path
						+ System.getProperty("line.separator") + "Output: "
						+ output);
			}
			System.out.println("The HD of " + name + " on " + ip
					+ " successfully set...");
		} catch (IOException e) {
			throw new HyperVIntercloudException("Unable to set the HD of "
					+ name + " on " + ip + " to " + path
					+ System.getProperty("line.separator") + "IOException: "
					+ e.getStackTrace().toString());
		}
	}

	private void addNewHD(String ip, String name, VHD v)
			throws HyperVIntercloudException {
		createNewHD(v.getPath(), v.getSize());
		addHD(ip, name, v);
	}

	public void createNewHD(String path, int sizeGB)
			throws HyperVIntercloudException {
		String command = "New-VHD -Path '" + path + "' -SizeBytes " + sizeGB
				+ "GB";
		try {
			String output = CmdExecutor.runSinglePsCmd(command);
			if (output.contains("Failed to create the virtual hard disk")) {
				throw new HyperVIntercloudException("Unable to create a new HD"
						+ System.getProperty("line.separator") + "Output: "
						+ output);
			}
			System.out.println("A new HD of size " + sizeGB
					+ "GB is created...");
		} catch (IOException e) {
			throw new HyperVIntercloudException("Unable to create a new HD"
					+ System.getProperty("line.separator") + "IOException: "
					+ e.getStackTrace().toString());
		}
	}

	private void addDVD(String ip, String name, String iso)
			throws HyperVIntercloudException {
		String command = "Add-VMDvdDrive -ComputerName '" + ip + "' -VMName '"
				+ name + "'";
		if (!iso.equals("")) {
			command += " -Path '" + iso + "'";
		}
		try {
			String output = CmdExecutor.runSinglePsCmd(command);
			if (!output.equals("")) {
				throw new HyperVIntercloudException("Unable to set the DVD of "
						+ name + " on " + ip
						+ System.getProperty("line.separator") + "Output: "
						+ output);
			}
			System.out.println("The DVD of " + name + " on " + ip
					+ " successfully set...");
		} catch (IOException e) {
			throw new HyperVIntercloudException("Unable to set the DVD of "
					+ name + " on " + ip + System.getProperty("line.separator")
					+ "IOException: " + e.getStackTrace().toString());
		}
	}

	public void addNetworkAdapter(String ip, String name)
			throws HyperVIntercloudException {
		String command = "Add-VMNetworkAdapter -ComputerName '" + ip
				+ "' -VMName '" + name + "' –SwitchName '"
				+ HyperVIntercloud.defaultNetworkCard + "'";
		try {
			String output = CmdExecutor.runSinglePsCmd(command);
			if (!output.equals("")) {
				throw new HyperVIntercloudException("Unable to set the NIC of "
						+ name + " on " + ip
						+ System.getProperty("line.separator") + "Output: "
						+ output);
			}
			System.out.println("The NIC of " + name + " on " + ip
					+ " successfully set...");
		} catch (IOException e) {
			throw new HyperVIntercloudException("Unable to set the NIC of "
					+ name + " on " + ip + System.getProperty("line.separator")
					+ "IOException: " + e.getStackTrace().toString());
		}
	}

	public void startVM(String ip, String name)
			throws HyperVIntercloudException {
		System.out.println("Starting " + name + " on " + ip + "...");
		String command = "Start-VM -ComputerName '" + ip + "' -Name '" + name
				+ "'";
		try {
			String output = CmdExecutor.runSinglePsCmd(command);
			if (!output.equals("")) {
				throw new HyperVIntercloudException("Unable to start " + name
						+ " on " + ip + System.getProperty("line.separator")
						+ "Output: " + output);
			}
			System.out.println(name + " from " + ip
					+ " successfully started...");
		} catch (IOException e) {
			throw new HyperVIntercloudException("Unable to start " + name
					+ " on " + ip + System.getProperty("line.separator")
					+ "IOException: " + e.getStackTrace().toString());
		}
	}

	public void shutdownVM(String ip, String name, boolean forced)
			throws HyperVIntercloudException {
		System.out.println("Shutting down " + name + " on " + ip + "...");
		String command;
		if (forced) {
			command = "Stop-VM -ComputerName '" + ip + "' -Name '" + name
					+ "' -TurnOff";
		} else {
			command = "Stop-VM -ComputerName '" + ip + "' -Name '" + name
					+ "' -Force";
		}
		try {
			String output = CmdExecutor.runSinglePsCmd(command);
			if (!output.equals("")) {
				throw new HyperVIntercloudException("Unable to shut down "
						+ name + " on " + ip
						+ System.getProperty("line.separator") + "Output: "
						+ output);
			}
			System.out.println(name + " from " + ip
					+ " successfully shutted down...");
		} catch (IOException e) {
			throw new HyperVIntercloudException("Unable to shut down " + name
					+ " on " + ip + System.getProperty("line.separator")
					+ "IOException: " + e.getStackTrace().toString());
		}
	}

	public void suspendVM(String ip, String name)
			throws HyperVIntercloudException {
		System.out.println("Suspending " + name + " on " + ip + "...");
		String command = "Suspend-VM -ComputerName '" + ip + "' -Name '" + name
				+ "'";

		try {
			String output = CmdExecutor.runSinglePsCmd(command);
			if (!output.equals("")) {
				throw new HyperVIntercloudException("Unable to suspend " + name
						+ " on " + ip + System.getProperty("line.separator")
						+ "Output: " + output);
			}
			System.out.println(name + " from " + ip
					+ " successfully suspended...");
		} catch (IOException e) {
			throw new HyperVIntercloudException("Unable to suspend " + name
					+ " on " + ip + System.getProperty("line.separator")
					+ "IOException: " + e.getStackTrace().toString());
		}
	}

	public void deleteVM(String ip, String name, boolean forced)
			throws HyperVIntercloudException, InterruptedException {
		List<String> vhds = getVHDs(ip, name, true);
		String command = "Remove-VM -ComputerName '" + ip + "' -Name '" + name
				+ "' -Force";
		try {
			String output = CmdExecutor.runSinglePsCmd(command);
			if (output
					.contains("The operation cannot be performed while the virtual machine is in its current state.")
					&& forced) {
				shutdownVM(ip, name, true);
				TimeUnit.SECONDS.sleep(5);
				deleteVM(ip, name, true);
			} else if (!output.equals("")) {
				throw new HyperVIntercloudException("Unable to delete " + name
						+ " on " + ip + System.getProperty("line.separator")
						+ "Output: " + output);
			}
			System.out.println(name + " from " + ip
					+ " successfully deleted...");
			for (String vhd : vhds) {
				File f = new File(vhd);
				f.delete();
			}
		} catch (IOException e) {
			throw new HyperVIntercloudException("Unable to delete " + name
					+ " on " + ip + System.getProperty("line.separator")
					+ "IOException: " + e.getStackTrace().toString());
		}
	}

	public void putVMFiles(String ip, String name, String fromDirectory)
			throws HyperVIntercloudException {
		if (ip.equals("127.0.0.1")) {
			importVM(ip, name, fromDirectory);
		} else {
			// TODO: Milestone 2: UDP transfer
			importVM(ip, name, fromDirectory);
		}
	}

	private void importVM(String ip, String name, String fromDirectory)
			throws HyperVIntercloudException {
		System.out.println("Importing " + name + " to " + ip + " from "
				+ fromDirectory + "...");
		File[] xml = CommonUtil.filterFilesByExt(fromDirectory
				+ "\\Virtual Machines\\", ".XML");
		if (xml == null || xml.length != 1) {
			throw new HyperVIntercloudException("There must be an XML file in "
					+ fromDirectory + "\\Virtual Machines\\.");
		}
		String xmlFile = xml[0].getName();
		String command = "Import-VM -Path '" + fromDirectory
				+ "\\Virtual Machines\\" + xmlFile + "' -Copy";
		try {
			String output = CmdExecutor.runSinglePsCmd(command);
			if (!output.contains("Operating normally")) {
				throw new HyperVIntercloudException("Unable to import " + name
						+ " to " + ip + System.getProperty("line.separator")
						+ "Output: " + output);
			}
			System.out
					.println(name + " successfully imported to " + ip + "...");
		} catch (IOException e) {
			throw new HyperVIntercloudException("Unable to import " + name
					+ " to " + ip + System.getProperty("line.separator")
					+ "IOException: " + e.getStackTrace().toString());
		}
	}

	public List<String> getVMFilePaths(String ip, String name,
			String toDirectory) throws HyperVIntercloudException {
		List<String> filepaths = new ArrayList<String>();
		exportVM(ip, name, null, toDirectory);
		List<String> vmid = getVMID(ip, name);
		List<String> vhds = getVHDs(ip, name, false);
		filepaths.add(toDirectory + "\\Snapshots\\");
		for (String vhd : vhds) {
			filepaths.add(toDirectory + "\\Virtual Hard Disks\\" + vhd);
		}
		filepaths.add(toDirectory + "\\Virtual Machines\\" + vmid + ".XML");
		if (ip.equals("127.0.0.1")) {
			return filepaths;
		}
		// TODO: Milestone 2: UDP transfer
		return filepaths;
	}

	private void exportVM(String ip, String name, String id, String toDirectory)
			throws HyperVIntercloudException {
		System.out.println("Exporting " + name + " to " + ip
				+ "'s local HDD...");
		String command = "Export-VM -ComputerName '" + ip + "' -Name '" + name
				+ "' -Path '" + toDirectory + "'";
		try {
			String output = CmdExecutor.runSinglePsCmd(command);
			if (!output.equals("")) {
				throw new HyperVIntercloudException("Unable to export " + name
						+ " from " + ip + System.getProperty("line.separator")
						+ "Output: " + output);
			}
			System.out.println(name + " from " + ip
					+ " successfully exported to its local HDD...");
		} catch (IOException e) {
			throw new HyperVIntercloudException("Unable to export " + name
					+ " from " + ip + System.getProperty("line.separator")
					+ "IOException: " + e.getStackTrace().toString());
		}
	}

	public List<String> getVMID(String ip, String name)
			throws HyperVIntercloudException {
		List<String> vmid = new ArrayList<String>();
		String command = "(Get-VM -ComputerName '" + ip + "' -Name '" + name
				+ "').id.Guid";
		try {
			String output = CmdExecutor.runSinglePsCmd(command);
			if (output
					.contains("The parameter is not valid. Hyper-V was unable to find a virtual machine")
					|| output
							.contains("The destination host is not available. Specify another host and try the operation again.")) {
				return vmid;
			}
			vmid = new ArrayList<String>(Arrays.asList(output.split(System
					.getProperty("line.separator"))));
			System.out.println("Guid for " + name + " on " + ip + ": " + vmid);
			return vmid;
		} catch (IOException e) {
			throw new HyperVIntercloudException("Unable to export " + name
					+ " from " + ip + System.getProperty("line.separator")
					+ "IOException: " + e.getStackTrace().toString());
		}
	}

	public List<String> getVHDs(String ip, String name, boolean absolutePath)
			throws HyperVIntercloudException {
		List<String> vhds = new ArrayList<String>();
		String command = "(Get-VMHardDiskDrive -ComputerName '" + ip
				+ "' -VMName '" + name + "').path";
		try {
			String output = CmdExecutor.runSinglePsCmd(command);
			if (output
					.contains("The parameter is not valid. Hyper-V was unable to find a virtual machine")
					|| output
							.contains("The destination host is not available. Specify another host and try the operation again.")) {
				throw new HyperVIntercloudException(output);
			}
			List<String> outputList = new ArrayList<String>(
					Arrays.asList(output.split(System
							.getProperty("line.separator"))));
			Iterator<String> i = outputList.iterator();
			while (i.hasNext()) {
				String s = i.next();
				if (s.contains(".vhd")) {
					if (!absolutePath) {
						s = s.split("\\\\")[s.split("\\\\").length - 1];
					}
					vhds.add(s);
				}
				System.out.println("VHDs for " + name + " on " + ip + ": "
						+ vhds);
			}
		} catch (IOException e) {
			throw new HyperVIntercloudException("Unable to export " + name
					+ " from " + ip + System.getProperty("line.separator")
					+ "IOException: " + e.getStackTrace().toString());
		}
		return vhds;
	}

	public List<String> getVMs(String ip) throws HyperVIntercloudException {
		List<String> vm = new ArrayList<String>();
		String command = "(Get-VM -ComputerName '" + ip + "').name";
		try {
			String output = CmdExecutor.runSinglePsCmd(command);
			List<String> outputList = new ArrayList<String>(
					Arrays.asList(output.split(System
							.getProperty("line.separator"))));
			Iterator<String> i = outputList.iterator();
			while (i.hasNext()) {
				String s = i.next();
				vm.add(s.trim());
			}
			System.out.println("List of VMs on " + ip + ": " + vm);
			return vm;
		} catch (IOException e) {
			throw new HyperVIntercloudException(
					"Unable to get a list of VMs from " + ip
							+ System.getProperty("line.separator")
							+ "IOException: " + e.getStackTrace().toString());
		}
	}

	public VM getVM(String ip, String name) throws HyperVIntercloudException {
		VM v = new VM();
		v.setOs("NotSupported");
		String getvm = "(Get-VM -ComputerName '" + ip + "' -VMName '" + name
				+ "')";
		String command1 = getvm + ".MemoryStartup;" + getvm
				+ ".ProcessorCount;" + getvm + ".State";
		String command2 = getvm + ".NetworkAdapters.SwitchName";
		try {
			String output1 = CmdExecutor.runSinglePsCmd(command1);
			String[] o = null;
			try {
				o = output1.split(System.getProperty("line.separator"));
				v.setMemoryB(Long.valueOf(o[0].trim()));
				v.setNumberOfCPUCores(Integer.valueOf(o[1].trim()));
				v.setState(o[2].trim());
			} catch (Exception e) {
				throw new HyperVIntercloudException(
						"Unable to get the details of " + name + " from " + ip
								+ System.getProperty("line.separator")
								+ "Output: " + output1);
			}
			String output2 = CmdExecutor.runSinglePsCmd(command2);
			if (output2.equals("")
					|| output2
							.contains("The term 'Get-VM' is not recognized as the name of a cmdlet")
					|| output2
							.contains("The parameter is not valid. Hyper-V was unable to find a virtual machine")
					|| output2
							.contains("The destination host is not available. Specify another host and try the operation again.")) {
				throw new HyperVIntercloudException(
						"Unable to get the details of " + name + " from " + ip
								+ System.getProperty("line.separator")
								+ "Output: " + output2);
			}
			List<String> networkAdapters = new ArrayList<String>(
					Arrays.asList(output2.split(System
							.getProperty("line.separator"))));
			v.setNetworkAdapters(networkAdapters);
			return v;
		} catch (IOException e) {
			throw new HyperVIntercloudException("Unable to get the details of "
					+ name + " from " + ip
					+ System.getProperty("line.separator") + "IOException: "
					+ e.getStackTrace().toString());
		}
	}
}
