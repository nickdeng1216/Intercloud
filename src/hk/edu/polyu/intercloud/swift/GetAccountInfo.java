package hk.edu.polyu.intercloud.swift;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;

/**
 * The class is used to read configuration file for opensatck account.
 * 
 * @author Kate.xie
 *
 */
public class GetAccountInfo {
	HashMap<String, Object> map = new HashMap<>();

	private static final String AWS_STORAGE_PROP_FILE = System
			.getProperty("user.dir") + "/" + "ops.properties";

	/**
	 * @return Return a map containing username,password,bucketname,key,path and
	 *         url.
	 * 
	 * @throws Exception
	 */
	public GetAccountInfo() throws Exception {
		System.out.println("This program will check the properties file.");
		Properties properties = new Properties();

		InputStream inputStream;
		try {

			inputStream = new FileInputStream(AWS_STORAGE_PROP_FILE);

			properties.load(inputStream);
		} catch (IOException e) {
			System.err.println("Sorry, the file " + "s"
					+ " is not found or unable to read.");
		}

		try {

			String username = properties.getProperty("Username");

			if (username.equals("") || username == null) {
				System.err
						.println("Please check if *username* is properly set.");
				System.exit(1);
			}
			String password = properties.getProperty("Password");
			if (password.equals("") || password == null) {
				System.err
						.println("Please check if *password* is properly set.");
				System.exit(1);
			}
			String url = properties.getProperty("Url");
			if (url.equals("") || url == null) {
				System.err.println("Please check if *url* is properly set.");
				System.exit(1);
			}
			String container = properties.getProperty("Container");
			if (container.equals("") || container == null) {
				System.err.println("Please check if *url* is properly set.");
				System.exit(1);
			}

			map.put("Username", username);
			map.put("Password", password);
			map.put("Url", url);
			map.put("Container", container);

		} catch (Exception e) {
			System.err
					.println("Please check if *ops.properties* are properly set.");
			System.exit(1);
		}

	}

	public String getValue(String key) {
		return (String) this.map.get(key);
	}

}
