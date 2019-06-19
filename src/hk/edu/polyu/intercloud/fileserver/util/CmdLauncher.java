package hk.edu.polyu.intercloud.fileserver.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class CmdLauncher {

	public static String runCommand(String command) throws IOException {

		Runtime rt = Runtime.getRuntime();
		Process proc = rt.exec(command);

		BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
		BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

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
}
