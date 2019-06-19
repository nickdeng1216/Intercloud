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
 * The class pulls down container from Docker hub. Step1,logging in Docker hub.
 * Step2,pulling the image to local docker. Step3,running the image.
 * 
 * @author Kate.xie
 *
 */
public class GuestRunProgram_containertransfer {
	/**
	 * 
	 * @param vmname
	 *            VM's name
	 * @param repository
	 *            repository's name
	 * @return return status number
	 * @throws Exception
	 */
	public String GuestRunProgram_pullcontainer_execute(String vmname,
			String repository) throws Exception {

		// connect to the sphere and find the vm
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

						if (vmname.equals(vm.getName())) {

							if (!"guestToolsRunning"
									.equals(vm.getGuest().toolsRunningStatus)) {
								System.out
										.println("The VMware Tools is not running in the Guest OS on VM: "
												+ vm.getName());
								System.out.println("Exiting...");

							}

							NamePasswordAuthentication npa = new NamePasswordAuthentication();
							System.out.println("kate" + vmname);
							getAccount account = new getAccount("docker_test1");

							npa.username = account.getValue("Username");
							npa.password = account.getValue("Password");

							GuestProgramSpec spec1 = new GuestProgramSpec();
							spec1.programPath = "/bin/echo";
							spec1.arguments = " \"docker login -u intercloudtest -p 12345678"
									+ "\"> docker_buildenvironment.sh";

							GuestProgramSpec spec2 = new GuestProgramSpec();
							spec2.programPath = "/bin/chmod";
							spec2.arguments = "777 docker_buildenvironment.sh ";

							GuestProgramSpec spec3 = new GuestProgramSpec();
							spec3.programPath = "/bin/bash";
							spec3.arguments = "docker_buildenvironment.sh ";

							GuestProgramSpec spec4 = new GuestProgramSpec();
							spec4.programPath = "/bin/echo";
							spec4.arguments = " \"docker pull" + repository
									+ "\"> docker_buildenvironment1.sh";

							GuestProgramSpec spec5 = new GuestProgramSpec();
							spec5.programPath = "/bin/chmod";
							spec5.arguments = "777 docker_buildenvironment1.sh ";

							GuestProgramSpec spec6 = new GuestProgramSpec();
							spec6.programPath = "/bin/bash";
							spec6.arguments = "docker_buildenvironment1.sh ";

							GuestProgramSpec spec7 = new GuestProgramSpec();
							spec7.programPath = "/bin/echo";
							spec7.arguments = " \"docker run -d -p 8080:80 "
									+ repository
									+ "\"> docker_buildenvironment2.sh";

							GuestProgramSpec spec8 = new GuestProgramSpec();
							spec8.programPath = "/bin/chmod";
							spec8.arguments = "777 docker_buildenvironment2.sh ";

							GuestProgramSpec spec9 = new GuestProgramSpec();
							spec9.programPath = "/bin/bash";
							spec9.arguments = "docker_buildenvironment2.sh ";

							GuestProcessManager gpm = gom.getProcessManager(vm);

							long pid1 = gpm.startProgramInGuest(npa, spec1);
							long pid2 = gpm.startProgramInGuest(npa, spec2);
							long pid3 = gpm.startProgramInGuest(npa, spec3);

							long pid4 = gpm.startProgramInGuest(npa, spec4);
							long pid5 = gpm.startProgramInGuest(npa, spec5);
							long pid6 = gpm.startProgramInGuest(npa, spec6);

							long pid7 = gpm.startProgramInGuest(npa, spec7);
							long pid8 = gpm.startProgramInGuest(npa, spec8);
							long pid9 = gpm.startProgramInGuest(npa, spec9);

							long[] pids = { pid9 };
							GuestProcessInfo[] info = gpm.listProcessesInGuest(
									npa, pids);
							System.out.println("Strating time"
									+ info[0].startTime.getTime());
							Calendar endTime = info[0].endTime;

							while (endTime == null) {

								GuestProcessInfo[] info1 = gpm
										.listProcessesInGuest(npa, pids);
								endTime = info1[0].endTime;
								System.out.println("pulling...");

							}

							GuestProgramSpec spec14 = new GuestProgramSpec();
							spec14.programPath = "/bin/echo";
							spec14.arguments = " \"docker restart "
									+ repository + "\"> aa.sh";
							GuestProgramSpec spec15 = new GuestProgramSpec();
							spec15.programPath = "/bin/bash";
							spec15.arguments = " aa.sh";

							long pid14 = gpm.startProgramInGuest(npa, spec14);
							long pid15 = gpm.startProgramInGuest(npa, spec15);
							long[] a = { pid14, pid15 };
							// Thread.sleep(20000);
							GuestProcessInfo ss[] = gpm.listProcessesInGuest(
									npa, a);
							System.out.println(ss[0].getDynamicType());
							System.out.println(ss[1].getEndTime());

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
		GuestRunProgram_containertransfer a = new GuestRunProgram_containertransfer();
		a.GuestRunProgram_pullcontainer_execute("docker",
				"intercloudtest/nginx_server:v1");

	}
}
