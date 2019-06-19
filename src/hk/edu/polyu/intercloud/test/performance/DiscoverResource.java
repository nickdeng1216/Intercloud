package hk.edu.polyu.intercloud.test.performance;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;

import javax.swing.JOptionPane;

import org.json.JSONArray;
import org.json.JSONObject;

public class DiscoverResource {
	static final String[] TARGETCLOUD = { "c3.e1.r1.iccp.us" };
	static final double[] ARRIVALRATE = { 1.0, 1.0, 1.0, 1.0, 1.0 };

	static JSONObject[] clientRequests;

	static ArrayList<int[]> notStartRequestList = new ArrayList<int[]>();

	public static void main(String[] args) {
		clientRequests = initialJSONObject();

		double startTimeOfLastRequest = 1.0;
		int requestIDCount = 1000001;
		for (int i = 0; i < TARGETCLOUD.length; i++) {
			double sum = 0.0;
			while (sum < startTimeOfLastRequest) {
				sum += getExponentialRandom(1.0 / ARRIVALRATE[i]);
				int[] row = { requestIDCount, i,
						new Random().nextInt(clientRequests.length),
						(int) (sum * 1000) };
				if (notStartRequestList.size() == 0) {
					notStartRequestList.add(row);
				} else {
					for (int j = 0; j < notStartRequestList.size(); j++) {
						if (row[3] < notStartRequestList.get(j)[3]) {
							notStartRequestList.add(j, row);
							break;
						} else if (j == notStartRequestList.size() - 1) {
							notStartRequestList.add(row);
							break;
						}
					}
				}
				requestIDCount++;
			}
		}

		for (int[] temp : notStartRequestList) {
			System.out.println(temp[0] + " " + temp[1] + " " + temp[2] + " "
					+ temp[3]);
		}

		String[] options = new String[] { "Start" };
		int o = JOptionPane.showOptionDialog(null, "Start Experiment?",
				"Options", JOptionPane.DEFAULT_OPTION,
				JOptionPane.PLAIN_MESSAGE, null, options, options[0]);

		/***** Start experiment ******/
		long startingTime = System.currentTimeMillis();
		System.out.println("Start Time: " + startingTime);

		try {
			int counter = 0;
			while (counter < notStartRequestList.size()) {
				long currentTime = System.currentTimeMillis();
				if (notStartRequestList.get(counter)[3] + startingTime <= currentTime) {
					int[] row = notStartRequestList.get(counter);
					Socket socket = new Socket(TARGETCLOUD[row[1]], 2001);
					PrintWriter out = new PrintWriter(new BufferedWriter(
							new OutputStreamWriter(socket.getOutputStream())),
							true);
					JSONObject j = new JSONObject(
							clientRequests[row[2]].toString());
					JSONArray parameters = (JSONArray) j.remove("Parameters");
					parameters.put(row[0]);
					j.put("Parameters", parameters);
					String msg = j.toString();

					out.print(msg + "\n");
					out.flush();
					socket.close();
					System.out.println(System.currentTimeMillis() + " SENT: "
							+ msg);
					counter++;
				}
				Thread.sleep(5);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// SendJSONThread t = new SendJSONThread(12345678, 2, 0);
		// t.run();

		// String toCloud = "c3.e1.r1.iccp.us";
		// Socket socket = new Socket(toCloud, 2001);
		// PrintWriter out = new PrintWriter(new BufferedWriter(
		// new OutputStreamWriter(socket.getOutputStream())), true);
		// String msg = clientRequests[0].toString();
		// out.print(msg + "\n");
		// out.flush();
		// out.close();
		// System.out.println("SENT: " + msg);
	}

	public static JSONObject[] initialJSONObject() {
		JSONObject[] clientRequests = new JSONObject[1];

		JSONObject j = new JSONObject();
		j.put("API", "InquireResourceAPI");
		j.put("Target", "");
		j.put("Method", "discoverResource");
		JSONArray parameters = new JSONArray();
		parameters.put("VM"); // service
		JSONObject requirements = new JSONObject(); // requirements
		requirements.put("CPU", "2");
		requirements.put("Memory", "4GB");
		requirements.put("Disk", "10GB");
		requirements.put("Geolocation", "HK");
		parameters.put(requirements);
		parameters.put(true); // protocol security
		// parameters.put(12345678); // client request id
		j.put("Parameters", parameters);
		clientRequests[0] = j;
		return clientRequests;
	}

	public static double getExponentialRandom(double p) {
		Random r = new Random();
		// return -(Math.log(r.nextDouble()) / p);
		return -p * Math.log(r.nextDouble()); // now
	}

	public static class SendJSONThread implements Runnable {
		private int clientRequestId;
		private int toCloudIndex;
		private int jsonIndex;

		public SendJSONThread(int clientRequestId, int toCloudIndex,
				int jsonIndex) {
			this.clientRequestId = clientRequestId;
			this.toCloudIndex = toCloudIndex;
			this.jsonIndex = jsonIndex;
		}

		@Override
		public void run() {
			JSONObject j = new JSONObject(clientRequests[jsonIndex].toString());
			JSONArray parameters = (JSONArray) j.remove("Parameters");
			parameters.put(clientRequestId);
			j.put("Parameters", parameters);
			String msg = j.toString();
			// outs[toCloudIndex].write(msg + "\n");
			// outs[toCloudIndex].flush();
			System.out.println(System.currentTimeMillis() + " SENT: " + msg);
		}
	}
}
