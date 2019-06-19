package com.vmware.vim25.mo.samples.ovf;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class VSpherecli {
	private String vSphereCLI = "C:\\share\\workspace\\Vijava\\vSphere_CLI";
	private String ip;
	private String username;
	private String password;
	private String commandPrefix;

	public VSpherecli() throws Exception {

		GetAccountInfo sphere = new GetAccountInfo("vsphere.properties");

		String username = sphere.getValue("Username");
		String password = sphere.getValue("Password");
		String ip = sphere.getValue("Hostip");

		commandPrefix = "vifs.pl --server " + ip + " --username " + username
				+ " --password %" + password;
	}

	private List<String> runCommand(String command) throws IOException {

		PrintWriter writer = new PrintWriter("clirun.cmd", "UTF-8");
		// TODO Set title
		writer.println("@echo off");
		writer.println("set vclipath=" + vSphereCLI + "\\bin");
		writer.println("set perlpath=" + vSphereCLI + "\\perl\\bin");
		writer.println("set path=%path%;%vclipath%;%perlpath%;");
		writer.println(command);
		writer.close();

		System.out.println("@echo off");
		System.out.println("set vclipath=" + vSphereCLI + "\\bin");
		System.out.println("set perlpath=" + vSphereCLI + "\\perl\\bin");
		System.out.println("set path=%path%;%vclipath%;%perlpath%;");
		System.out.println(command);

		Runtime rt = Runtime.getRuntime();
		Process proc = rt.exec("clirun.cmd");

		BufferedReader stdInput = new BufferedReader(new InputStreamReader(
				proc.getInputStream()));
		BufferedReader stdError = new BufferedReader(new InputStreamReader(
				proc.getErrorStream()));

		// Read the output from the command
		String s = "";
		int i;
		while ((i = stdInput.read()) != -1) {
			s += (char) i;
		}

		// Read any errors from the attempted command
		while ((i = stdError.read()) != -1) {
			s += (char) i;
		}

		List<String> output = new ArrayList<String>(Arrays.asList(s
				.split(System.getProperty("line.separator"))));
		System.out.println(output);
		return output;
	}

	public List<String> mountdisk() throws IOException {
		String command = commandPrefix;
		List<String> output = runCommand(command);
		Iterator<String> i = output.iterator();
		while (i.hasNext()) {
			String s = i.next();
			if (s.equals("Content Listing") || s.equals("---------------")
					|| s.equals("") || s == null) {
				i.remove();
			}
		}
		return output;
	}

	public List<String> uploadfile(String path, String remotepath)
			throws IOException {
		String command = commandPrefix + " --put \"" + path + "\" \""
				+ remotepath + "\"";
		List<String> output = runCommand(command);
		Iterator<String> i = output.iterator();
		while (i.hasNext()) {
			String s = i.next();
			if (s.equals("Content Listing") || s.equals("---------------")
					|| s.equals("") || s == null) {
				i.remove();
			}
		}
		return output;
	}

	public List<String> movefile(String path, String remvetepath)
			throws IOException {
		String command = commandPrefix + " -m \"" + path + "\" \""
				+ remvetepath + "\"";
		List<String> output = runCommand(command);
		Iterator<String> i = output.iterator();
		while (i.hasNext()) {
			String s = i.next();
			if (s.equals("Content Listing") || s.equals("---------------")
					|| s.equals("") || s == null) {
				i.remove();
			}
		}
		return output;
	}

	public static void main(String[] args) throws Exception {
		VSpherecli vm = new VSpherecli();
		try {
			// vm.listVMStorages();
			vm.uploadfile("c:\\SampleXP.vmdk", "[ESXi02-DS01] SampleXP.vmdk");
			vm.uploadfile("c:\\SampleXP-flat.vmdk",
					"[ESXi02-DS01] SampleXP-flat.vmdk");
		} catch (Exception e) {

			e.printStackTrace();
		}
	}
}
