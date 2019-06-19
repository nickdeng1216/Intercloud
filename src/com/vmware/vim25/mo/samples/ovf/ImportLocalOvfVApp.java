/*================================================================================
Copyright (c) 2008 VMware, Inc. All Rights Reserved.

Redistribution and use in source and binary forms, with or without modification, 
are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, 
this list of conditions and the following disclaimer.

 * Redistributions in binary form must reproduce the above copyright notice, 
this list of conditions and the following disclaimer in the documentation 
and/or other materials provided with the distribution.

 * Neither the name of VMware, Inc. nor the names of its contributors may be used
to endorse or promote products derived from this software without specific prior 
written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
IN NO EVENT SHALL VMWARE, INC. OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, 
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR 
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
POSSIBILITY OF SUCH DAMAGE.
================================================================================*/
package com.vmware.vim25.mo.samples.ovf;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import com.vmware.vim25.HttpNfcLeaseDeviceUrl;
import com.vmware.vim25.HttpNfcLeaseInfo;
import com.vmware.vim25.HttpNfcLeaseState;
import com.vmware.vim25.OvfCreateImportSpecParams;
import com.vmware.vim25.OvfCreateImportSpecResult;
import com.vmware.vim25.OvfFileItem;
import com.vmware.vim25.OvfNetworkMapping;
import com.vmware.vim25.mo.ComputeResource;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.HttpNfcLease;
import com.vmware.vim25.mo.ResourcePool;
import com.vmware.vim25.mo.ServiceInstance;

/**
 * The class imports VM to sPhere.
 * 
 * @author Kate.Xie
 *
 */
public class ImportLocalOvfVApp {
	private static final int CHUCK_LEN = 64 * 1024;

	public static LeaseProgressUpdater leaseUpdater;

	/**
	 * 
	 * @param vAppOrVmName
	 * @param newVmName
	 *            new VMwarename in Vsphere
	 * @param URL
	 *            vSphere URL https://192.168.11.101/sdk
	 * @param name
	 *            vSphere login name
	 * @param password
	 *            vSphere login name password
	 * @param hostip
	 *            vmware hostip 192.168.11.101
	 * @param ovfLocal
	 *            uploaded folder path
	 * @throws Exception
	 */
	public String ImportLocalOvfVApp_execute(String newVmName, String ovfLocal)
			throws Exception {
		GetAccountInfo sphere = new GetAccountInfo("vsphere.properties");
		String URL = sphere.getValue("Url");
		String name = sphere.getValue("Username");
		String password = sphere.getValue("Password");
		String hostip = sphere.getValue("Hostip");

		ServiceInstance si = new ServiceInstance(new URL(URL), name, password,
				true);

		HostSystem host = (HostSystem) si.getSearchIndex().findByIp(null,
				hostip, false);

		System.out.println("Host Name : " + host.getName());
		System.out.println("Network : " + host.getNetworks()[0].getName());
		System.out.println("Datastore : " + host.getDatastores()[0].getName());

		Folder vmFolder = (Folder) host.getVms()[0].getParent();

		OvfCreateImportSpecParams importSpecParams = new OvfCreateImportSpecParams();
		importSpecParams.setHostSystem(host.getMOR());
		importSpecParams.setLocale("US");
		importSpecParams.setEntityName(newVmName);
		importSpecParams.setDeploymentOption("");
		OvfNetworkMapping networkMapping = new OvfNetworkMapping();
		networkMapping.setName("Network 1");
		networkMapping.setNetwork(host.getNetworks()[0].getMOR()); // network);
		importSpecParams
				.setNetworkMapping(new OvfNetworkMapping[] { networkMapping });
		importSpecParams.setPropertyMapping(null);

		String ovfDescriptor = readOvfContent(ovfLocal);
		if (ovfDescriptor == null) {
			si.getServerConnection().logout();
			return "error";

		}

		System.out.println("ovfDesc:" + ovfDescriptor);

		ResourcePool rp = ((ComputeResource) host.getParent())
				.getResourcePool();

		OvfCreateImportSpecResult ovfImportResult = si.getOvfManager()
				.createImportSpec(ovfDescriptor, rp, host.getDatastores()[0],
						importSpecParams);

		if (ovfImportResult == null) {
			si.getServerConnection().logout();
			return "error";

		}

		long totalBytes = addTotalBytes(ovfImportResult);
		System.out.println("Total bytes: " + totalBytes);

		HttpNfcLease httpNfcLease = null;

		httpNfcLease = rp.importVApp(ovfImportResult.getImportSpec(), vmFolder,
				host);

		// Wait until the HttpNfcLeaseState is ready
		HttpNfcLeaseState hls;
		for (;;) {
			hls = httpNfcLease.getState();
			if (hls == HttpNfcLeaseState.ready
					|| hls == HttpNfcLeaseState.error) {
				break;
			}
		}

		if (hls.equals(HttpNfcLeaseState.ready)) {
			System.out.println("HttpNfcLeaseState: ready ");
			HttpNfcLeaseInfo httpNfcLeaseInfo = httpNfcLease.getInfo();
			printHttpNfcLeaseInfo(httpNfcLeaseInfo);

			leaseUpdater = new LeaseProgressUpdater(httpNfcLease, 5000);
			leaseUpdater.start();

			HttpNfcLeaseDeviceUrl[] deviceUrls = httpNfcLeaseInfo
					.getDeviceUrl();

			long bytesAlreadyWritten = 0;
			for (HttpNfcLeaseDeviceUrl deviceUrl : deviceUrls) {
				String deviceKey = deviceUrl.getImportKey();
				for (OvfFileItem ovfFileItem : ovfImportResult.getFileItem()) {
					if (deviceKey.equals(ovfFileItem.getDeviceId())) {
						System.out
								.println("Import key==OvfFileItem device id: "
										+ deviceKey);
						String absoluteFile = new File(ovfLocal).getParent()
								+ File.separator + ovfFileItem.getPath();
						String urlToPost = deviceUrl.getUrl().replace("*",
								hostip);
						uploadVmdkFile(ovfFileItem.isCreate(), absoluteFile,
								urlToPost, bytesAlreadyWritten, totalBytes);
						bytesAlreadyWritten += ovfFileItem.getSize();
						System.out.println("Completed uploading the VMDK file:"
								+ absoluteFile);
					}
				}
			}

			leaseUpdater.interrupt();
			httpNfcLease.httpNfcLeaseProgress(100);
			httpNfcLease.httpNfcLeaseComplete();
		}
		si.getServerConnection().logout();

		return "downloaded";
	}

