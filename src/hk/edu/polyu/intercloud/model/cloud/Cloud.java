package hk.edu.polyu.intercloud.model.cloud;

/**
 * This class is used to store cloud info
 * 
 * @author Kate
 * @author harry
 * 
 */

public class Cloud {
	private String ip;
	private String name;
	private String role;
	private boolean auth;

	public Cloud(String ip, String name, String role, boolean auth) {
		this.ip = ip;
		this.name = name;
		this.role = role;
		this.auth = auth;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean getAuth() {
		return auth;
	}

	public void setAuth(boolean auth) {
		this.auth = auth;
	}
}
