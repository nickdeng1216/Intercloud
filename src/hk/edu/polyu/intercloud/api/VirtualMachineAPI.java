package hk.edu.polyu.intercloud.api;

import hk.edu.polyu.intercloud.command.vm.PutVM;
import hk.edu.polyu.intercloud.command.vm.VMPowerControl;
import hk.edu.polyu.intercloud.common.Common;
import hk.edu.polyu.intercloud.communication.Sockets;
import hk.edu.polyu.intercloud.exceptions.AuthenticationAPIException;
import hk.edu.polyu.intercloud.exceptions.ObjectStorageAPIException;
import hk.edu.polyu.intercloud.exceptions.VirtualMachineAPIException;
import hk.edu.polyu.intercloud.model.protocol.AdditionalInformation;
import hk.edu.polyu.intercloud.model.protocol.GeneralInformation;
import hk.edu.polyu.intercloud.model.protocol.Protocol;
import hk.edu.polyu.intercloud.model.protocol.RequestInformation;
import hk.edu.polyu.intercloud.model.protocol.ResponseInformation;
import hk.edu.polyu.intercloud.util.DatabaseUtil;
import hk.edu.polyu.intercloud.util.ProtocolUtil;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

/**
 * The API class for VM service
 * 
 * @author Kate
 *
 */
public class VirtualMachineAPI {

	/**
	 * The target cloud
	 */
	private String cloud;

	/**
	 * Construct a VM API class object.
	 * 
	 * @param cloud
	 *            The name of the target cloud
	 * @throws AuthenticationAPIException
	 * @throws ObjectStorageAPIException
	 */
	public VirtualMachineAPI(String cloud) throws AuthenticationAPIException,
			ObjectStorageAPIException {
		this.cloud = cloud;
		AuthenticationAPI aAPI = new AuthenticationAPI();
		aAPI.checkAuth(cloud);
		checkMyService();
	}

	private void checkMyService() throws ObjectStorageAPIException {
		if (!Common.my_service_providers.containsKey("VM")) {
			throw new ObjectStorageAPIException("VM" + " is not provided by "
					+ Common.my_name);
		}
	}

	public final class POWER {
		public static final String ON = VMPowerControl.ON;
		public static final String OFF = VMPowerControl.OFF;
		public static final String SUSPEND = VMPowerControl.SUSPEND;
	}

	/**
	 * Control the power status of the latest version of a VM. Example:
	 * 
	 * <pre>
	 * powerControl(&quot;MyVM&quot;, POWER.ON, true);
	 * </pre>
	 * 
	 * turns on the VM named "MyVM" by a signed protocol message.
	 * 
	 * @param vmname
	 *            The name of the VM.
	 * @param power
	 *            The power status, can either be POWER.ON, POWER.OFF or
	 *            POWER.SUSPEND.
	 * @param protocolSecurity
	 *            Whether the protocol message needs to be signed or not.
	 * @return The protocol ID, used for status checking.
	 * @throws VirtualMachineAPIException
	 */
	public String powerControl(String vmname, String power,
			boolean protocolSecurity) throws VirtualMachineAPIException {
		return this.powerControl(vmname, null, power, protocolSecurity);
	}

