package hk.edu.polyu.intercloud.model.cloud;

/**
 * 
 * @author harry
 *
 */
public class Exchange implements Intercloud {

	private String ip;
	private int port;
	private String name;

	/**
	 * set ip and serverport, make listofcloud instantiation
	 * 
	 * @param ip
	 * @param port
	 */
	public Exchange(String ip, int port, String name) {
		this.ip = ip;
		this.port = port;
		this.name = name;
	}

	@Override
	public String getIp() {
		return this.ip;
	}

	@Override
	public int getPort() {
		return this.port;
	}

	@Override
	public String getName() {
		return name;
	}

}
