package com.vmware.vim25.mo.samples.ovf;

import java.net.URL;

import com.vmware.vim25.mo.Datacenter;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualMachine;

/**
 * 
 * @author Kate
 *
 */
public class GetVMList {
	public String GetVMList() throws Exception {
		// connect to the sphere and find the vm
		GetAccountInfo sphere = new GetAccountInfo("vsphere.properties");
		String URL = sphere.getValue("Url");
		String name = sphere.getValue("Username");
		String password = sphere.getValue("Password");
		// String hostip = sphere.getValue("Hostip");

		ServiceInstance si = new ServiceInstance(new URL(URL), name, password,
				true);
		Folder rootFolder = si.getRootFolder();

		ManagedEntity[] mes = rootFolder.getChildEntity();

		String list = "";
		for (int i = 0; i < mes.length; i++) {

			if (mes[i] instanceof Datacenter) {
				Datacenter dc = (Datacenter) mes[i];
				Folder vmFolder = dc.getVmFolder();
				ManagedEntity[] vms = vmFolder.getChildEntity();

				for (int j = 0; j < vms.length; j++) {
					if (vms[j] instanceof VirtualMachine) {
						VirtualMachine vm = (VirtualMachine) vms[j];
						list += vm.getName() + " ";

					}
				}
			}
		}
		si.getServerConnection().logout();

		return list;

	}

	public static void main(String[] args) throws Exception {
		GetVMList a = new GetVMList();

		String a1 = a.GetVMList();
		System.out.print(a1);

	}
}
