package com.vmware.vim25.mo.samples;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.vmware.vim25.Description;
import com.vmware.vim25.VirtualDeviceConfigSpec;
import com.vmware.vim25.VirtualDeviceConfigSpecFileOperation;
import com.vmware.vim25.VirtualDeviceConfigSpecOperation;
import com.vmware.vim25.VirtualDisk;
import com.vmware.vim25.VirtualDiskFlatVer2BackingInfo;
import com.vmware.vim25.VirtualEthernetCard;
import com.vmware.vim25.VirtualEthernetCardNetworkBackingInfo;
import com.vmware.vim25.VirtualLsiLogicController;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.VirtualMachineFileInfo;
import com.vmware.vim25.VirtualPCNet32;
import com.vmware.vim25.VirtualSCSISharing;
import com.vmware.vim25.mo.Datacenter;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ResourcePool;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.samples.ovf.GetAccountInfo;

public class CreateVM_api {
	public String CreateVM_execute(String vmname, String memorysize,
			String disksize, int cpucount) throws Exception {
		GetAccountInfo sphere = new GetAccountInfo("vsphere.properties");
		String dcName = "ha-datacenter";

		String netName = "VM Network";
		String nicName = "Network Adapter 1";
		String guestosid = "sles10Guest";

		String diskmode = sphere.getValue("Diskmode");
		String URL = sphere.getValue("Url");
		String name = sphere.getValue("Username");
		String password = sphere.getValue("Password");
		// String hostip = sphere.getValue("Hostip");
		String datastore = sphere.getValue("Datastore");

		ServiceInstance si = new ServiceInstance(new URL(URL), name, password,
				true);

		Folder rootFolder = si.getRootFolder();

		Datacenter dc = (Datacenter) new InventoryNavigator(rootFolder)
				.searchManagedEntity("Datacenter", dcName);
		ResourcePool rp = (ResourcePool) new InventoryNavigator(dc)
				.searchManagedEntities("ResourcePool")[0];

		Folder vmFolder = dc.getVmFolder();

		List<String> vms = new ArrayList<String>();

		VMList list = new VMList();
		vms = list.VMList_execute();

		if (vms.contains(vmname))
			return "404";

		// create vm config spec
		VirtualMachineConfigSpec vmSpec = new VirtualMachineConfigSpec();
		vmSpec.setName(vmname);
		vmSpec.setAnnotation("VirtualMachine Annotation");
		vmSpec.setMemoryMB(Long.parseLong(memorysize) * 1024);
		vmSpec.setNumCPUs(cpucount);
		vmSpec.setGuestId(guestosid);

		// create virtual devices
		int cKey = 1000;
		VirtualDeviceConfigSpec scsiSpec = createScsiSpec(cKey);
		VirtualDeviceConfigSpec diskSpec = createDiskSpec(datastore, cKey,
				Long.parseLong(disksize) * 1024 * 1024, diskmode);
		VirtualDeviceConfigSpec nicSpec = createNicSpec(netName, nicName);

		vmSpec.setDeviceChange(new VirtualDeviceConfigSpec[] { scsiSpec,
				diskSpec, nicSpec });

		// create vm file info for the vmx file
		VirtualMachineFileInfo vmfi = new VirtualMachineFileInfo();
		vmfi.setVmPathName("[" + datastore + "]");
		vmSpec.setFiles(vmfi);

		// call the createVM_Task method on the vm folder
		Task task = vmFolder.createVM_Task(vmSpec, rp, null);
		String result = task.waitForMe();
		String value = "404";
		if (result == Task.SUCCESS) {
			System.out.println("VM Created Sucessfully");
			value = "200";
		} else {
			System.out.println("VM could not be created. ");
			value = "404";
		}
		return value;
	}

	static VirtualDeviceConfigSpec createScsiSpec(int cKey) {
		VirtualDeviceConfigSpec scsiSpec = new VirtualDeviceConfigSpec();
		scsiSpec.setOperation(VirtualDeviceConfigSpecOperation.add);
		VirtualLsiLogicController scsiCtrl = new VirtualLsiLogicController();
		scsiCtrl.setKey(cKey);
		scsiCtrl.setBusNumber(0);
		scsiCtrl.setSharedBus(VirtualSCSISharing.noSharing);
		scsiSpec.setDevice(scsiCtrl);
		return scsiSpec;
	}

	static VirtualDeviceConfigSpec createDiskSpec(String dsName, int cKey,
			long diskSizeKB, String diskMode) {
		VirtualDeviceConfigSpec diskSpec = new VirtualDeviceConfigSpec();
		diskSpec.setOperation(VirtualDeviceConfigSpecOperation.add);
		diskSpec.setFileOperation(VirtualDeviceConfigSpecFileOperation.create);

		VirtualDisk vd = new VirtualDisk();
		vd.setCapacityInKB(diskSizeKB);
		diskSpec.setDevice(vd);
		vd.setKey(0);
		vd.setUnitNumber(0);
		vd.setControllerKey(cKey);

		VirtualDiskFlatVer2BackingInfo diskfileBacking = new VirtualDiskFlatVer2BackingInfo();
		String fileName = "[" + dsName + "]";
		diskfileBacking.setFileName(fileName);
		diskfileBacking.setDiskMode(diskMode);
		diskfileBacking.setThinProvisioned(true);
		vd.setBacking(diskfileBacking);
		return diskSpec;
	}

	static VirtualDeviceConfigSpec createNicSpec(String netName, String nicName)
			throws Exception {
		VirtualDeviceConfigSpec nicSpec = new VirtualDeviceConfigSpec();
		nicSpec.setOperation(VirtualDeviceConfigSpecOperation.add);

		VirtualEthernetCard nic = new VirtualPCNet32();
		VirtualEthernetCardNetworkBackingInfo nicBacking = new VirtualEthernetCardNetworkBackingInfo();
		nicBacking.setDeviceName(netName);

		Description info = new Description();
		info.setLabel(nicName);
		info.setSummary(netName);
		nic.setDeviceInfo(info);

		// type: "generated", "manual", "assigned" by VC
		nic.setAddressType("generated");
		nic.setBacking(nicBacking);
		nic.setKey(0);

		nicSpec.setDevice(nic);
		return nicSpec;

	}

	public static void main(String[] args) throws Exception {
		// CreateVM a = new CreateVM();
		// a.CreateVM_execute("test14", "2", "3", 4);
	}
}
