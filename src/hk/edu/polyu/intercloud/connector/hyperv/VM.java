package hk.edu.polyu.intercloud.connector.hyperv;

import java.util.List;

public class VM {

	private long memoryB;
	private int numberOfCPUCores;
	private List<String> networkAdapters;
	private String state;
	private String os;

	public long getMemoryB() {
		return memoryB;
	}

	public void setMemoryB(long memoryB) {
		this.memoryB = memoryB;
	}

	public int getNumberOfCPUCores() {
		return numberOfCPUCores;
	}

	public void setNumberOfCPUCores(int numberOfCPUCores) {
		this.numberOfCPUCores = numberOfCPUCores;
	}

	public List<String> getNetworkAdapters() {
		return networkAdapters;
	}

	public void setNetworkAdapters(List<String> networkAdapters) {
		this.networkAdapters = networkAdapters;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getOs() {
		return os;
	}

	public void setOs(String os) {
		this.os = os;
	}
}
