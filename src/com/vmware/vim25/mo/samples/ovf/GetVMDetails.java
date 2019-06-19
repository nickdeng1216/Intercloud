package com.vmware.vim25.mo.samples.ovf;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import com.vmware.vim25.VirtualMachineRuntimeInfo;
import com.vmware.vim25.VirtualMachineSummary;
import com.vmware.vim25.mo.Datacenter;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualMachine;

/**
 * The class gets VM's info in VMware.
 * 
 * @author Kate.Xie
 *
 */
public class GetVMDetails {
	public Map GetVMDetails_execute(String vAppOrVmName) throws Exception {
		GetAccountInfo sphere = new GetAccountInfo("vsphere.properties");
		String URL = sphere.getValue("Url");
		String name = sphere.getValue("Username");
		String password = sphere.getValue("Password");
		String hostip = sphere.getValue("Hostip");

		int memorysize;
		long hardsize;
		int cpu;
		String network;
		String guest;
		String cpu_string;
		String memorysize_string;
		Integer numEthernetCards;
		String numEthernetCards_string;
		String hardsize_string;
		HashMap<String, String> details = new HashMap<String, String>();

		ServiceInstance si = new ServiceInstance(new URL(URL), name, password,
				true);
		Folder rootFolder = si.getRootFolder();

		ManagedEntity[] mes = rootFolder.getChildEntity();

		for (int i = 0; i < mes.length; i++) {
			if (mes[i] instanceof Datacenter) {
				Datacenter dc = (Datacenter) mes[i];
				Folder vmFolder = dc.getVmFolder();
				ManagedEntity[] vms = vmFolder.getChildEntity();

				for (int j = 0; j < vms.length; j++) {
					if (vms[j] instanceof VirtualMachine) {
						VirtualMachine vm = (VirtualMachine) vms[j];

						VirtualMachineSummary summary = (vm.getSummary());

						VirtualMachineRuntimeInfo vmri = vm.getRuntime();
						if (vAppOrVmName.equals(vm.getName())) {

							guest = vm.getConfig().getGuestFullName();
							memorysize = vm.getConfig().getHardware().memoryMB;
							numEthernetCards = vm.getSummary().config.numEthernetCards;
							hardsize = vm.getSummary().storage.committed / 1024 / 1024;

							cpu = vm.getConfig().hardware.numCPU;
							cpu_string = Integer.toString(cpu);
							memorysize_string = Integer.toString(memorysize);
							numEthernetCards_string = Integer
									.toString(numEthernetCards);
							hardsize_string = String.valueOf(hardsize);

							details.put("CPU", cpu_string);
							details.put("MemorySize", memorysize_string);
							details.put("Guest", guest);
							details.put("NumEthernetCards",
									numEthernetCards_string);
							// details.put("HardDiskSize", hardsize_string);

						}
					}
				}
			}
		}

		return details;
	}

	public static void main(String args[]) throws Exception {
		GetVMDetails a = new GetVMDetails();
		System.out.print(a.GetVMDetails_execute("DemoWeb").get("Guest"));

	}

}
