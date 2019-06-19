package com.vmware.vim25.mo.samples.ovf;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;

/**
 * The class reads sPhere configuration file.
 * 
 * @author Kate.Xie
 *
 */
public class GetAccountInfo {

	@SuppressWarnings("unchecked")
	HashMap<String, Object> map = new HashMap();

	public GetAccountInfo(String property_file) throws Exception {
		String SPHERE_STORAGE_PROP_FILE = System.getProperty("user.dir") + "/"
				+ property_file;
		System.out.println("This program will check the properties file.");
		Properties properties = new Properties();

		InputStream inputStream;
		try {

			inputStream = new FileInputStream(SPHERE_STORAGE_PROP_FILE);

			properties.load(inputStream);
		} catch (IOException e) {
			System.err.println("Sorry, the file " + "s"
					+ " is not found or unable to read.");
		}

		try {

			String url = properties.getProperty("Url");

			if (url.equals("") || url == null) {

				System.exit(1);
			}
			String password = properties.getProperty("Password");
			if (password.equals("") || password == null) {

				System.exit(1);
			}
			String username = properties.getProperty("Username");
			if (username.equals("") || username == null) {

				System.exit(1);
			}

			String hostip = properties.getProperty("Hostip");
			if (hostip.equals("") || hostip == null) {

				System.exit(1);
			}

			String datastore = properties.getProperty("Datastore");
			if (datastore.equals("") || datastore == null) {

				System.exit(1);
			}

			String diskmode = properties.getProperty("Diskmode");
			if (diskmode.equals("") || diskmode == null) {

				System.exit(1);
			}

			map.put("Username", username);
			map.put("Password", password);
			map.put("Url", url);
			map.put("Hostip", hostip);
			map.put("Datastore", datastore);
			map.put("Diskmode", diskmode);

		} catch (Exception e) {
			System.err
					.println("Please check if *vsphere.properties* are properly set.");
			System.exit(1);
		}

	}

	public String getValue(String key) {
		return (String) this.map.get(key);
	}

	public static void main(String[] args) throws Exception {
		GetAccountInfo a = new GetAccountInfo("vsphere.properties");

	}

}
