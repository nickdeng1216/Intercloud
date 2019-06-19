package hk.edu.polyu.intercloud.ctlstorage;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Test {

	public static void main(String args[])
			throws CtlStorageForIntercloudException {
		CtlStorageForIntercloud c = new CtlStorageForIntercloud();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		String strDate = sdf.format(new Date());
		c.listBuckets();
		// Upload
		c.putObject("intercloudtest01", strDate + "-photo", new File(
				"photo.jpg"));
		c.putObject("intercloudtest01", strDate + "-movie",
				new File("test.mp4"));
		// Download
		c.getObject("intercloudtest01", strDate + "-photo", new File(
				"downloaded.jpg"));
		c.getObject("intercloudtest01", strDate + "-photo", new File(
				"downloaded.mp4"));
	}
}
