package com.vmware.vim25.mo.samples;

import java.net.URL;

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

/**
 * The class builds Nginx.Mysql and Wordpress. Step1,it downloads these three
 * softwares. Step2,it configures file. Step3,it runs these softwares.
 * 
 * @author Kate.xie
 *
 */
public class GuestRunProgram {

	public String GuestRunProgram_execute(String vmname) throws Exception {

		// connect to the sphere and find the vm
		String vAppOrVmName = "docker";
		ServiceInstance si = new ServiceInstance(new URL(
				"https://192.168.11.102/sdk"), "root", "%TGB6yhn", true);
		Folder rootFolder = si.getRootFolder();

		ManagedEntity[] mes = new InventoryNavigator(rootFolder)
				.searchManagedEntities("VirtualMachine");
		if (mes == null || mes.length == 0) {
			return "404";
		}

		GuestOperationsManager gom = si.getGuestOperationsManager();

		ManagedEntity[] mes1 = rootFolder.getChildEntity();

		for (int i = 0; i < mes.length; i++) {
			if (mes1[i] instanceof Datacenter) {
				Datacenter dc = (Datacenter) mes1[i];
				Folder vmFolder = dc.getVmFolder();
				ManagedEntity[] vms = vmFolder.getChildEntity();

				for (int j = 0; j < vms.length; j++) {
					if (vms[j] instanceof VirtualMachine) {
						VirtualMachine vm = (VirtualMachine) vms[j];

						if (vAppOrVmName.equals(vm.getName())) {

							if (!"guestToolsRunning"
									.equals(vm.getGuest().toolsRunningStatus)) {
								System.out
										.println("The VMware Tools is not running in the Guest OS on VM: "
												+ vm.getName());
								System.out.println("Exiting...");

							}

							NamePasswordAuthentication npa = new NamePasswordAuthentication();
							System.out.println(vmname);
							npa.username = vmname;

							npa.password = "12345678";

							GuestProgramSpec spec1 = new GuestProgramSpec();
							spec1.programPath = "/bin/echo";
							spec1.arguments = " \"docker run --name wordpress-db -e MYSQL_ROOT_PASSWORD=mysecretpassword -d mysql;docker run --name wordpress-app --link wordpress-db:mysql -d wordpress;mkdir wordpress-nginx; cd wordpress-nginx;\"> docker_buildenvironment.sh";

							GuestProgramSpec spec2 = new GuestProgramSpec();
							spec2.programPath = "/bin/chmod";
							spec2.arguments = "777 docker_buildenvironment.sh ";

							GuestProgramSpec spec3 = new GuestProgramSpec();
							spec3.programPath = "/bin/bash";
							spec3.arguments = "docker_buildenvironment.sh ";

							GuestProgramSpec spec5 = new GuestProgramSpec();
							spec5.programPath = "/bin/echo";
							spec5.arguments = " \" server {\n"

									+ "listen       80;\n "
									+ "server_name  localhost;\n"

									+ "error_log /var/log/nginx/error.log warn;\n"
									+ "location / {"
									+ "proxy_pass http://wordpress-app:80/;\n"

									+ "proxy_redirect http://server_name http://wordpress-app:80/;\n"

									+ "proxy_set_header   Host              \\$host; \n"

									+ "proxy_set_header   X-Forwarded-For   \\$proxy_add_x_forwarded_for;\n"

									+ "proxy_set_header   X-Forwarded-Proto  http;}\n"

									+ "error_page   500 502 503 504  /50x.html;\n"

									+ "location = /50x.html { root   /usr/share/nginx/html;  }\n"
									+ "}\" > default.conf; \n";

							GuestProgramSpec spec11 = new GuestProgramSpec();
							spec11.programPath = "/bin/bash ";
							spec11.arguments = " docker_buildenvironment_3.sh";

							GuestProgramSpec spec6 = new GuestProgramSpec();
							spec6.programPath = "/bin/echo";
							spec6.arguments = " \"FROM nginx \n COPY default.conf /etc/nginx/conf.d/default.conf \"  >> Dockerfile ";

							GuestProgramSpec spec7 = new GuestProgramSpec();
							spec7.programPath = "/bin/echo";
							spec7.arguments = " \"docker build -t wordpress-nginx .\" >docker_buildenvironment_1.sh ";

							GuestProgramSpec spec8 = new GuestProgramSpec();
							spec8.programPath = "/bin/bash";
							spec8.arguments = "docker_buildenvironment_1.sh ";

							GuestProgramSpec spec9 = new GuestProgramSpec();
							spec9.programPath = "/bin/echo";
							spec9.arguments = " \"docker run -d --name=wordpress-nginx --link=wordpress-app:wordpress-app -p 80:80 wordpress-nginx \" >docker_buildenvironment_2.sh ";
							GuestProgramSpec spec10 = new GuestProgramSpec();
							spec10.programPath = "/bin/bash";
							spec10.arguments = "docker_buildenvironment_2.sh ";

							GuestProcessManager gpm = gom.getProcessManager(vm);

							long pid1 = gpm.startProgramInGuest(npa, spec1);
							long pid2 = gpm.startProgramInGuest(npa, spec2);
							long pid3 = gpm.startProgramInGuest(npa, spec3);
							long pid5 = gpm.startProgramInGuest(npa, spec5);

							long pid6 = gpm.startProgramInGuest(npa, spec6);
							long pid7 = gpm.startProgramInGuest(npa, spec7);

							long pid9 = gpm.startProgramInGuest(npa, spec9);
							Thread.sleep(3000);
							long pid8 = gpm.startProgramInGuest(npa, spec8);
							long pid10 = gpm.startProgramInGuest(npa, spec10);

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
		GuestRunProgram a = new GuestRunProgram();
		a.GuestRunProgram_execute("docker");

	}
}
