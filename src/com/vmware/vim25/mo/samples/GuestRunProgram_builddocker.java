package com.vmware.vim25.mo.samples;

import java.net.URL;
import java.util.Calendar;

import com.vmware.vim25.GuestProcessInfo;
import com.vmware.vim25.GuestProgramSpec;
import com.vmware.vim25.NamePasswordAuthentication;
import com.vmware.vim25.mo.Datacenter;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.GuestOperationsManager;
import com.vmware.vim25.mo.GuestProcessManager;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualMachine;
import com.vmware.vim25.mo.samples.ovf.GetAccountInfo;

/**
 * The class installs docker remotely.
 * 
 * @author Kate.xie
 *
 */
public class GuestRunProgram_builddocker {

	public String GuestRunProgram_builddocker_execute(String vmname)
			throws Exception {

		GetAccountInfo sphere = new GetAccountInfo("vsphere.properties");
		String URL = sphere.getValue("Url");
		String name = sphere.getValue("Username");
		String password = sphere.getValue("Password");
		String hostip = sphere.getValue("Hostip");

		ServiceInstance si = new ServiceInstance(new URL(URL), name, password,
				true);
		Folder rootFolder = si.getRootFolder();

		ManagedEntity[] mes = new InventoryNavigator(rootFolder)
				.searchManagedEntities("VirtualMachine");
		if (mes == null || mes.length == 0) {
			return "404";
		}

		GuestOperationsManager gom = si.getGuestOperationsManager();

		ManagedEntity[] mes1 = rootFolder.getChildEntity();

		for (int i = 0; i < mes1.length; i++) {
			if (mes1[i] instanceof Datacenter) {
				Datacenter dc = (Datacenter) mes1[i];
				Folder vmFolder = dc.getVmFolder();
				ManagedEntity[] vms = vmFolder.getChildEntity();

				for (int j = 0; j < vms.length; j++) {
					if (vms[j] instanceof VirtualMachine) {
						VirtualMachine vm = (VirtualMachine) vms[j];
						System.out.print(vm.getName());

						if (vmname.equals(vm.getName())) {

							if (!"guestToolsRunning"
									.equals(vm.getGuest().toolsRunningStatus)) {
								System.out
										.println("The VMware Tools is not running in the Guest OS on VM: "
												+ vm.getName());
								System.out.println("Exiting...");

							}

							NamePasswordAuthentication npa = new NamePasswordAuthentication();
							getAccount account = new getAccount(vmname);

							npa.username = account.getValue("Username");
							npa.password = account.getValue("Password");

							GuestProgramSpec spec1 = new GuestProgramSpec();
							spec1.programPath = "/bin/echo";
							spec1.arguments = " \"echo '12345678' | sudo -S apt-get update \">builddocker_1.sh";

							GuestProgramSpec spec2 = new GuestProgramSpec();
							spec2.programPath = "/bin/chmod";
							spec2.arguments = "777 builddocker_1.sh";

							GuestProgramSpec spec3 = new GuestProgramSpec();
							spec3.programPath = "/bin/bash";
							spec3.arguments = "builddocker_1.sh ";

							GuestProgramSpec spec4 = new GuestProgramSpec();
							spec4.programPath = "/bin/echo";
							spec4.arguments = " \"echo '12345678' | sudo -S apt-get install docker-engine\"> builddocker_2.sh";

							GuestProgramSpec spec5 = new GuestProgramSpec();
							spec5.programPath = "/bin/chmod";
							spec5.arguments = "777 builddocker_2.sh ";

							GuestProgramSpec spec6 = new GuestProgramSpec();
							spec6.programPath = "/bin/bash";
							spec6.arguments = "builddocker_2.sh ";

							GuestProcessManager gpm = gom.getProcessManager(vm);

							long pid1 = gpm.startProgramInGuest(npa, spec1);
							long pid2 = gpm.startProgramInGuest(npa, spec2);
							long pid3 = gpm.startProgramInGuest(npa, spec3);

							long[] pids2 = { pid3 };
							GuestProcessInfo[] info2 = gpm
									.listProcessesInGuest(npa, pids2);
							System.out.println("Strating time"
									+ info2[0].startTime.getTime());
							Calendar endTime1 = info2[0].endTime;

							while (endTime1 == null) {

								GuestProcessInfo[] info3 = gpm
										.listProcessesInGuest(npa, pids2);
								endTime1 = info3[0].endTime;
								System.out.println("processing...");

							}

							long pid4 = gpm.startProgramInGuest(npa, spec4);
							Thread.sleep(1000);
							long pid5 = gpm.startProgramInGuest(npa, spec5);
							Thread.sleep(1000);
							long pid6 = gpm.startProgramInGuest(npa, spec6);
							Thread.sleep(1000);

							long[] pids_1 = { pid6 };
							GuestProcessInfo[] info3 = gpm
									.listProcessesInGuest(npa, pids_1);
							System.out.println("Strating time"
									+ info3[0].startTime.getTime());
							Calendar endTime2 = info3[0].endTime;
							System.out
									.println("Ending time" + info3[0].endTime);
							while (endTime2 == null) {

								GuestProcessInfo[] info4 = gpm
										.listProcessesInGuest(npa, pids_1);
								endTime2 = info4[0].endTime;
								System.out.println("processing...");

							}

							si.getServerConnection().logout();
							return "200";
						}

					}

				}
			}

		}
		return "404";

	}

	public static void main(String args[]) throws Exception {
		GuestRunProgram_builddocker a = new GuestRunProgram_builddocker();
		a.GuestRunProgram_builddocker_execute("docker-2");
	}
}