	/**
	 * Control the power status of a VM. Example:
	 * 
	 * <pre>
	 * powerControl(&quot;MyVM&quot;, &quot;123456789&quot;, POWER.ON, true);
	 * </pre>
	 * 
	 * turns on the version "123456789" of the VM named "MyVM" by a signed
	 * protocol message.
	 * 
	 * @param vmname
	 *            The name of the VM.
	 * @param version
	 *            The version number of the VM. (You may use listVM to get it)
	 * @param power
	 *            The power status, can either be POWER.ON, POWER.OFF or
	 *            POWER.SUSPEND.
	 * @param protocolSecurity
	 *            Whether the protocol message needs to be signed or not.
	 * @return The protocol ID, used for status checking.
	 * @throws VirtualMachineAPIException
	 */
	public String powerControl(String vmname, String version, String power,
			boolean protocolSecurity) throws VirtualMachineAPIException {
		try {
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date date = new Date();
			String[] dateTime = dateFormat.format(date).split("\\s+");

			GeneralInformation generalInformation = new GeneralInformation(
					Common.my_name, cloud, dateTime[0], dateTime[1]);

			RequestInformation requestInformation = new RequestInformation();
			requestInformation.setCommand("VMPowerControl");
			requestInformation.setService("VM");
			requestInformation.addTags("VMName", vmname);
			requestInformation.addTags("Power", power);
			if (version != null) {
				requestInformation.addTags("VMVersion", version);
			}

			ResponseInformation responseInformation = null;

			String protocolid = ProtocolUtil.generateID();
			AdditionalInformation additional = new AdditionalInformation();
			Protocol protocol = new Protocol(Common.ICCP_VER, protocolid,
					generalInformation, requestInformation,
					responseInformation, additional, null);

			String pro_String = ProtocolUtil.generateRequest(protocol,
					protocolSecurity);

			String ip = Common.my_friends.get(cloud).getIp();
			int port = Common.GW_PORT;

			Sockets socket = new Sockets(ip, port, Common.my_name);
			socket.sendMessage(pro_String);

			return protocolid;
		} catch (Exception e) {

			throw new VirtualMachineAPIException(e.getMessage(), e);

		}

	}

	/**
	 * Get the details of the latest version of a VM. Example:
	 * 
	 * <pre>
	 * getVMDetails(&quot;MyVM&quot;, false);
	 * </pre>
	 * 
	 * gets the details of the VM named "MyVM" by an unsigned protocol message.
	 * 
	 * @param vmname
	 *            The name of the VM.
	 * @param protocolSecurity
	 *            Whether the protocol message needs to be signed or not.
	 * @return The protocol ID, used for status checking.
	 * @throws VirtualMachineAPIException
	 */
	public String getVMDetails(String vmname, boolean protocolSecurity)
			throws VirtualMachineAPIException {
		return this.getVMDetails(vmname, null, protocolSecurity);
	}

	/**
	 * Get the details of a VM. Example:
	 * 
	 * <pre>
	 * getVMDetails(&quot;MyVM&quot;, &quot;123456789&quot;, false);
	 * </pre>
	 * 
	 * gets the details of the version "123456789" of the VM named "MyVM" by an
	 * unsigned protocol message.
	 * 
	 * @param vmname
	 *            The name of the VM.
	 * @param version
	 *            The version number of the VM. (You may use listVM to get it)
	 * @param protocolSecurity
	 *            Whether the protocol message needs to be signed or not.
	 * @return The protocol ID, used for status checking.
	 * @throws VirtualMachineAPIException
	 */
	public String getVMDetails(String vmname, String version,
			boolean protocolSecurity) throws VirtualMachineAPIException {
		try {
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date date = new Date();
			String[] dateTime = dateFormat.format(date).split("\\s+");

			GeneralInformation generalInformation = new GeneralInformation(
					Common.my_name, cloud, dateTime[0], dateTime[1]);

			RequestInformation requestInformation = new RequestInformation();
			requestInformation.setCommand("GetVMDetails");
			requestInformation.setService("VM");
			requestInformation.addTags("VMName", vmname);
			if (version != null) {
				requestInformation.addTags("VMVersion", version);
			}

			ResponseInformation responseInformation = null;

			String protocolid = ProtocolUtil.generateID();
			AdditionalInformation additional = new AdditionalInformation();
			Protocol protocol = new Protocol(Common.ICCP_VER, protocolid,
					generalInformation, requestInformation,
					responseInformation, additional, null);

			String pro_String = ProtocolUtil.generateRequest(protocol,
					protocolSecurity);

			String ip = Common.my_friends.get(cloud).getIp();
			int port = Common.GW_PORT;

			Sockets socket = new Sockets(ip, port, Common.my_name);
			socket.sendMessage(pro_String);

			return protocolid;

		} catch (Exception e) {
			throw new VirtualMachineAPIException(e.getMessage(), e);
		}

	}

