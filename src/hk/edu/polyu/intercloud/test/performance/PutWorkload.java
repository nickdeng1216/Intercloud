package hk.edu.polyu.intercloud.test.performance;

import hk.edu.polyu.intercloud.api.ObjectStorageAPI;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import org.json.JSONArray;
import org.json.JSONObject;

public class PutWorkload {

	static final String WORKING = "iccp1.iccp.cf";
	static final String OBJECT = "1GB";
	static final String TARGET = "iccp2.iccp.cf";
	static final int DUPLICATES = 10;

	public static void main(String[] args) throws InterruptedException,
			UnknownHostException, IOException {
		for (int i = 0; i < DUPLICATES; i++) {
			Socket socket = new Socket(WORKING, 2001);
			PrintWriter out = new PrintWriter(new BufferedWriter(
					new OutputStreamWriter(socket.getOutputStream())), true);
			String objectName = OBJECT + "." + i + ".zip";
			System.out.println("\n_" + objectName);
			JSONObject j = new JSONObject();
			j.put("API", "ObjectStorageAPI");
			j.put("Target", TARGET);
			j.put("Method", "put");
			JSONArray ja = new JSONArray();
			ja.put(objectName);
			ja.put("HTTPS");
			ja.put(true);
			ja.put(ObjectStorageAPI.DATA_SECURITY.PRIVATE);
			ja.put(false);
			j.put("Parameters", ja);
			String msg = j.toString();
			out.print(msg + "\n");
			out.flush();
			out.close();
			System.out.println("SENT: " + msg);
			Thread.sleep(10);
		}
	}

}
