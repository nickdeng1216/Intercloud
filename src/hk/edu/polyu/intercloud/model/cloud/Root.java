package hk.edu.polyu.intercloud.model.cloud;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

public class Root implements Intercloud {

	private List<Exchange> listOfExchange;

	public List<Exchange> getListOfExchange() {
		return listOfExchange;
	}

	public void setListOfExchange(List<Exchange> listOfExchange) {
		this.listOfExchange = listOfExchange;
	}

	private String ip;
	private int port;
	private String name;
	private String system;
	private String systemVersion;

	@Override
	public String getIp() {
		try {
			ip = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ip;
	}

	@Override
	public int getPort() {
		return port;
	}

	@Override
	public String getName() {
		return name;
	}

}