	public static long addTotalBytes(OvfCreateImportSpecResult ovfImportResult) {
		OvfFileItem[] fileItemArr = ovfImportResult.getFileItem();

		long totalBytes = 0;
		if (fileItemArr != null) {
			for (OvfFileItem fi : fileItemArr) {
				printOvfFileItem(fi);
				totalBytes += fi.getSize();
			}
		}
		return totalBytes;
	}

	private static void uploadVmdkFile(boolean put, String diskFilePath,
			String urlStr, long bytesAlreadyWritten, long totalBytes)
			throws IOException {
		if (!diskFilePath.endsWith(".vmdk")) {
			return;
		}
		HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
			@Override
			public boolean verify(String urlHostName, SSLSession session) {
				return true;
			}
		});

		HttpsURLConnection conn = (HttpsURLConnection) new URL(urlStr)
				.openConnection();
		conn.setDoOutput(true);
		conn.setUseCaches(false);
		conn.setChunkedStreamingMode(CHUCK_LEN);
		conn.setRequestMethod(put ? "PUT" : "POST"); // Use a post method to
														// write the file.
		conn.setRequestProperty("Connection", "Keep-Alive");
		conn.setRequestProperty("Content-Type",
				"application/x-vnd.vmware-streamVmdk");
		conn.setRequestProperty("Content-Length",
				Long.toString(new File(diskFilePath).length()));

		BufferedOutputStream bos = new BufferedOutputStream(
				conn.getOutputStream());

		BufferedInputStream diskis = new BufferedInputStream(
				new FileInputStream(diskFilePath));
		int bytesAvailable = diskis.available();
		int bufferSize = Math.min(bytesAvailable, CHUCK_LEN);
		byte[] buffer = new byte[bufferSize];

		long totalBytesWritten = 0;
		System.out.println("Writing to : " + urlStr);
		while (true) {
			int bytesRead = diskis.read(buffer, 0, bufferSize);
			if (bytesRead == -1) {
				System.out.println("Finished writing to " + urlStr + " ("
						+ totalBytesWritten + ")");
				break;
			}

			totalBytesWritten += bytesRead;
			bos.write(buffer, 0, bufferSize);
			bos.flush();

			int progressPercent = (int) (((bytesAlreadyWritten + totalBytesWritten) * 100) / totalBytes);
			leaseUpdater.setPercent(progressPercent);
		}

		diskis.close();
		bos.flush();
		bos.close();
		conn.disconnect();
	}

	public static String readOvfContent(String ovfFilePath) throws IOException {
		StringBuffer strContent = new StringBuffer();
		BufferedReader in = new BufferedReader(new InputStreamReader(
				new FileInputStream(ovfFilePath)));
		String lineStr;
		while ((lineStr = in.readLine()) != null) {
			strContent.append(lineStr);
		}
		in.close();
		return strContent.toString();
	}

	private static void printHttpNfcLeaseInfo(HttpNfcLeaseInfo info) {
		System.out
				.println("================ HttpNfcLeaseInfo ================");
		HttpNfcLeaseDeviceUrl[] deviceUrlArr = info.getDeviceUrl();
		for (HttpNfcLeaseDeviceUrl durl : deviceUrlArr) {
			System.out.println("Device URL Import Key: " + durl.getImportKey());
			System.out.println("Device URL Key: " + durl.getKey());
			System.out.println("Device URL : " + durl.getUrl());
			System.out.println("Updated device URL: " + durl.getUrl());
		}
		System.out.println("Lease Timeout: " + info.getLeaseTimeout());
		System.out.println("Total Disk capacity: "
				+ info.getTotalDiskCapacityInKB());
		System.out
				.println("==================================================");
	}

	private static void printOvfFileItem(OvfFileItem fi) {
		System.out.println("================ OvfFileItem ================");
		System.out.println("chunkSize: " + fi.getChunkSize());
		System.out.println("create: " + fi.isCreate());
		System.out.println("deviceId: " + fi.getDeviceId());
		System.out.println("path: " + fi.getPath());
		System.out.println("size: " + fi.getSize());
		System.out.println("==============================================");
	}

	public static void main(String[] args) throws Exception {
		ImportLocalOvfVApp a = new ImportLocalOvfVApp();
		a.ImportLocalOvfVApp_execute(
				String.valueOf(System.currentTimeMillis()),
				"C:/share/workspace/Intercloud/retrieve/Windows98/Windows98.ovf");

	}
}