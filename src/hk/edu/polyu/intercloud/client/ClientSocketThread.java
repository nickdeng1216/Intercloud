package hk.edu.polyu.intercloud.client;

import hk.edu.polyu.intercloud.api.CheckStatusAPI;
import hk.edu.polyu.intercloud.common.Common;
import hk.edu.polyu.intercloud.util.LogUtil;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;

import org.json.JSONException;
import org.json.JSONObject;

public class ClientSocketThread extends Thread {
    Socket socket;
    DataInputStream in;

    /**
     * <b>ServerThread constructor</b>
     *
     * @param socket     - Client socket accepted by server.
     * @param threadName - Sever name.
     */
    public ClientSocketThread(Socket socket) {
        if (null == socket) {
            return;
        }
        this.socket = socket;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void run() {
        if (null == socket) {
            LogUtil.logError("Socket is null");
            return;
        }
        try {
            in = new DataInputStream(socket.getInputStream());
            while (true) {
                if (socket.isClosed()) {
                    return;
                }
                String jsonMessage = "";
                try {
                    while (in.available() <= 0) {
                        Thread.sleep(50);
                        continue;
                    }
                    jsonMessage = in.readLine();
                    JSONObject j = new JSONObject(jsonMessage);
                    System.out.println("$ RECV from "
                            + socket.getRemoteSocketAddress().toString() + " "
                            + j.toString(4));
                    LogUtil.logPerformance("Client Socket RECV", jsonMessage,
                            System.currentTimeMillis(), 0L);
                    String cls = j.getString("API");
                    String method = j.getString("Method");
                    if (cls.endsWith("CheckStatusAPI")
                            && method.equalsIgnoreCase("checkStatus")) {
                        // CheckStatusAPI: Call immediately, no need to queue.
                        // Send the status.
                        long rid = j.getLong("ID"); // Request ID
                        String status = new CheckStatusAPI().getStatus(rid);
                        PrintWriter out = new PrintWriter(
                                socket.getOutputStream(), true);
                        out.println(status);
                        if (status.startsWith("{") && status.endsWith("}")) {
                            System.out.println("$ SENT to "
                                    + socket.getRemoteSocketAddress()
                                    .toString() + " "
                                    + new JSONObject(status).toString(4));
                        } else {
                            System.out.println("$ SENT to "
                                    + socket.getRemoteSocketAddress()
                                    .toString() + " " + status);
                        }
                        socket.close();
                    } else if ((cls.endsWith("ObjectStorageAPI") && Common.my_service_providers
                            .containsKey("ObjectStorage"))
                            || (cls.endsWith("VirtualMachineAPI") && Common.my_service_providers
                            .containsKey("VM"))) {
                        // ObjectStorageAPI and VirtualMachineAPI:
                        // Gen and send a request ID, then queue.
                        long rid = System.nanoTime(); // Request ID
                        PrintWriter out = new PrintWriter(
                                socket.getOutputStream(), true);
                        out.println(rid);
                        System.out.println("$ SENT to "
                                + socket.getRemoteSocketAddress().toString()
                                + " " + rid);
                        j.put("ID", rid);
                        Common.clientQ.offer(j);
                        socket.close();    /** this is the reason why we cannot get response from the server to the mobile,
                                                because the socket is closed. Nick 14:50 3rd, Mar, 2019 */
                    }
                    // add a new API to request to the gateway directly. Nick 15:05 3rd, Mar, 2019
                    else if (cls.endsWith("ObjectRequestAPI")) {
                        long rid = System.nanoTime(); // Request ID
                        PrintWriter out = new PrintWriter(
                                socket.getOutputStream(), true);
                        out.println(rid);
                        System.out.println("$ SENT to "
                                + socket.getRemoteSocketAddress().toString()
                                + " " + rid);
                        j.put("ID", rid);
                        Common.clientQ.offer(j);
                    } else {
                        // No such API or service not provided:
                        String error = "Error: No such API or service not provided.";
                        PrintWriter out = new PrintWriter(
                                socket.getOutputStream(), true);
                        out.println(error);
                        System.out.println("$ SENT to "
                                + socket.getRemoteSocketAddress().toString()
                                + " " + error);
                        socket.close();
                    }
                } catch (JSONException e) {
                    LogUtil.logException(e);
                } catch (SocketException e) {
                    LogUtil.logException(e);
                    return;
                } catch (IOException e) {
                    LogUtil.logException(e);
                }
            }
        } catch (Exception e) {
            LogUtil.logException(e);
            return;
        }
    }
}
