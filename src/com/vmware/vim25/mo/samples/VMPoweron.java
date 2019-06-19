package com.vmware.vim25.mo.samples;

import java.net.URL;

import com.vmware.vim25.VirtualMachinePowerState;
import com.vmware.vim25.VirtualMachineRuntimeInfo;
import com.vmware.vim25.VirtualMachineSummary;
import com.vmware.vim25.mo.Datacenter;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.VirtualMachine;
import com.vmware.vim25.mo.samples.ovf.GetAccountInfo;

/**
 * The class powers on VM in VMware.
 * 
 * @author Kate.xie
 *
 */
public class VMPoweron {
	public String VMPoweron_execute(String vAppOrVmName) throws Exception {
		// connect to the sphere and find the vm
		GetAccountInfo sphere = new GetAccountInfo("vsphere.properties");
		String URL = sphere.getValue("Url");
		String name = sphere.getValue("Username");
		String password = sphere.getValue("Password");
		String hostip = sphere.getValue("Hostip");
		boolean flag = false;
		String reason = "200";

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

							if (vmri.getPowerState() != VirtualMachinePowerState.poweredOn) {
								Task task = vm.powerOnVM_Task(null);
								task.waitForMe();
								flag = true;
								System.out.println("vm:" + vm.getName()
										+ " powered on.");
							}

						}
					}
				}
			}
		}
		si.getServerConnection().logout();
		if (!flag) {
			throw new Exception("Operation failed.");
		}
		return reason;
	}

	public static void main(String[] args) throws Exception {
		VMPoweron a = new VMPoweron();
		a.VMPoweron_execute("docker_14");

	}

}
