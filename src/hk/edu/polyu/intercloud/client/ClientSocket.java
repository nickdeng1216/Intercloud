package hk.edu.polyu.intercloud.client;

import hk.edu.polyu.intercloud.exceptions.ClientSocketException;
import hk.edu.polyu.intercloud.util.LogUtil;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Date;

/**
 * This class communicates with PHP
 * 
 * @author Priere
 *
 */
public class ClientSocket {

	public static final String RECEIVER = "RECEIVER";
	public static final String SENDER = "SENDER";
	private Socket socket;
	private ServerSocket serversocket;
	private DataOutputStream out;

	// As a sender
	public ClientSocket(String ip, int port, String type)
			throws ClientSocketException {
		if (type.equals(ClientSocket.RECEIVER)) {
			try {
				serversocket = new ServerSocket(port);
				while (true) {
					Socket socket;
					socket = serversocket.accept();
					new Thread(new ClientSocketThread(socket), socket
							.getRemoteSocketAddress().toString()).start();
				}
			} catch (IOException e) {
				throw new ClientSocketException(e.getMessage(), e);
			}
		} else if (type.equals(ClientSocket.SENDER)) {
			try {
				socket = new Socket(ip, port);
				out = new DataOutputStream(socket.getOutputStream());
			} catch (UnknownHostException e) {
				throw new ClientSocketException(e.getMessage(), e);
			} catch (IOException e) {
				throw new ClientSocketException(e.getMessage(), e);
			}
		} else {
			throw new ClientSocketException("No such type of ClientSocket.");
		}
	}

	// As a sender to send messages
	public void sendMessage(String msg) {
		System.out.println("$$$ JSON to PHP $$$ Send time: " + new Date()
				+ " Message:\n" + msg);
		try {
			out.writeUTF(msg);
			out.flush();
		} catch (IOException e) {
			LogUtil.logException(e);
		}
	}

}
