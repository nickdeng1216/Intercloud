package hk.edu.polyu.intercloud.test.performance;

import hk.edu.polyu.intercloud.api.ObjectStorageAPI;
import hk.edu.polyu.intercloud.api.ObjectStorageAPI.DATA_SECURITY;
import hk.edu.polyu.intercloud.main.Main;
import hk.edu.polyu.intercloud.util.LogUtil;

import java.util.concurrent.TimeUnit;

import javax.swing.JOptionPane;

public class PutObjectOverhead {

	static final String[] OBJECT_NAMES = new String[] { "5MB.zip", "10MB.zip",
			"20MB.zip", "50MB.zip", "100MB.zip", "200MB.zip", "512MB.zip",
			"1GB.zip" };
	static final int[] SLEEP_MINS = { 2, 2, 2, 2, 5, 5, 10, 20 };
	static final String TARGET = JOptionPane.showInputDialog("PUT to:");

	public static void main(String[] args) throws InterruptedException {
		try {
			Main.main(null);
			ObjectStorageAPI o = new ObjectStorageAPI(TARGET);
			for (int i = 0; i < OBJECT_NAMES.length; i++) {
				System.out.println("\n\n_" + OBJECT_NAMES[i] + "\n");
				LogUtil.logPerformance("API Invoke", TARGET,
						System.currentTimeMillis(), 0L);
				o.put(OBJECT_NAMES[i], "HTTPS", true, DATA_SECURITY.PRIVATE,
						false);
				System.out.println("ZZZ PERF TEST: Sleep for " + SLEEP_MINS[i]
						+ " mins.");
				TimeUnit.MINUTES.sleep(SLEEP_MINS[i]);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
