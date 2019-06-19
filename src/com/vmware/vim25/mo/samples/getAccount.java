package com.vmware.vim25.mo.samples;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;

/**
 * The class reads the VM property file
 * 
 * @author Kate.Xie
 *
 */

public class getAccount {
	HashMap<String, Object> map = new HashMap();

	/**
	 * 
	 * @param vmname
	 *            VM's name
	 * @return return a map containing Username and password.
	 * @throws Exception
	 */
	public getAccount(String vmname) throws Exception {
		final String DOCKER_PROP_FILE = System.getProperty("user.dir") + "/"
				+ "vm_" + vmname + ".properties";

		System.out.println(DOCKER_PROP_FILE
				+ "This program will check the vm properties file.");
		Properties properties = new Properties();

		InputStream inputStream;
		try {

			inputStream = new FileInputStream(DOCKER_PROP_FILE);

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
			System.out.print(password);
			if (password.equals("") || password == null) {
				System.err
						.println("Please check if *password* is properly set.");
				System.exit(1);
			}

			map.put("Username", username);
			map.put("Password", password);

		} catch (Exception e) {
			System.err
					.println("Please check if *vm.properties* are properly set.");
			System.exit(1);
		}
	}

	public String getValue(String key) {
		return (String) this.map.get(key);
	}

	public static void main(String args[]) throws Exception {
		getAccount a = new getAccount("docker_test1");

	}

}
