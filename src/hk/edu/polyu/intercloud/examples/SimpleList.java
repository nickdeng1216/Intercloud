package hk.edu.polyu.intercloud.examples;

import hk.edu.polyu.intercloud.api.ObjectStorageAPI;
import hk.edu.polyu.intercloud.main.Main;

import java.util.concurrent.TimeUnit;

/**
 * This is an example of continuous monitoring of the objects stored in the
 * storage cloud via native Java calls. The class queries the storage cloud,
 * iccp1.iccp.cf, every 1 minute.
 * 
 * In the program, the while-loop runs every minute. It invokes the list method
 * in the ObjectStorageAPI to query for a list of the object stored in the cloud
 * at iccp1.iccp.cf.
 * 
 * @author Priere
 *
 */
public class SimpleList {

	static final String TARGET = "iccp1.iccp.cf";

	public static void main(String[] args) {
		try {
			Main.main(null); // IMPORTANT! Run the Gateway!
			ObjectStorageAPI o = new ObjectStorageAPI(TARGET);
			while (true) {
				o.list(false); // Ask TARGET for a list of objects I stored
				TimeUnit.MINUTES.sleep(1); // Sleep for 1 min.
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
