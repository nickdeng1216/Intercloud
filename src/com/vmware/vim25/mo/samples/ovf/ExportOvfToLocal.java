package com.vmware.vim25.mo.samples.ovf;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import com.vmware.vim25.HttpNfcLeaseDeviceUrl;
import com.vmware.vim25.HttpNfcLeaseInfo;
import com.vmware.vim25.HttpNfcLeaseState;
import com.vmware.vim25.OvfCreateDescriptorParams;
import com.vmware.vim25.OvfCreateDescriptorResult;
import com.vmware.vim25.OvfFile;
import com.vmware.vim25.mo.Datacenter;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.HttpNfcLease;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualApp;
import com.vmware.vim25.mo.VirtualMachine;

/**
 * The class exports VMDK(s) and OVF Descriptor for a VM to gateway.
 * 
 * @author Kate.Xie
 */

public class ExportOvfToLocal {
	public static LeaseProgressUpdater leaseProgUpdater;

	/**
	 * 
	 * @param vAppOrVmName
	 *            VMware name
	 * @param URL
	 *            vSphere URL https://192.168.11.101/sdk
	 * @param name
	 *            vSphere login name
	 * @param password
	 *            vSphere login password
	 * @param hostip
	 *            vmware host ip 192.168.11.101
	 * @param targetDir
	 *            target download folder
	 * @throws Exception
	 */
	public List<String> ExportOvfToLocal_execute(String vAppOrVmName,
			String targetDir) throws Exception {
		GetAccountInfo sphere = new GetAccountInfo("vsphere.properties");
		String URL = sphere.getValue("Url");
		String name = sphere.getValue("Username");
		String password = sphere.getValue("Password");
		String hostip = sphere.getValue("Hostip");
		int numVirtualDisks = 0;

		List<String> list_path = new ArrayList<String>();

		// get disk NO

		ServiceInstance si = new ServiceInstance(new URL(URL), name, password,
				true);
		String entityType = "VirtualMachine";

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

						if (vAppOrVmName.equals(vm.getName())) {

							numVirtualDisks = vm.getSummary().config.numVirtualDisks;

						}
					}
				}
			}
		}

		InventoryNavigator iv = new InventoryNavigator(si.getRootFolder());

		HttpNfcLease hnLease = null;

		ManagedEntity me = null;
		if (entityType.equals("VirtualApp")) {
			me = iv.searchManagedEntity("VirtualApp", vAppOrVmName);
			hnLease = ((VirtualApp) me).exportVApp();
		} else {
			me = iv.searchManagedEntity("VirtualMachine", vAppOrVmName);
			hnLease = ((VirtualMachine) me).exportVm();

		}

		// Wait until the HttpNfcLeaseState is ready
		HttpNfcLeaseState hls;
		for (;;) {
			hls = hnLease.getState();
			if (hls == HttpNfcLeaseState.ready) {
				break;
			}
			if (hls == HttpNfcLeaseState.error) {
				si.getServerConnection().logout();

			}
		}

		System.out.println("HttpNfcLeaseState: ready ");
		HttpNfcLeaseInfo httpNfcLeaseInfo = hnLease.getInfo();
		httpNfcLeaseInfo.setLeaseTimeout(300 * 1000 * 1000);
		printHttpNfcLeaseInfo(httpNfcLeaseInfo);

		// Note: the diskCapacityInByte could be many time bigger than
		// the total size of VMDK files downloaded.
		// As a result, the progress calculated could be much less than reality.
		long diskCapacityInByte = (httpNfcLeaseInfo.getTotalDiskCapacityInKB()) * 1024;

		leaseProgUpdater = new LeaseProgressUpdater(hnLease, 5000);
		leaseProgUpdater.start();

		long alredyWrittenBytes = 0;
		HttpNfcLeaseDeviceUrl[] deviceUrls = httpNfcLeaseInfo.getDeviceUrl();
		if (deviceUrls != null) {
			OvfFile[] ovfFiles = new OvfFile[deviceUrls.length];
			System.out.println("Downloading Files:");
			for (int i = 0; i < deviceUrls.length; i++) {
				String deviceId = deviceUrls[i].getKey();
				String deviceUrlStr = deviceUrls[i].getUrl();
				if (!deviceUrlStr.endsWith(".vmdk")) {
					continue;
				}
				String diskFileName = deviceUrlStr.substring(deviceUrlStr
						.lastIndexOf("/") + 1);
				String diskUrlStr = deviceUrlStr.replace("*", hostip);
				String diskLocalPath = targetDir + diskFileName;
				System.out.println("File Name: " + diskFileName);
				System.out.println("VMDK URL: " + diskUrlStr);
				String cookie = si.getServerConnection().getVimService()
						.getWsc().getCookie();
				long lengthOfDiskFile = writeVMDKFile(diskLocalPath,
						diskUrlStr, cookie, alredyWrittenBytes,
						diskCapacityInByte);
				alredyWrittenBytes += lengthOfDiskFile;
				OvfFile ovfFile = new OvfFile();
				ovfFile.setPath(diskFileName);
				ovfFile.setDeviceId(deviceId);
				ovfFile.setSize(lengthOfDiskFile);
				ovfFiles[i] = ovfFile;
			}

			OvfCreateDescriptorParams ovfDescParams = new OvfCreateDescriptorParams();
			ovfDescParams.setOvfFiles(ovfFiles);
			OvfCreateDescriptorResult ovfCreateDescriptorResult = si
					.getOvfManager().createDescriptor(me, ovfDescParams);

			String ovfPath = targetDir + vAppOrVmName + ".ovf";
			FileWriter out = new FileWriter(ovfPath);
			out.write(ovfCreateDescriptorResult.getOvfDescriptor());
			out.close();
			System.out.println("OVF Desriptor Written to file: " + ovfPath);
		}

		System.out.println("Completed Downloading the files");
		leaseProgUpdater.interrupt();
		hnLease.httpNfcLeaseProgress(100);
		hnLease.httpNfcLeaseComplete();

		si.getServerConnection().logout();

		for (int i = 0; i < numVirtualDisks; i++) {
			list_path.add(targetDir + "disk-" + i + ".vmdk");
		}
		list_path.add(targetDir + vAppOrVmName + ".ovf");
		return list_path;
	}

	private static void printHttpNfcLeaseInfo(HttpNfcLeaseInfo info) {
		System.out
				.println("########################  HttpNfcLeaseInfo  ###########################");
		System.out.println("Lease Timeout: " + info.getLeaseTimeout());
		System.out.println("Total Disk capacity: "
				+ info.getTotalDiskCapacityInKB());
		HttpNfcLeaseDeviceUrl[] deviceUrlArr = info.getDeviceUrl();
		if (deviceUrlArr != null) {
			int deviceUrlCount = 1;
			for (HttpNfcLeaseDeviceUrl durl : deviceUrlArr) {
				System.out.println("HttpNfcLeaseDeviceUrl : "
						+ deviceUrlCount++);
				System.out.println("	Device URL Import Key: "
						+ durl.getImportKey());
				System.out.println("	Device URL Key: " + durl.getKey());
				System.out.println("	Device URL : " + durl.getUrl());
				System.out.println("	SSL Thumbprint : "
						+ durl.getSslThumbprint());
			}
		} else {
			System.out.println("No Device URLS Found");
		}
	}

	private static long writeVMDKFile(String localFilePath, String diskUrl,
			String cookie, long bytesAlreadyWritten, long totalBytes)
			throws IOException {
		HttpsURLConnection conn = getHTTPConnection(diskUrl, cookie);
		InputStream in = conn.getInputStream();
		OutputStream out = new FileOutputStream(new File(localFilePath));
		byte[] buf = new byte[102400];
		int len = 0;
		long bytesWritten = 0;
		System.out.println("Writing to: " + localFilePath);
		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
			bytesWritten += len;
			int percent = (int) (((bytesAlreadyWritten + bytesWritten) * 100) / totalBytes);
			leaseProgUpdater.setPercent(percent);
		}
		System.out.println("Finished writing to " + localFilePath + " ("
				+ bytesWritten + ")");
		in.close();
		out.close();
		return bytesWritten;
	}

	private static HttpsURLConnection getHTTPConnection(String urlStr,
			String cookieStr) throws IOException {
		HostnameVerifier hv = new HostnameVerifier() {
			@Override
			public boolean verify(String urlHostName, SSLSession session) {
				return true;
			}
		};
		HttpsURLConnection.setDefaultHostnameVerifier(hv);
		URL url = new URL(urlStr);
		HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

		conn.setDoInput(true);
		conn.setDoOutput(true);
		conn.setAllowUserInteraction(true);
		conn.setRequestProperty("Cookie", cookieStr);
		conn.connect();
		return conn;
	}

}