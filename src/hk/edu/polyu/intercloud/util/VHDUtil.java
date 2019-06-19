package hk.edu.polyu.intercloud.util;

import hk.edu.polyu.intercloud.exceptions.IntercloudException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class VHDUtil {

	private static final String STARV2V = System.getProperty("user.dir")
			+ "\\v2v\\StarV2Vc.exe";

	public static String convertHD(String oldPath, String newPath,
			String newFormat) throws IntercloudException {
		String command = "\"" + STARV2V + "\" if=\"" + oldPath + "\" of=\""
				+ newPath + "\" ot=" + newFormat;
		try {
			String output = runSingleCmd(command);
			if (!(output.contains("Done!"))) {
				throw new IntercloudException("Unable to convert " + oldPath
						+ System.getProperty("line.separator") + "Output: "
						+ output);
			}
			System.out.println(oldPath + " successfully converted...");
		} catch (IOException e) {
			throw new IntercloudException("Unable to convert " + oldPath
					+ System.getProperty("line.separator") + "IOException: "
					+ e.getStackTrace().toString());
		}
		return newPath;
	}

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
}