	/**
	 * Get a list of VMs hosted. Example:
	 * 
	 * <pre>
	 * getVMList(true);
	 * </pre>
	 * 
	 * gets a list of VMs by a signed protocol message.
	 * 
	 * @param protocolSecurity
	 *            Whether the protocol message needs to be signed or not.
	 * @return The protocol ID, used for status checking.
	 * @throws VirtualMachineAPIException
	 */
	public String listVM(boolean protocolSecurity)
			throws VirtualMachineAPIException {
		try {
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date date = new Date();
			String[] dateTime = dateFormat.format(date).split("\\s+");

			GeneralInformation generalInformation = new GeneralInformation(
					Common.my_name, cloud, dateTime[0], dateTime[1]);

			RequestInformation requestInformation = new RequestInformation();
			requestInformation.setCommand("ListVM");
			requestInformation.setService("VM");

			ResponseInformation responseInformation = null;

			String protocolid = ProtocolUtil.generateID();
			AdditionalInformation additional = new AdditionalInformation();
			Protocol protocol = new Protocol(Common.ICCP_VER, protocolid,
					generalInformation, requestInformation,
					responseInformation, additional, null);

			String pro_String = ProtocolUtil.generateRequest(protocol,
					protocolSecurity);

			String ip = Common.my_friends.get(cloud).getIp();
			int port = Common.GW_PORT;

			Sockets socket = new Sockets(ip, port, Common.my_name);
			socket.sendMessage(pro_String);

			return protocolid;
		} catch (Exception e) {
			throw new VirtualMachineAPIException(e.getMessage(), e);
		}

	}

	/**
	 * Create a VM in the target cloud. Example:
	 * 
	 * <pre>
	 * createVM(&quot;MyVM&quot;, false);
	 * </pre>
	 * 
	 * creates a VM named "MyVM" by an unsigned protocol message.
	 * 
	 * @param vmname
	 *            The name of the new VM.
	 * @param memory
	 *            Memory size, in GB.
	 * @param disksize
	 *            Disk size, in GB.
	 * @param cpu
	 *            Number of CPU(s).
	 * @param protocolSecurity
	 *            Whether the protocol message needs to be signed or not.
	 * @return The protocol ID, used for status checking.
	 * @throws VirtualMachineAPIException
	 */
	public String createVM(String vmname, double memory, double disksize,
			double cpu, boolean protocolSecurity)
			throws VirtualMachineAPIException {
		try {
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date date = new Date();
			String[] dateTime = dateFormat.format(date).split("\\s+");

			GeneralInformation generalInformation = new GeneralInformation(
					Common.my_name, cloud, dateTime[0], dateTime[1]);

			RequestInformation requestInformation = new RequestInformation();
			requestInformation.setCommand("CreateVM");
			requestInformation.setService("VM");
			// in GB
			requestInformation.addTags("MemorySize",
					String.valueOf((int) memory));
			// in GB
			requestInformation.addTags("DiskSize",
					String.valueOf((int) disksize));
			requestInformation.addTags("CpuCoreNum", String.valueOf((int) cpu));
			requestInformation.addTags("VMName", vmname);

			ResponseInformation responseInformation = null;

			String protocolid = ProtocolUtil.generateID();
			AdditionalInformation additional = new AdditionalInformation();
			Protocol protocol = new Protocol(Common.ICCP_VER, protocolid,
					generalInformation, requestInformation,
					responseInformation, additional, null);

			String pro_String = ProtocolUtil.generateRequest(protocol,
					protocolSecurity);

			String ip = Common.my_friends.get(cloud).getIp();
			int port = Common.GW_PORT;

			Sockets socket = new Sockets(ip, port, Common.my_name);
			socket.sendMessage(pro_String);

			return protocolid;
		} catch (Exception e) {

			throw new VirtualMachineAPIException(e.getMessage(), e);

		}

	}

	/**
	 * Retrieve the latest version of a VM from the target cloud and host it.
	 * 
	 * @param vmname
	 *            The name of the VM.
	 * @param transferMethod
	 *            The data transfer method, e.g. HTTPS.
	 * @param protocolSecurity
	 *            Whether the protocol message needs to be signed or not.
	 * @return The protocol ID, used for status checking.
	 * @throws VirtualMachineAPIException
	 */
	public String getVM(String vmname, String transferMethod,
			boolean protocolSecurity) throws VirtualMachineAPIException {
		return this.getVM(vmname, null, transferMethod, protocolSecurity);
	}

