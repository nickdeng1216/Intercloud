package hk.edu.polyu.intercloud.communication;

import hk.edu.polyu.intercloud.common.Common;
import hk.edu.polyu.intercloud.exceptions.NoSuchDataException;
import hk.edu.polyu.intercloud.exceptions.ProtocolException;
import hk.edu.polyu.intercloud.model.protocol.Protocol;
import hk.edu.polyu.intercloud.util.DatabaseUtil;
import hk.edu.polyu.intercloud.util.LogUtil;
import hk.edu.polyu.intercloud.util.ProtocolUtil;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;

/**
 * <b>This class and whole package is used for communication among gateways</b>
 * 
 * @author harry
 * @since 0.1
 * @version 0.3
 */
public class Sockets {
	private Socket socket;
	private ServerSocket serversocket;
	private DataOutputStream out;

	/**
	 * <b>Socket Client Constructor</b>
	 * <p>
	 * <b>Example:</b>
	 * 
	 * <pre>
	 * 		&#64;code
	 * 		Sockets("158.168.22.101", "2001", "Cloud1");
	 * </pre>
	 * 
	 * @param ip
	 *            - Target IP
	 * @param port
	 *            - Target Port
	 * @param name
	 *            - Client Name who establishes connection
	 *            <p>
	 * 
	 * @see
	 * 
	 *      <pre>
	 * Server Constructor {@link #Sockets(int, String)}
	 * </pre>
	 */
	public Sockets(String ip, int port, String name) {
		try {
			socket = new Socket(ip, port);
			out = new DataOutputStream(socket.getOutputStream());
		} catch (UnknownHostException e) {
			LogUtil.logException(e);
		} catch (IOException e) {
			LogUtil.logException(e);
		}
	}

	/**
	 * <b>Used for client to send protocol</b>
	 * 
	 * @param msg
	 *            - Message should be sent
	 *            <p>
	 * @throws ProtocolException
	 * @throws ParseException
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 * @throws NoSuchDataException
	 */
	public void sendMessage(String msg) throws ProtocolException,
			ClassNotFoundException, SQLException, ParseException,
			NoSuchDataException {
		try {
			LogUtil.logPerformance("Socket SEND", socket
					.getRemoteSocketAddress().toString(), System
					.currentTimeMillis(), 0L);
			System.out.println(new Date() + " Socket sent:\n" + msg);
			out.writeUTF(msg);
			out.flush();
			out.close();
		} catch (IOException e) {
			LogUtil.logException(e);
		}
		Protocol protocol = ProtocolUtil.parseProtocolType(msg);
		if (protocol.getRequestInformation() != null
				&& protocol.getResponseInformation() == null) {
			DatabaseUtil.insertReqTrack(protocol.getId(), protocol
					.getGeneralInformation().getTo(), msg);
		} else {
			DatabaseUtil.insertResultTrack(protocol.getId(), protocol
					.getGeneralInformation().getFrom(), msg);
		}

	}

	/**
	 * <b>Start socket server in gateway. All server action is defined in
	 * ServerThread class. Refer to {@link ServerThread}.</b>
	 * 
	 * @param port
	 *            - Server will listen to this port.
	 * @param name
	 *            - Server name.
	 *            <p>
	 * @throws IOException
	 */
	public Sockets(int port, String name) throws IOException {
		System.out.println("===============Start " + name
				+ " Server===============");
		serversocket = new ServerSocket(port);
		new MessageProcessor("GatewayMessageProcessor").start();
		while (true) {
			Socket socket = serversocket.accept();
			ServerThread serverThread = new ServerThread(socket, name);
			serverThread.start();
		}
	}

	/**
	 * <b>Gateway starts a new thread for server listening service.</b>
	 */
	class ServerThread extends Thread {
		Socket socket;
		String servername, vendor;
		DataInputStream in;
		DataOutputStream out;
		boolean runFlag = true;

		/**
		 * <b>ServerThread constructor</b>
		 * 
		 * @param socket
		 *            - Client socket accepted by server.
		 * @param threadName
		 *            - Sever name.
		 * @param vendor
		 *            - Hypervisor type of cloud.
		 */
		public ServerThread(Socket socket, String threadName) {
			super(threadName);
			if (null == socket) {
				runFlag = false;
				return;
			}
			this.socket = socket;
		}

		/**
		 * <b>Every received protocol comes here to be processed. Protocol is
		 * transfered to String type.
		 * <p>
		 * If Exchange receives a protocol, Exchange retransmit it to other
		 * gateways, else cloud gateway generates response and replies. If
		 * gateway who initialize request receives response contains same id
		 * with sent request, the session is finished. And both request and
		 * response will be logged into file.
		 * <p>
		 * </b>
		 */
		@Override
		public void run() {
			if (null == socket) {
				System.out.println("socket is null");
				return;
			}
			try {
				in = new DataInputStream(socket.getInputStream());
				while (runFlag) {
					if (socket.isClosed()) {
						System.out.println("socket is closed");
						return;
					}
					try {
						String msg = null;
						try {
							msg = in.readUTF();
						} catch (EOFException e) {

						}
						// ************************************************
						// ****************** Attention *******************
						// ************************************************
						LogUtil.logPerformance("Socket RECV", socket
								.getRemoteSocketAddress().toString(), System
								.currentTimeMillis(), 0L);
						System.out.println(new Date() + " Socket received:\n"
								+ msg);
						boolean offered = false;
						for (String s : Common.msgSet) {
							if (msg.contains(s)) {
								Common.gatewayQ_light.offer(msg);
								offered = true;
								break;
							}
						}
						if (!offered) {
							Common.gatewayQ.offer(msg);
						}
						exit();
					} catch (SocketException e) {
						LogUtil.logException(e);
						return;
					} catch (IOException e) {
						LogUtil.logException(e);
					}
				}
			} catch (IOException e) {
				LogUtil.logException(e);
				return;
			} catch (Exception e) {
				LogUtil.logException(e);
				return;
			}
		}

		public void exit() {
			runFlag = false;
		}
	}
}
