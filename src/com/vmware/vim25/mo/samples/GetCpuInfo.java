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
public class GetCpuInfo {
	public double GetCpuInfo_execute() throws Exception {
		// connect to the sphere
		GetAccountInfo sphere = new GetAccountInfo("vsphere.properties");
		String URL = sphere.getValue("Url");
		String name = sphere.getValue("Username");
		String password = sphere.getValue("Password");

		// find host system
		ServiceInstance si = new ServiceInstance(new URL(URL), name, password,
				true);
		Folder rootFolder = si.getRootFolder();
		InventoryNavigator inventoryNavigator = new InventoryNavigator(
				rootFolder);
		ManagedEntity[] hostEntities = inventoryNavigator
				.searchManagedEntities("HostSystem");
		HostSystem hostSystem = (HostSystem) hostEntities[0];

		// all cpu
		Integer overallMemoryUsage = hostSystem.getSummary().hardware.cpuMhz;
		int corenumber = hostSystem.getSummary().hardware.numCpuCores;
		double allcpu = overallMemoryUsage * corenumber;

		// used cpu
		int usedcpu = hostSystem.getSummary().quickStats.overallCpuUsage;

		// unused cpu
		double unused = allcpu - usedcpu;
		si.getServerConnection().logout();
		return unused;
	}

	public static void main(String[] args) throws Exception {
		GetCpuInfo a = new GetCpuInfo();
		System.out.print(a.GetCpuInfo_execute());

	}
}
