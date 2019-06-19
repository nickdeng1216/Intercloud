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

public class GuestRunProgram_DBtransfer_push {

	public String GuestRunProgram_DBtransfer_push_execute(String vmname,
			String container, String hubname, String db) throws Exception {

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
							System.out.println("what " + vmname);
							getAccount account = new getAccount(vmname);

							npa.username = account.getValue("Username");

							npa.password = account.getValue("Password");

							GuestProgramSpec spec1_1 = new GuestProgramSpec();
							spec1_1.programPath = "/bin/echo";
							spec1_1.arguments = " \"docker exec -it "
									+ container + "  bash \"> DB.sh";
							System.out.print(spec1_1.arguments);

							GuestProgramSpec spec2_1 = new GuestProgramSpec();
							spec2_1.programPath = "/bin/chmod";
							spec2_1.arguments = "777 DB.sh ";

							GuestProgramSpec spec3_1 = new GuestProgramSpec();
							spec3_1.programPath = "/bin/bash";
							spec3_1.arguments = "DB.sh ";

							GuestProgramSpec spec1_2 = new GuestProgramSpec();
							spec1_2.programPath = "/bin/echo";
							spec1_2.arguments = " \" echo '12345678' | sudo -S mysql -u root -p "
									+ db + "> " + db + ".sql \"> DB1.sh";
							System.out.print(spec1_2.arguments);

							GuestProgramSpec spec2_2 = new GuestProgramSpec();
							spec2_2.programPath = "/bin/chmod";
							spec2_2.arguments = "777 DB1.sh ";

							GuestProgramSpec spec3_2 = new GuestProgramSpec();
							spec3_2.programPath = "/bin/bash";
							spec3_2.arguments = "DB1.sh ";

							GuestProgramSpec spec1 = new GuestProgramSpec();
							spec1.programPath = "/bin/echo";
							spec1.arguments = " \"docker stop \\$(docker ps --filter \"name="
									+ container + "\" -q );  " + "\"> DB2.sh";
							System.out.print(spec1.arguments);

							GuestProgramSpec spec2 = new GuestProgramSpec();
							spec2.programPath = "/bin/chmod";
							spec2.arguments = "777 DB2.sh ";

							GuestProgramSpec spec3 = new GuestProgramSpec();
							spec3.programPath = "/bin/bash";
							spec3.arguments = "DB2.sh ";

							GuestProgramSpec spec4 = new GuestProgramSpec();
							spec4.programPath = "/bin/echo";
							spec4.arguments = " \" docker commit \\$(docker ps -a --filter \"name="
									+ container
									+ " \" -q ) intercloudtest/"
									+ hubname + ";" + "\"> DB3.sh";
							System.out.print(spec4.arguments);

							GuestProgramSpec spec5 = new GuestProgramSpec();
							spec5.programPath = "/bin/chmod";
							spec5.arguments = "777 DB3.sh ";

							GuestProgramSpec spec6 = new GuestProgramSpec();
							spec6.programPath = "/bin/bash";
							spec6.arguments = "DB3.sh ";

							GuestProgramSpec spec10 = new GuestProgramSpec();
							spec10.programPath = "/bin/echo";
							spec10.arguments = " \"  docker login -u intercloudtest -p 12345678;docker push"
									+ "  intercloudtest/"
									+ hubname
									+ ";"
									+ "\"> DB4.sh";
							System.out.print(spec10.arguments);

							GuestProgramSpec spec11 = new GuestProgramSpec();
							spec11.programPath = "/bin/chmod";
							spec11.arguments = "777 DB4.sh ";

							GuestProgramSpec spec12 = new GuestProgramSpec();
							spec12.programPath = "/bin/bash";
							spec12.arguments = "DB4.sh ";

							GuestProcessManager gpm = gom.getProcessManager(vm);

							long pid1_1 = gpm.startProgramInGuest(npa, spec1_1);
							long pid2_1 = gpm.startProgramInGuest(npa, spec2_1);
							long pid3_1 = gpm.startProgramInGuest(npa, spec3_1);

							long pid1_2 = gpm.startProgramInGuest(npa, spec1_2);
							long pid2_2 = gpm.startProgramInGuest(npa, spec2_2);
							long pid3_2 = gpm.startProgramInGuest(npa, spec3_2);

							long pid1 = gpm.startProgramInGuest(npa, spec1);
							long pid2 = gpm.startProgramInGuest(npa, spec2);
							long pid3 = gpm.startProgramInGuest(npa, spec3);

							long pid4 = gpm.startProgramInGuest(npa, spec4);
							long pid5 = gpm.startProgramInGuest(npa, spec5);
							long pid6 = gpm.startProgramInGuest(npa, spec6);
							// Thread.sleep(3000);
							// long pid7 = gpm.startProgramInGuest(npa, spec7);
							// long pid8 = gpm.startProgramInGuest(npa, spec8);
							// long pid9 = gpm.startProgramInGuest(npa, spec9);
							Thread.sleep(3000);

							long pid10 = gpm.startProgramInGuest(npa, spec10);
							long pid11 = gpm.startProgramInGuest(npa, spec11);
							long pid12 = gpm.startProgramInGuest(npa, spec12);
							// System.out.print(pid1);
							// Thread.sleep(1000);

							long[] pids = { pid12 };
							GuestProcessInfo[] info = gpm.listProcessesInGuest(
									npa, pids);
							System.out.println("Strating time"
									+ info[0].startTime.getTime());
							Calendar endTime = info[0].endTime;
							System.out.println(info[0].endTime);
							while (endTime == null) {

								GuestProcessInfo[] info1 = gpm
										.listProcessesInGuest(npa, pids);
								endTime = info1[0].endTime;
								System.out.println("pushing...");

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
		GuestRunProgram_DBtransfer_push a = new GuestRunProgram_DBtransfer_push();
		a.GuestRunProgram_DBtransfer_push_execute("docker_test1", "root",
				"mysql_2", "Intercloud");
	}
}