	/**
	 * Retrieve a VM from the target cloud and host it.
	 * 
	 * @param vmname
	 *            The name of the VM.
	 * @param version
	 *            The version number of the VM. (You may use listVM to get it)
	 * @param transferMethod
	 *            The data transfer method, e.g. HTTPS.
	 * @param protocolSecurity
	 *            Whether the protocol message needs to be signed or not.
	 * @return The protocol ID, used for status checking.
	 * @throws VirtualMachineAPIException
	 */
	public String getVM(String vmname, String version, String transferMethod,
			boolean protocolSecurity) throws VirtualMachineAPIException {
		try {
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date date = new Date();
			String[] dateTime = dateFormat.format(date).split("\\s+");

			GeneralInformation generalInformation = new GeneralInformation(
					Common.my_name, cloud, dateTime[0], dateTime[1]);

			RequestInformation requestInformation = new RequestInformation();
			requestInformation.setCommand("GetVM");
			requestInformation.setService("VM");
			requestInformation.addTags("VMName", vmname);
			if (version != null) {
				requestInformation.addTags("VMVersion", version);
			}
			requestInformation.addTags("TranferMethod", transferMethod);
			// requestInformation.addTags("GuestOS", guestOS);
			ResponseInformation responseInformation = null;

			String protocolid = ProtocolUtil.generateID();

			AdditionalInformation additional = new AdditionalInformation();
			Protocol protocol = new Protocol(Common.ICCP_VER, protocolid,
					generalInformation, requestInformation,
					responseInformation, additional, null);

			String pro_String = ProtocolUtil.generateRequest(protocol,
					protocolSecurity);

			String ip = Common.my_friends.get(cloud).getIp();
			int port = Common.GW_PORT;

			Sockets socket = new Sockets(ip, port, Common.my_name);
			socket.sendMessage(pro_String);

			return protocolid;
		} catch (Exception e) {
			throw new VirtualMachineAPIException(e.getMessage(), e);
		}
	}

	/**
	 * Store a VM to another cloud to host it. Example:
	 * 
	 * <pre>
	 * putVM(&quot;MyVM&quot;, &quot;HTTPS&quot;, true)
	 * </pre>
	 * 
	 * stores a VM named "MyVM" to the target cloud via HTTPS by a signed
	 * protocol message. The target cloud will host the VM afterwards.
	 * 
	 * @param vmname
	 *            The name of the VM.
	 * @param transferMethod
	 *            The data transfer method, e.g. HTTPS.
	 * @param protocolSecurity
	 *            Whether the protocol message needs to be signed or not.
	 * @return The protocol ID, used for status checking.
	 * @throws VirtualMachineAPIException
	 */
	public String putVM(String vmname, String transferMethod,
			boolean protocolSecurity) throws VirtualMachineAPIException {
		try {
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date date = new Date();
			String[] dateTime = dateFormat.format(date).split("\\s+");

			GeneralInformation generalInformation = new GeneralInformation(
					Common.my_name, cloud, dateTime[0], dateTime[1]);

			RequestInformation requestInformation = new RequestInformation();
			requestInformation.setCommand("PutVM");
			requestInformation.setService("VM");

			requestInformation.addTags("VMName", vmname);
			requestInformation.addTags("TranferMethod", transferMethod);

			ResponseInformation responseInformation = null;

			String protocolid = ProtocolUtil.generateID();
			AdditionalInformation additional = new AdditionalInformation();
			PutVM vm = new PutVM();

			HashMap<String, String> map = new HashMap<>();
			map.put("VMName", vmname);
			map.put("TransferMethod", transferMethod);
			additional = vm.pre_execute(null, map, null);
			Protocol protocol = new Protocol(Common.ICCP_VER, protocolid,
					generalInformation, requestInformation,
					responseInformation, additional, null);

			/**
			 * Insert own VM
			 */
			DatabaseUtil.insertOwnVMTable(protocolid, vmname, cloud);

			String pro_String = ProtocolUtil.generateRequest(protocol,
					protocolSecurity);

			String ip = Common.my_friends.get(cloud).getIp();
			int port = Common.GW_PORT;

			Sockets socket = new Sockets(ip, port, Common.my_name);
			socket.sendMessage(pro_String);

			return protocolid;
		} catch (Exception e) {

			throw new VirtualMachineAPIException(e.getMessage(), e);

		}

	}
}
