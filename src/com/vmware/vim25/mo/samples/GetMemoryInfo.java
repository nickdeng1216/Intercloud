package com.vmware.vim25.mo.samples;

import java.net.URL;

import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.samples.ovf.GetAccountInfo;

/**
 * 
 * @author Kate.xie
 *
 */
public class GetMemoryInfo {
	public double GetMemoryInfo_execute() throws Exception {
		// connect to the sphere
		GetAccountInfo sphere = new GetAccountInfo("vsphere.properties");
		String URL = sphere.getValue("Url");
		String name = sphere.getValue("Username");
		String password = sphere.getValue("Password");
		String hostip = sphere.getValue("Hostip");
		boolean flag = false;

		// find host system

		ServiceInstance si = new ServiceInstance(new URL(URL), name, password,
				true);
		Folder rootFolder = si.getRootFolder();

		InventoryNavigator inventoryNavigator = new InventoryNavigator(
				rootFolder);

		ManagedEntity[] hostEntities = inventoryNavigator
				.searchManagedEntities("HostSystem");

		HostSystem hostSystem = (HostSystem) hostEntities[0];

		// used
		Integer usedMemoryUsage = hostSystem.getSummary().quickStats.overallMemoryUsage;
		// all size
		double memorySize = (double) hostSystem.getHardware().memorySize / 1024 / 1024; //
		// unused
		double unused = memorySize - usedMemoryUsage;
		si.getServerConnection().logout();
		return unused;
	}

	public static void main(String[] args) throws Exception {
		GetMemoryInfo a = new GetMemoryInfo();
		System.out.print(a.GetMemoryInfo_execute());

	}
}
