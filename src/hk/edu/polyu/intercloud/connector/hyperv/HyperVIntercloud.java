package hk.edu.polyu.intercloud.connector.hyperv;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

public class HyperVIntercloud {

	private static final String HYPERV_PROP_FILE = System
			.getProperty("user.dir") + "/" + "hyperv.properties";
	public static String defaultNetworkCard;

	private final HyperVUtil u = new HyperVUtil();

	public HyperVIntercloud() throws HyperVIntercloudException {
		Properties properties = new Properties();
		InputStream inputStream;
		try {
			inputStream = new FileInputStream(HYPERV_PROP_FILE);
			properties.load(inputStream);
		} catch (IOException e) {
			System.err.println("Sorry, the file " + HYPERV_PROP_FILE
					+ " is not found or unable to read.");
			throw new HyperVIntercloudException(e.getMessage(), e);
		}
		try {
			defaultNetworkCard = properties.getProperty("default_network_card");
			System.out.println("Default network card: " + defaultNetworkCard);
		} catch (Exception e) {
			throw new HyperVIntercloudException(e.getMessage(), e);
		}
	}

	public VM getVMDetails(String name) throws HyperVIntercloudException {
		return getVMDetails("127.0.0.1", name);
	}

	public VM getVMDetails(String ip, String name)
			throws HyperVIntercloudException {
		return u.getVM(ip, name);
	}

	public List<String> getVMList() throws HyperVIntercloudException {
		return getVMList("127.0.0.1");
	}

	public List<String> getVMList(String ip) throws HyperVIntercloudException {
		return u.getVMs(ip);
	}

	public void newVM(String name, int numberOfCPUCores, int memoryGB,
			List<VHD> vhds, List<VHD> newVHDs, List<String> isos,
			int numberOfNetworkAdapters, boolean powerOn)
			throws HyperVIntercloudException {
		newVM("127.0.0.1", name, numberOfCPUCores, memoryGB, vhds, newVHDs,
				isos, numberOfNetworkAdapters, powerOn);
	}

	public void newVM(String ip, String name, int numberOfCPUCores,
			int memoryGB, List<VHD> vhds, List<VHD> newVHDs, List<String> isos,
			int numberOfNetworkAdapters, boolean powerOn)
			throws HyperVIntercloudException {
		if (checkName(ip, name) > 0) {
			throw new HyperVIntercloudException(
					HyperVIntercloudException.NAME_ALREADY_EXISTS);
		}
		u.createVM(ip, name, numberOfCPUCores, memoryGB, vhds, newVHDs, isos,
				numberOfNetworkAdapters);
		if (powerOn) {
			startVM(ip, name);
		}
	}

	public List<String> getVM(String name, String toDirectory)
			throws HyperVIntercloudException {
		return getVM("127.0.0.1", name, toDirectory);
	}

	public List<String> getVM(String ip, String name, String toDirectory)
			throws HyperVIntercloudException {
		if (checkName(ip, name) != 1) {
			throw new HyperVIntercloudException(
					HyperVIntercloudException.NAME_NOT_UNIQUE);
		}
		return u.getVMFilePaths(ip, name, toDirectory);
	}

	public void putVM(String name, String fromDirectory)
			throws HyperVIntercloudException {
		putVM("127.0.0.1", name, fromDirectory);
	}

	public void putVM(String ip, String name, String fromDirectory)
			throws HyperVIntercloudException {
		u.putVMFiles(ip, name, fromDirectory);
	}

	public void deleteVM(String name, boolean forced)
			throws HyperVIntercloudException, InterruptedException {
		deleteVM("127.0.0.1", name, forced);
	}

	public void deleteVM(String ip, String name, boolean forced)
			throws HyperVIntercloudException, InterruptedException {
		if (checkName(ip, name) != 1) {
			throw new HyperVIntercloudException(
					HyperVIntercloudException.NAME_NOT_UNIQUE);
		}
		u.deleteVM(ip, name, forced);
	}

	public void startVM(String name) throws HyperVIntercloudException {
		startVM("127.0.0.1", name);
	}

	public void startVM(String ip, String name)
			throws HyperVIntercloudException {
		if (checkName(ip, name) != 1) {
			throw new HyperVIntercloudException(
					HyperVIntercloudException.NAME_NOT_UNIQUE);
		}
		u.startVM(ip, name);
	}

	public void shutdownVM(String name, boolean forced)
			throws HyperVIntercloudException {
		shutdownVM("127.0.0.1", name, forced);
	}

	public void shutdownVM(String ip, String name, boolean forced)
			throws HyperVIntercloudException {
		if (checkName(ip, name) != 1) {
			throw new HyperVIntercloudException(
					HyperVIntercloudException.NAME_NOT_UNIQUE);
		}
		u.shutdownVM(ip, name, forced);
	}

	public void suspendVM(String name) throws HyperVIntercloudException {
		suspendVM("127.0.0.1", name);
	}

	public void suspendVM(String ip, String name)
			throws HyperVIntercloudException {
		if (checkName(ip, name) != 1) {
			throw new HyperVIntercloudException(
					HyperVIntercloudException.NAME_NOT_UNIQUE);
		}
		u.suspendVM(ip, name);
	}

	public int checkName(String name) throws HyperVIntercloudException {
		return checkName("127.0.0.1", name);
	}

	public int checkName(String ip, String name)
			throws HyperVIntercloudException {
		return u.getVMID(ip, name).size();
	}

}
