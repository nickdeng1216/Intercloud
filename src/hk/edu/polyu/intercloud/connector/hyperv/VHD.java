package hk.edu.polyu.intercloud.connector.hyperv;

public class VHD {

	private String path;
	private int size;

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public VHD(String path, int size) {
		super();
		this.path = path;
		this.size = size;
	}

}
