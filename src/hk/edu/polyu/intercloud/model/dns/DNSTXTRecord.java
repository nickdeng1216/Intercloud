package hk.edu.polyu.intercloud.model.dns;

public class DNSTXTRecord {
	private String iccpVersion;
	private String service;
	private String vendor;
	private String geolocation;

	public String getIccpVersion() {
		return iccpVersion;
	}

	public void setIccpVersion(String iccpVersion) {
		this.iccpVersion = iccpVersion;
	}

	public String getService() {
		return service;
	}

	public void setService(String service) {
		this.service = service;
	}

	public String getVendor() {
		return vendor;
	}

	public void setVendor(String vendor) {
		this.vendor = vendor;
	}

	public String getGeolocation() {
		return geolocation;
	}

	public void setGeolocation(String geolocation) {
		this.geolocation = geolocation;
	}

	public void print() {
		System.out.println("ICCPVersion = " + this.iccpVersion);
		System.out.println("Service = " + this.service);
		System.out.println("Vendor = " + this.vendor);
		System.out.println("Geolocation = " + this.geolocation);
	}

}
