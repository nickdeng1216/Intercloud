package hk.edu.polyu.intercloud.aws;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;

/**
 * The class is used to read configuration file for Amazon account.
 * 
 * @author Kate.xie
 *
 */
public class GetAccountInfo {
	HashMap<String, Object> map = new HashMap<>();

	private static final String AWS_STORAGE_PROP_FILE = System
			.getProperty("user.dir") + "/" + "aws.properties";

	/**
	 * @return A map containing username, password, bucketname, key, path and
	 *         url.
	 * 
	 * @throws Exception
	 */
	public GetAccountInfo() throws Exception {
		System.out.println("Checking the AWS properties file.");
		Properties properties = new Properties();

		InputStream inputStream;
		try {
			inputStream = new FileInputStream(AWS_STORAGE_PROP_FILE);
			properties.load(inputStream);
		} catch (IOException e) {
			throw new IOException("Sorry, the file " + AWS_STORAGE_PROP_FILE
					+ " is not found or unable to read.");
		}

		try {
			String username = properties.getProperty("Username");
			if (username.equals("") || username == null) {
				throw new Exception(
						"Please check if *username* is properly set.");
			}
			String password = properties.getProperty("Password");
			if (password.equals("") || password == null) {
				throw new Exception(
						"Please check if *password* is properly set.");
			}
			String bucketname = properties.getProperty("Bucketname");
			if (bucketname.equals("") || bucketname == null) {
				throw new Exception(
						"Please check if *bucketname* is properly set.");
			}

			String url = properties.getProperty("Url");
			if (url.equals("") || url == null) {
				throw new Exception("Please check if *Url* is properly set.");
			}

			map.put("Username", username);
			map.put("Password", password);
			map.put("Bucketname", bucketname);
			map.put("Url", url);

		} catch (Exception e) {
			throw new Exception(
					"Please check if *aws.properties* are properly set.");
		}

	}

	public String getValue(String key) {
		return (String) this.map.get(key);
	}

}
