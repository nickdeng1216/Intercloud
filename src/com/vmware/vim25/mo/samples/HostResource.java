package com.vmware.vim25.mo.samples;

import java.net.URL;

import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ResourcePool;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.samples.ovf.GetAccountInfo;

public class HostResource {
	private String url;
	private String name;
	private String password;
	private String hostip;

	public HostResource(String propertiesFilePath) throws Exception {
		// connect to the sphere
		GetAccountInfo sphere = new GetAccountInfo(propertiesFilePath);
		this.url = sphere.getValue("Url");
		this.name = sphere.getValue("Username");
		this.password = sphere.getValue("Password");
		this.hostip = sphere.getValue("Hostip");
	}

	public long getUnreservedCPU() throws Exception {
		// find RESCOURCES pool
		ServiceInstance si = new ServiceInstance(new URL(url), name, password,
				true);
		Folder rootFolder = si.getRootFolder();

		InventoryNavigator inventoryNavigator = new InventoryNavigator(
				rootFolder);
		ManagedEntity[] rps = new InventoryNavigator(rootFolder)
				.searchManagedEntities("ResourcePool");
		ResourcePool rp = (ResourcePool) rps[0];

		// Unit in MHz
		long unreservedCUP = rp.getRuntime().cpu.unreservedForVm;
		long reservedCUP = rp.getRuntime().cpu.reservationUsed;

		si.getServerConnection().logout();
		return unreservedCUP;
	}

	public long getMaxCPU() throws Exception {
		ServiceInstance si = new ServiceInstance(new URL(url), name, password,
				true);
		Folder rootFolder = si.getRootFolder();
		InventoryNavigator inventoryNavigator = new InventoryNavigator(
				rootFolder);
		ManagedEntity[] hostEntities = inventoryNavigator
				.searchManagedEntities("HostSystem");
		HostSystem hostSystem = (HostSystem) hostEntities[0];

		// Unit in MHz
		long maxCPUPerVM = hostSystem.getSystemResources().config.cpuAllocation.limit;

		si.getServerConnection().logout();
		return maxCPUPerVM;
	}

	public long getUnreservedMemory() throws Exception {
		// find RESCOURCES pool
		ServiceInstance si = new ServiceInstance(new URL(url), name, password,
				true);
		Folder rootFolder = si.getRootFolder();
		InventoryNavigator inventoryNavigator = new InventoryNavigator(
				rootFolder);
		ManagedEntity[] rps = new InventoryNavigator(rootFolder)
				.searchManagedEntities("ResourcePool");
		ResourcePool rp = (ResourcePool) rps[0];

		// Unit in MB
		long unreservedMemory = rp.getRuntime().memory.unreservedForVm;
		long reservedMemory = rp.getRuntime().memory.reservationUsed;

		si.getServerConnection().logout();
		return unreservedMemory;
	}

	public long getMaxMemory() throws Exception {
		ServiceInstance si = new ServiceInstance(new URL(url), name, password,
				true);
		Folder rootFolder = si.getRootFolder();
		InventoryNavigator inventoryNavigator = new InventoryNavigator(
				rootFolder);
		ManagedEntity[] hostEntities = inventoryNavigator
				.searchManagedEntities("HostSystem");
		HostSystem hostSystem = (HostSystem) hostEntities[0];

		// Unit in MB
		long maxMemoryPerVM = hostSystem.getSystemResources().config.memoryAllocation.limit;

		si.getServerConnection().logout();
		return maxMemoryPerVM;
	}

	public long getDiskFreeSpace() throws Exception {
		// find host system
		ServiceInstance si = new ServiceInstance(new URL(url), name, password,
				true);
		Folder rootFolder = si.getRootFolder();
		InventoryNavigator inventoryNavigator = new InventoryNavigator(
				rootFolder);
		ManagedEntity[] hostEntities = inventoryNavigator
				.searchManagedEntities("HostSystem");
		HostSystem hostSystem = (HostSystem) hostEntities[0];

		long freeDiskSpace = hostSystem.getDatastores()[0].getInfo()
				.getFreeSpace();
		// Unit in GB
		freeDiskSpace = freeDiskSpace / 1024 / 1024 / 1024;

		si.getServerConnection().logout();
		return freeDiskSpace;
	}

	public long getMaxFileSize() throws Exception {
		// find host system
		ServiceInstance si = new ServiceInstance(new URL(url), name, password,
				true);
		Folder rootFolder = si.getRootFolder();
		InventoryNavigator inventoryNavigator = new InventoryNavigator(
				rootFolder);
		ManagedEntity[] hostEntities = inventoryNavigator
				.searchManagedEntities("HostSystem");
		HostSystem hostSystem = (HostSystem) hostEntities[0];

		long maxFileSize = hostSystem.getDatastores()[0].getInfo()
				.getMaxFileSize();
		// Unit in GB
		maxFileSize = maxFileSize / 1024 / 1024 / 1024;

		si.getServerConnection().logout();
		return maxFileSize;
	}

	public static void main(String[] arg) throws Exception {
		HostResource hr = new HostResource("vsphere.properties");
		System.out.println(hr.getMaxFileSize());

	}
}
