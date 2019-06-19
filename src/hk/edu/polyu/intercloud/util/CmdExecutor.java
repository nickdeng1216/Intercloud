package hk.edu.polyu.intercloud.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CmdExecutor {

	public static String runSingleCmd(String command) throws IOException {
		Runtime rt = Runtime.getRuntime();
		Process proc = rt.exec(command);
		System.out.println(command);
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
		return s;
	}

	public static String runSinglePsCmd(String command) throws IOException {
		Runtime rt = Runtime.getRuntime();
		command = "powershell -Command \"" + command + "\"";
		Process proc = rt.exec(command);
		System.out.println(command);
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
		return s;
	}

	public static List<String> runBatchCmd(List<String> commands)
			throws IOException {
		PrintWriter writer = new PrintWriter("clirun.cmd", "UTF-8");
		for (String command : commands) {
			writer.println(command);
			System.out.println(command);
		}
		writer.close();
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
		// System.out.println("********** Output **********");
		// output.stream().forEach(System.out::println);
		// System.out.println("********** E n d  **********");
		return output;
	}

}
