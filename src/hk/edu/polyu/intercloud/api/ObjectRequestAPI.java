package hk.edu.polyu.intercloud.api;

import hk.edu.polyu.intercloud.aws.CheckObject;
import hk.edu.polyu.intercloud.azurestorage.AzureStorageIntercloud;
import hk.edu.polyu.intercloud.command.objectstorage.ListObject;
import hk.edu.polyu.intercloud.common.Common;
import hk.edu.polyu.intercloud.communication.Sockets;
import hk.edu.polyu.intercloud.ctlstorage.CtlStorageForIntercloud;
import hk.edu.polyu.intercloud.exceptions.*;
import hk.edu.polyu.intercloud.gstorage.GoogleStorageIntercloud;
import hk.edu.polyu.intercloud.minio.MinioForIntercloud;
import hk.edu.polyu.intercloud.model.protocol.*;
import hk.edu.polyu.intercloud.swift.SwiftForIntercloud;
import hk.edu.polyu.intercloud.util.DatabaseUtil;
import hk.edu.polyu.intercloud.util.LogUtil;
import hk.edu.polyu.intercloud.util.PropertiesReader;
import hk.edu.polyu.intercloud.util.ProtocolUtil;

import java.io.IOException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

/**
 * The API for request to the gateway directly
 *
 * @author Nick
 * @date 14:59 3rd, Mar, 2019
 */
public class ObjectRequestAPI {

    /**
     * The target Cloud
     */
    private String cloud;

    /**
     * Construct an Object Storage API class object.
     *
     * @param cloud
     *            The name of the target cloud
     * @throws AuthenticationAPIException
     * @throws ObjectStorageAPIException
     */
    public ObjectRequestAPI(String cloud) throws AuthenticationAPIException,
            ObjectStorageAPIException {
        this.cloud = cloud;
        AuthenticationAPI aAPI = new AuthenticationAPI();
        aAPI.checkAuth(cloud);
        checkMyService();
    }
    public String list(boolean protocolSecurity)
            throws ObjectStorageAPIException {
        String protocolid = ProtocolUtil.generateID();

        try {
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = new Date();
            String[] dateTime = dateFormat.format(date).split("\\s+");

            GeneralInformation generalInformation = new GeneralInformation(Common.my_name, cloud, dateTime[0], dateTime[1]);

            RequestInformation requestInformation = new RequestInformation();
            requestInformation.setCommand("ListObject");
            requestInformation.setService("ObjectStorage");
            ResponseInformation responseInformation = null;

            ListObject listobject = new ListObject();
            HashMap<String, String> map = new HashMap<>();

            AdditionalInformation additionalInformation = listobject.pre_execute(null, map,	Common.my_service_providers.get("ObjectStorage"));

            Protocol protocol = new Protocol(Common.ICCP_VER, protocolid, generalInformation, requestInformation,
                    responseInformation, additionalInformation, null);

            String pro_String = ProtocolUtil.generateRequest(protocol, protocolSecurity);

            String ip = Common.my_friends.get(cloud).getIp();
            int port = Common.GW_PORT;

            /**
             * New Sockets client to send protocol.
             */
            Sockets socket = new Sockets(ip, port, Common.my_name);

            if (requestInformation != null && responseInformation == null) {
                socket.sendMessage(pro_String);
            }

            return protocolid;

        } catch (Exception e) {
            throw new ObjectStorageAPIException(e.getMessage(), e);
        }
    }

    private void checkMyService() throws ObjectStorageAPIException {
        if (!Common.my_service_providers.containsKey("ObjectStorage")) {
            throw new ObjectStorageAPIException("ObjectStorage"
                    + " is not provided by " + Common.my_name);
        }
    }
    public String put() {
        return "";
    }

    public String get() {
        return "";
    }

    public String delete() {
        return "";
    }
}