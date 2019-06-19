package hk.edu.polyu.intercloud.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.swing.JOptionPane;

import org.json.JSONArray;
import org.json.JSONObject;

public class ClientTesterLite {

	static final String listener = "127.0.0.1";
	static final int port = 2001;

	public static void main(String[] args) {
		String message = "Please input: 'put/get,object,target'"
				+ System.lineSeparator() + "For example,"
				+ System.lineSeparator() + "put,orange.jpg,iccp1.iccp.cf";
		while (true) {
			try {
				String input = JOptionPane.showInputDialog(null, message,
						"Client Tester Lite", JOptionPane.QUESTION_MESSAGE)
						+ ",";
				if (input.equals("null,")) {
					break;
				}
				String[] params = input.split(",");
				String response = request(params[0], params[1], params[2]);
				System.out.println("RESPONSE: " + response);
				Thread.sleep(5000);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	static String request(String method, String object, String targetCloud)
			throws UnknownHostException, IOException {
		JSONObject json = new JSONObject();
		json.put("API", "ObjectStorageAPI");
		json.put("Target", targetCloud);
		json.put("Method", method);
		String str = "";
		if (method.equals("put")) {
			str = "[\"" + object + "\",\"HTTP\",true,\"Public\",true]";
		} else if (method.equals("get")) {
			str = "[\"" + object + "\",\"HTTP\",true,\"Public\"]";
		} else {
			JOptionPane.showMessageDialog(null, "Only put/get are supported.");
			return "Only put/get are supported.";
		}
		JSONArray ja = new JSONArray(str);
		json.put("Parameters", ja);
		return send(json);
	}

	static String send(JSONObject json) throws UnknownHostException,
			IOException {
		String response = null, read = null;
		Socket socket = new Socket(listener, port);
		PrintWriter out = new PrintWriter(new BufferedWriter(
				new OutputStreamWriter(socket.getOutputStream())), true);
		BufferedReader in = new BufferedReader(new InputStreamReader(
				socket.getInputStream()));
		String msg = json.toString();
		out.println(msg);
		System.out.println("$ [-> " + listener + ":" + port + "] " + msg);
		while ((read = in.readLine()) != null) {
			System.out.println("$ [<- " + listener + ":" + port + "] " + read);
			response = read;
		}
		out.flush();
		out.close();
		in.close();
		socket.close();
		return response;
	}
}
