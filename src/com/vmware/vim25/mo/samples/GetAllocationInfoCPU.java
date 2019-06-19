package com.vmware.vim25.mo.samples;

import java.net.URL;

import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ResourcePool;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.samples.ovf.GetAccountInfo;

public class GetAllocationInfoCPU {
	public double GetAllocationInfoCPU_execute() throws Exception {
		// connect to the sphere
		GetAccountInfo sphere = new GetAccountInfo("vsphere.properties");
		String URL = sphere.getValue("Url");
		String name = sphere.getValue("Username");
		String password = sphere.getValue("Password");
		String hostip = sphere.getValue("Hostip");
		boolean flag = false;

		// find RESCOURCES pool

		ServiceInstance si = new ServiceInstance(new URL(URL), name, password,
				true);
		Folder rootFolder = si.getRootFolder();

		InventoryNavigator inventoryNavigator = new InventoryNavigator(
				rootFolder);
		ManagedEntity[] rps = new InventoryNavigator(rootFolder)
				.searchManagedEntities("ResourcePool");

		for (int i = 0; i < rps.length; i++) {
			ResourcePool rp = (ResourcePool) rps[i];
			// available
			System.out.println(rp.getRuntime().cpu.unreservedForVm);
			// reserved
			System.out.println(rp.getRuntime().cpu.reservationUsed);

		}
		return 0;
	}

	public static void main(String[] args) throws Exception {
		GetAllocationInfoCPU a = new GetAllocationInfoCPU();
		System.out.print(a.GetAllocationInfoCPU_execute());

	}
}
