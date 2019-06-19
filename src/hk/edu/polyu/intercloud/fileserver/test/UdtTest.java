package hk.edu.polyu.intercloud.fileserver.test;

import hk.edu.polyu.intercloud.fileserver.client.Udt;
import hk.edu.polyu.intercloud.fileserver.exceptions.UdtException;

public class UdtTest {

	public static void main(String[] args) throws UdtException {
		Udt.download("iccp3.iccp.cf", 9000, "minio.exe", "minio1.exe");
	}
}
