package hk.edu.polyu.intercloud.examples;

import hk.edu.polyu.intercloud.api.ObjectStorageAPI;
import hk.edu.polyu.intercloud.api.ObjectStorageAPI.DATA_SECURITY;
import hk.edu.polyu.intercloud.main.Main;
import hk.edu.polyu.intercloud.minio.main.MinioClient;
import hk.edu.polyu.intercloud.minio.main.Result;
import hk.edu.polyu.intercloud.minio.messages.Item;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * This is an example of using the Gateway to sync an object, named Cherry.jpg,
 * in two clouds' object storage, via native Java calls. The class checks
 * whether the object on the cloud's Minio object storage server is modified
 * every 1 minute. If it is modified, it will transfer and store the object to
 * the storage cloud at iccp1.iccp.cf.
 * 
 * In the program, the while-loop runs every minute. It gets the last
 * modification time of the object compared to the time when the object is last
 * checked. If the last modification time is later than the last check time, it
 * will invoke the put method in the ObjectStorageAPI to store the object to the
 * cloud at iccp1.iccp.cf via HTTP.
 * 
 * @author Priere
 *
 */
public class SimpleSync {

	static final String END_POINT = "http://192.168.11.190:9000";
	static final String ACCESS_KEY = "APLEGXID2W6DLA91JEYV";
	static final String SECRET_KEY = "BVD7LJPf6xD177vxYqz9J4MoiGNugZGmtsLH+xFw";
	static final String BUCKET_NAME = "intercloud";
	static final String OBJECT_NAME = "apple.jpg";
	static final String TARGET = "iccp1.iccp.cf";
	static MinioClient minio;
	static ObjectStorageAPI o;

	public static void main(String[] args) {
		try {
			Main.main(null); // IMPORTANT! Run the Gateway!
			Date lastCheck = new Date();
			minio = new MinioClient(END_POINT, ACCESS_KEY, SECRET_KEY);
			o = new ObjectStorageAPI(TARGET);
			o.put(OBJECT_NAME, "HTTP", false, DATA_SECURITY.PUBLIC, true);
			while (true) {
				Date lastModified = getLastModified();
				Date now = new Date();
				System.out.println("File last modified at " + lastModified
						+ " [" + now + "]");
				// If the last modification is after the last check time
				if (lastModified.after(lastCheck)) {
					System.out.println("Syncing to the target cloud.");
					// Put the object to the target cloud
					o.put(OBJECT_NAME, "HTTP", false, DATA_SECURITY.PUBLIC,
							true);
				}
				lastCheck = now;
				TimeUnit.MINUTES.sleep(1); // 1 min.
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Check the last modification time using Minio's API
	static Date getLastModified() throws Exception {
		Iterable<Result<Item>> theObjects = minio.listObjects(BUCKET_NAME);
		for (Result<Item> result : theObjects) {
			Item item = result.get();
			if (item.objectName().equals(OBJECT_NAME))
				return item.lastModified();
		}
		throw new Exception("No such object");
	}
}
