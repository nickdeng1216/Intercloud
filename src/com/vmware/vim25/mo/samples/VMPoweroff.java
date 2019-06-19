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

package com.vmware.vim25.mo.samples;

import java.net.URL;

import com.vmware.vim25.VirtualMachinePowerState;
import com.vmware.vim25.VirtualMachineRuntimeInfo;
import com.vmware.vim25.VirtualMachineSummary;
import com.vmware.vim25.mo.Datacenter;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.VirtualMachine;
import com.vmware.vim25.mo.samples.ovf.GetAccountInfo;

/**
 * The class powers off VM in VMware.
 * 
 * @author Kate.xie
 */

public class VMPoweroff {
	public String VMPoweroff_execute(String vAppOrVmName) throws Exception {
		// connect to the sphere and find the vm
		GetAccountInfo sphere = new GetAccountInfo("vsphere.properties");
		String URL = sphere.getValue("Url");
		String name = sphere.getValue("Username");
		String password = sphere.getValue("Password");
		String hostip = sphere.getValue("Hostip");
		boolean flag = false;
		String reason = "200";

		ServiceInstance si = new ServiceInstance(new URL(URL), name, password,
				true);
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

						VirtualMachineSummary summary = (vm.getSummary());

						VirtualMachineRuntimeInfo vmri = vm.getRuntime();
						if (vAppOrVmName.equals(vm.getName())) {
							if (vmri.getPowerState() != VirtualMachinePowerState.poweredOff) {
								Task task = vm.powerOffVM_Task();
								task.waitForMe();
								System.out.println("vm:" + vm.getName()
										+ " powered off.");
							}
							flag = true;
						}
					}
				}
			}
		}
		si.getServerConnection().logout();
		if (!flag) {
			throw new Exception("Operation failed.");
		}
		return reason;
	}

	public static void main(String[] args) throws Exception {
		VMPoweroff a = new VMPoweroff();
		String ao = a.VMPoweroff_execute("Gateway02");
		System.out.print(ao);

	}

}
