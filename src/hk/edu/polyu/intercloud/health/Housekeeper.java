package hk.edu.polyu.intercloud.health;

import hk.edu.polyu.intercloud.common.Common;
import hk.edu.polyu.intercloud.fileserver.client.Http;
import hk.edu.polyu.intercloud.model.cloud.Cloud;
import hk.edu.polyu.intercloud.security.CER;
import hk.edu.polyu.intercloud.security.CRL;
import hk.edu.polyu.intercloud.util.CertificateUtil;
import hk.edu.polyu.intercloud.util.DatabaseUtil;
import hk.edu.polyu.intercloud.util.LogUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class Housekeeper implements Callable<String> {

	public static void start() {
		System.out
				.println("Housekeeping runs every "
						+ Common.housekeeperInterval
						+ "s."
						+ System.lineSeparator()
						+ "You may modify \"housekeeper_interval\" in the properties file.");
		Runnable r = new Runnable() {
			@Override
			public void run() {
				while (true) {
					try {
						Housekeeper h = new Housekeeper();
						ExecutorService executor = Executors
								.newSingleThreadExecutor();
						System.out.println(new SimpleDateFormat(
								"yyyy-MM-dd HH:mm:ss").format(new Date())
								+ "\tHousekeeping...");
						Future<String> future = executor.submit(h);
						String result = future.get();
						executor.shutdown();
						System.out.println(new SimpleDateFormat(
								"yyyy-MM-dd HH:mm:ss").format(new Date())
								+ "\tHousekeeping " + result + ".");
						TimeUnit.SECONDS.sleep(Common.housekeeperInterval);
					} catch (InterruptedException e) {
					} catch (ExecutionException e) {
						LogUtil.logException(e);
					}
				}
			}
		};
		new Thread(r, "Housekeeper").start();
	}

	@Override
	public String call() throws Exception {
		// Retrieve
		String path_cer = Common.KEY_PATH + "CA" + File.separator
				+ Common.ca_name + ".cer";
		String path_crl = Common.KEY_PATH + "CA" + File.separator
				+ Common.ca_name + ".crl";
		String path_pem = Common.KEY_PATH + "CA" + File.separator
				+ Common.ca_name + ".pem";
		// delete old version firstly
		File file_cer = new File(path_cer);
		file_cer.delete();
		File file_crl = new File(path_crl);
		file_crl.delete();
		File file_pem = new File(path_pem);
		file_pem.delete();
		// Start retrieving default https
		Http.download("http://" + Common.ca_ip + ":12002/" + Common.ca_name
				+ ".cer", path_cer);
		Http.download("http://" + Common.ca_ip + ":12002/" + Common.ca_name
				+ ".crl", path_crl);
		Http.download("http://" + Common.ca_ip + ":12002/" + Common.ca_name
				+ ".pem", path_pem);
		// verify
		for (Map.Entry<String, Cloud> entry : Common.my_friends.entrySet()) {
			boolean b = verify(entry.getKey());
			if (!b) {
				Common.my_friends.get(entry.getKey()).setAuth(b);
			}
		}
		DatabaseUtil.clearConnectedClouds();
		for (Map.Entry<String, Cloud> entry : Common.my_friends.entrySet()) {
			DatabaseUtil.insertConnectedCloud(entry.getKey(), entry.getValue());
		}
		return "Completed";
	}

	boolean verify(String name) throws Exception {
		CER cer = new CER();
		CRL crl = new CRL();
		String cer_path = Common.KEY_PATH + "Others" + File.separator + name
				+ ".cer";
		String key_path = Common.KEY_PATH + "Others" + File.separator + name
				+ ".pem";
		File f_cer = new File(cer_path);
		File f_key = new File(key_path);
		boolean flag = true;
		if (!f_cer.exists()) {
			flag = false;
			return flag;
		}
		if (!f_key.exists()) {
			flag = false;
			return flag;
		}
		String content = CertificateUtil.readfile(cer_path);

		// check expired or not
		boolean step1 = cer.checkValidity_string(content);
		// check ca's signature
		boolean step2 = cer.verify_string(content, Common.KEY_PATH + "CA"
				+ File.separator + Common.ca_name + ".pem");
		// check with ca name
		boolean step3 = crl.verifyCA(cer_path);
		// check crl
		boolean step4 = crl.verifyrevoke(cer_path);

		// if not valid return exception
		if (!(step1 && step2 && step3 && step4)) {

			File file = new File(cer_path);
			File file_key = new File(key_path);
			if (file.delete())
				System.out.println("Certificate deleted");
			if (file_key.delete())
				System.out.println("Public key deleted");
			flag = false;

		}
		return flag;
	}

	static String getProp(String item) throws IOException {
		Properties properties = new Properties();
		InputStream inputStream;
		String prop = "";
		inputStream = new FileInputStream(Common.GW_PROP_FILE);
		properties.load(inputStream);
		prop = properties.getProperty(item);
		System.out.println(item + " = " + prop);
		return prop;
	}
}
