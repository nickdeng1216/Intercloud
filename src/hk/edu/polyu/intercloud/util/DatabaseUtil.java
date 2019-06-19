package hk.edu.polyu.intercloud.util;

import hk.edu.polyu.intercloud.common.Common;
import hk.edu.polyu.intercloud.exceptions.NoSuchDataException;
import hk.edu.polyu.intercloud.model.cloud.Cloud;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The class calls database and related methods for the gateway.
 * 
 * @author Kate.xie
 *
 */
public class DatabaseUtil {
	public static final String status_1 = "sending";
	public static final String status_2 = "success";
	public static final String status_3 = "failed";

	public static final String DATA_SEC_PUBLIC = "Public";
	public static final String DATA_SEC_SHARED = "Shared";
	public static final String DATA_SEC_PRIVATE = "Private";

	/**
	 * Make a connection with the database.
	 * 
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 */
	private static void getConnection() throws SQLException,
			ClassNotFoundException {
		if (Common.con == null || Common.con.isClosed()) {
			Class.forName("oracle.jdbc.driver.OracleDriver");
			Common.con = DriverManager.getConnection(
					"jdbc:oracle:thin:@localhost:1521:xe", Common.DB_USER,
					Common.DB_PASS);
		}
	}

	// ---------------------------
	// Security
	// ---------------------------

	public static boolean checkDuplicateOthersAll(String digest)
			throws SQLException, ClassNotFoundException {
		getConnection();
		boolean flag;
		String count = null;
		String sql = "select count(*) as count from OS_OTHERS_OBJECTS where DIGEST=\'"
				+ digest + "\' ";
		Statement stat = null;
		ResultSet rs = null;
		stat = Common.con.createStatement();
		rs = stat.executeQuery(sql);
		while (rs.next()) {
			count = rs.getString("count");
		}
		if (count.equals("0")) {
			flag = false;
		} else {
			flag = true;
		}
		stat.close();
		rs.close();
		return flag;
	}

	public static String getDuplicateNamesOthersAll(String digest)
			throws SQLException, ClassNotFoundException {
		getConnection();
		String object_name = null;
		String sql = "select object_name from OS_OTHERS_OBJECTS where DIGEST=\'"
				+ digest + "\' ";
		Statement stat = null;
		ResultSet rs = null;
		stat = Common.con.createStatement();
		rs = stat.executeQuery(sql);
		while (rs.next()) {
			object_name = rs.getString("object_name");
		}
		stat.close();
		rs.close();
		return object_name;
	}

	public static boolean checkDuplicateOthers(String digest, String owner_cloud)
			throws SQLException, ClassNotFoundException {
		getConnection();
		boolean flag;
		String count = null;
		String sql = "select count(*) as count from OS_OTHERS_OBJECTS where DIGEST=\'"
				+ digest + "\' and OWNER_CLOUD= \'" + owner_cloud + "\'";
		Statement stat = null;
		ResultSet rs = null;
		stat = Common.con.createStatement();
		rs = stat.executeQuery(sql);
		while (rs.next()) {
			count = rs.getString("count");
		}
		if (count.equals("0")) {
			flag = false;
		} else {
			flag = true;
		}
		stat.close();
		rs.close();
		return flag;
	}

	public static int countDuplicateOthers(String digest, String owner_cloud)
			throws SQLException, ClassNotFoundException {
		getConnection();
		String count = null;
		String sql = "select count(*) as count from OS_OTHERS_OBJECTS where DIGEST=\'"
				+ digest + "\' and OWNER_CLOUD= \'" + owner_cloud + "\'";
		Statement stat = null;
		ResultSet rs = null;
		stat = Common.con.createStatement();
		rs = stat.executeQuery(sql);
		while (rs.next()) {
			count = rs.getString("count");
		}
		stat.close();
		rs.close();
		return Integer.parseInt(count);
	}

	public static int countDuplicateOthersAll(String digest)
			throws SQLException {
		String count = null;
		String sql = "select count(*) as count from OS_OTHERS_OBJECTS where DIGEST=\'"
				+ digest + "\'";
		Statement stat = null;
		ResultSet rs = null;
		stat = Common.con.createStatement();
		rs = stat.executeQuery(sql);
		while (rs.next()) {
			count = rs.getString("count");
		}
		stat.close();
		rs.close();
		return Integer.parseInt(count);
	}

	public static void insertOthersObjectTable(String id, String object_name,
			String own_cloud, String owner_object_name, String path,
			String digest, String security_level) throws SQLException,
			ParseException, ClassNotFoundException {
		getConnection();
		String sql = "Insert into OS_OTHERS_OBJECTS (ID,OBJECT_NAME,OWNER_CLOUD,OWNER_OBJECT_NAME,OBJECT_PATH,DIGEST,DATA_SECURITY_LEVEL,CREATE_TIME) VALUES (?,?,?,?,?,?,?,SYSDATE)";
		int security_level_int = 0;
		if (security_level.equalsIgnoreCase(DATA_SEC_PRIVATE)) {
			security_level_int = 3;
		} else if (security_level.equalsIgnoreCase(DATA_SEC_SHARED)) {
			security_level_int = 2;
		} else if (security_level.equalsIgnoreCase(DATA_SEC_PUBLIC)) {
			security_level_int = 1;
		} else {
			security_level_int = 0;
		}
		PreparedStatement pst = Common.con.prepareStatement(sql);
		int idx = 1;
		pst.setString(idx++, id);
		pst.setString(idx++, object_name);
		pst.setString(idx++, own_cloud);
		pst.setString(idx++, owner_object_name);
		pst.setString(idx++, path);
		pst.setString(idx++, digest);
		pst.setInt(idx++, security_level_int);
		pst.executeUpdate();
		pst.close();
	}

	public static boolean checkDuplicateOwn(String digest, String storage_cloud)
			throws SQLException, ClassNotFoundException {
		getConnection();
		boolean flag;
		String count = null;
		String sql = "select count(*) as count from OS_OWN_OBJECTS where  DIGEST=\'"
				+ digest + "\' and STORAGE_CLOUD= \'" + storage_cloud + "\'";
		Statement stat = null;
		ResultSet rs = null;
		stat = Common.con.createStatement();
		rs = stat.executeQuery(sql);
		while (rs.next()) {
			count = rs.getString("count");
		}
		if (count.equals("0")) {
			flag = false;
		} else {
			flag = true;
		}
		stat.close();
		rs.close();
		return flag;
	}

	public static void insertOwnObjectTable(String id, String object_name,
			String storage_cloud, String storage_object_name, String digest,
			String security_level) throws SQLException, ParseException,
			ClassNotFoundException {
		getConnection();
		String sql = "Insert into OS_OWN_OBJECTS (ID,OBJECT_NAME,STORAGE_CLOUD,STORAGE_OBJECT_NAME,DIGEST,DATA_SECURITY_LEVEL,STATUS,CREATE_TIME) VALUES (?,?,?,?,?,?,'sending',SYSDATE)";
		int security_level_int = 0;
		if (security_level.equalsIgnoreCase(DATA_SEC_PRIVATE)) {
			security_level_int = 3;
		} else if (security_level.equalsIgnoreCase(DATA_SEC_SHARED)) {
			security_level_int = 2;
		} else if (security_level.equalsIgnoreCase(DATA_SEC_PUBLIC)) {
			security_level_int = 1;
		} else {
			security_level_int = 0;
		}
		PreparedStatement pst = Common.con.prepareStatement(sql);
		int idx = 1;
		pst.setString(idx++, id);
		pst.setString(idx++, object_name);
		pst.setString(idx++, storage_cloud);
		pst.setString(idx++, storage_object_name);
		pst.setString(idx++, digest);
		pst.setInt(idx++, security_level_int);
		pst.executeUpdate();
		pst.close();
	}

	public static String getDataSecurityLevelOwn(String object_name,
			String storage_cloud) throws SQLException, ParseException,
			ClassNotFoundException {
		getConnection();
		String sql = "SELECT * FROM (SELECT DATA_SECURITY_LEVEL from OS_OWN_OBJECTS where OBJECT_NAME =\'"
				+ object_name
				+ "\' and STORAGE_CLOUD=\'"
				+ storage_cloud
				+ "\'order by LAST_UPDATE desc ) WHERE ROWNUM = 1";
		Statement stat = null;
		ResultSet rs = null;
		stat = Common.con.createStatement();
		rs = stat.executeQuery(sql);
		String level = null;
		while (rs.next()) {
			level = rs.getString("DATA_SECURITY_LEVEL");
		}
		if (level.contentEquals("3")) {
			level = DATA_SEC_PRIVATE;
		} else if (level.contentEquals("2")) {
			level = DATA_SEC_SHARED;
		} else if (level.contentEquals("1")) {
			level = DATA_SEC_PUBLIC;
		} else {
			level = "null";
		}
		stat.close();
		rs.close();
		return level;
	}

	public static String getDataSecurityLevelOthers(String object_name,
			String storage_cloud) throws SQLException, ParseException,
			ClassNotFoundException {
		getConnection();
		String sql = "SELECT DATA_SECURITY_LEVEL FROM (SELECT DATA_SECURITY_LEVEL from OS_OTHERS_OBJECTS where OWNER_OBJECT_NAME =\'"
				+ object_name
				+ "\' and OWNER_CLOUD=\'"
				+ storage_cloud
				+ "\'order by LAST_UPDATE desc ) WHERE ROWNUM = 1";
		Statement stat = null;
		ResultSet rs = null;
		stat = Common.con.createStatement();
		rs = stat.executeQuery(sql);
		String level = null;
		while (rs.next()) {
			level = rs.getString("DATA_SECURITY_LEVEL");
		}
		if (level.contentEquals("3")) {
			level = DATA_SEC_PRIVATE;
		} else if (level.contentEquals("2")) {
			level = DATA_SEC_SHARED;
		} else if (level.contentEquals("1")) {
			level = DATA_SEC_PUBLIC;
		} else {
			level = DATA_SEC_PUBLIC;
		}
		stat.close();
		rs.close();
		return level;
	}

	public static void updateOwnObjectTable(String object_name,
			String storage_cloud, String storage_object_name, String digest)
			throws SQLException, ParseException, ClassNotFoundException {
		getConnection();
		String sql = "Update  OS_OWN_OBJECTS set STORAGE_CLOUD=?,STORAGE_OBJECT_NAME=?,DIGEST=?,UPDATE_TIME=SYSDATE WHERE OBJECT_NAME=?";
		PreparedStatement pst = Common.con.prepareStatement(sql);
		int idx = 1;
		pst.setString(idx++, storage_cloud);
		pst.setString(idx++, storage_object_name);
		pst.setString(idx++, digest);
		pst.setString(idx++, object_name);
		pst.executeUpdate();
		pst.close();
	}

	public static void updateOthersObjectTable(String object_name,
			String storage_cloud, String storage_object_name, String path,
			String status, String digest) throws SQLException, ParseException,
			ClassNotFoundException {
		getConnection();
		String sql = "Update OS_OTHERS_OBJECTS set OWNER_CLOUD=?,OWNER_OBJECT_NAME=?,OBJECT_PATH=?,DIGEST=?,STATUS=?,UPDATE_TIME=SYSDATE WHERE OBJECT_NAME=?";
		PreparedStatement pst = Common.con.prepareStatement(sql);
		int idx = 1;
		pst.setString(idx++, storage_cloud);
		pst.setString(idx++, storage_object_name);
		pst.setString(idx++, path);
		pst.setString(idx++, digest);
		pst.setString(idx++, status);
		pst.setString(idx++, object_name);
		pst.executeUpdate();
		pst.close();
	}

	public static String findObject(String owner_object_name, String digest)
			throws SQLException, ParseException, ClassNotFoundException {
		getConnection();
		String sql = "Select OBJECT_NAME from OS_OTHERS_OBJECTS where DIGEST=\'"
				+ digest
				+ "\' and OWNER_OBJECT_NAME =\'"
				+ owner_object_name
				+ "\'";
		Statement stat = null;
		ResultSet rs = null;
		stat = Common.con.createStatement();
		rs = stat.executeQuery(sql);
		String object_name = null;
		while (rs.next()) {
			object_name = rs.getString("OBJECT_NAME");
		}
		stat.close();
		rs.close();
		return object_name;
	}

	public static String getDigestOwn(String file_name, String storage_cloud)
			throws SQLException, ParseException, ClassNotFoundException {
		getConnection();
		String sql = "SELECT * FROM (Select DIGEST from OS_OWN_OBJECTS where OBJECT_NAME=\'"
				+ file_name
				+ "\' and STORAGE_CLOUD= \'"
				+ storage_cloud
				+ "\' order by LAST_UPDATE desc ) WHERE ROWNUM = 1 ";
		Statement stat = null;
		ResultSet rs = null;
		stat = Common.con.createStatement();
		rs = stat.executeQuery(sql);
		String digest = null;
		while (rs.next()) {
			digest = rs.getString("DIGEST");
		}
		stat.close();
		rs.close();
		return digest;
	}

	public static void updateStatusOwn(String status, String id)
			throws SQLException, ParseException, ClassNotFoundException {
		getConnection();
		String sql = "Update OS_OWN_OBJECTS set STATUS=? WHERE ID=?";
		PreparedStatement pst = Common.con.prepareStatement(sql);
		int idx = 1;
		pst.setString(idx++, status);
		pst.setString(idx++, id);
		pst.executeUpdate();
		pst.close();
	}

	public static String getStatusOwn(String id) throws SQLException,
			ParseException, ClassNotFoundException {
		getConnection();
		String sql = "select status from OS_OWN_OBJECTS WHERE ID=\'" + id
				+ "\'";
		Statement stat = null;
		ResultSet rs = null;
		stat = Common.con.createStatement();
		rs = stat.executeQuery(sql);
		String status = null;
		while (rs.next()) {
			status = rs.getString("status");
		}
		stat.close();
		rs.close();
		return status;
	}

	public static void insertReqTrack(String id, String targetcloud,
			String request) throws SQLException, ParseException,
			ClassNotFoundException {
		getConnection();
		String sql = "Insert into REQUEST_TRACK (ID,TARGET_CLOUD,REQUEST) VALUES (?,?,?)";
		PreparedStatement pst = Common.con.prepareStatement(sql);
		int idx = 1;
		pst.setString(idx++, id);
		pst.setString(idx++, targetcloud);
		pst.setString(idx++, request);
		pst.executeUpdate();
		pst.close();

	}

	public static String getResultTrack(long id) throws SQLException,
			ParseException, ClassNotFoundException {
		getConnection();
		String sql = "select RESULT from RESULT_TRACK WHERE ID=\'" + id + "\'";
		Statement stat = null;
		ResultSet rs = null;
		stat = Common.con.createStatement();
		rs = stat.executeQuery(sql);
		String request = null;
		while (rs.next()) {
			request = rs.getString("RESULT");
		}
		stat.close();
		rs.close();
		return request;
	}

	public static String getReqTrack(long id) throws SQLException,
			ParseException, ClassNotFoundException {
		getConnection();
		String sql = "select request from  REQUEST_TRACK  WHERE ID=\'" + id
				+ "\'";
		Statement stat = null;
		ResultSet rs = null;
		stat = Common.con.createStatement();
		rs = stat.executeQuery(sql);
		String request = null;
		while (rs.next()) {
			request = rs.getString("request");
		}
		stat.close();
		rs.close();
		return request;
	}

	public static void insertResultTrack(String id, String targetcloud,
			String feedback) throws SQLException, ParseException,
			NoSuchDataException, ClassNotFoundException {
		String request = getReqTrack(Long.parseLong(id));
		if (request == null)
			throw new NoSuchDataException("No Such Data");
		else {
			String sql = "Insert into RESULT_TRACK (ID,TARGET_CLOUD,RESULT) VALUES (?,?,?)";
			PreparedStatement pst = Common.con.prepareStatement(sql);
			int idx = 1;
			pst.setString(idx++, id);
			pst.setString(idx++, targetcloud);
			pst.setString(idx++, feedback);
			pst.executeUpdate();
			pst.close();
		}
	}

	public static void updateLastGetAttemptOwn(String id) throws SQLException,
			ClassNotFoundException {
		getConnection();
		String sql = "Update OS_OWN_OBJECTS set LAST_GET_ATTEMPT=SYSDATE WHERE ID=?";
		PreparedStatement pst = Common.con.prepareStatement(sql);
		int idx = 1;
		pst.setString(idx++, id);
		pst.executeUpdate();
		pst.close();
	}

	public static String getLastGetAttemptOwn(String id) throws SQLException,
			ClassNotFoundException {
		getConnection();
		String sql = "select TO_CHAR (LAST_GET_ATTEMPT, 'MM-DD-YYYY HH24:MI:SS') as last from OS_OWN_OBJECTS where ID=\'"
				+ id + "\' ";
		Statement stat = null;
		ResultSet rs = null;
		stat = Common.con.createStatement();
		rs = stat.executeQuery(sql);
		String last_get_attempt = null;
		while (rs.next()) {
			last_get_attempt = rs.getString("last");
		}
		stat.close();
		rs.close();
		return last_get_attempt;
	}

	public static void updateLastGetAttemptOthers(String id)
			throws SQLException, ClassNotFoundException {
		getConnection();
		String sql = "Update OS_OTHERS_OBJECTS set LAST_GET_ATTEMPT=SYSDATE WHERE ID=?";
		PreparedStatement pst = Common.con.prepareStatement(sql);
		int idx = 1;
		pst.setString(idx++, id);
		pst.executeUpdate();
		pst.close();
	}

	public static String getLastGetAttemptOthers(String id)
			throws SQLException, ClassNotFoundException {
		getConnection();
		String sql = "select TO_CHAR (LAST_GET_ATTEMPT, 'MM-DD-YYYY HH24:MI:SS') as last from OS_OTHERS_OBJECTS where ID=\'"
				+ id + "\' ";
		Statement stat = null;
		ResultSet rs = null;
		stat = Common.con.createStatement();
		rs = stat.executeQuery(sql);
		String last_get_attempt = null;
		while (rs.next()) {
			last_get_attempt = rs.getString("last");
		}
		stat.close();
		rs.close();
		return last_get_attempt;
	}

	// -----------------------------
	// For challenge in authentication table
	// -----------------------------

	public static String getChallenge(String id, String cloud)
			throws SQLException, ParseException, ClassNotFoundException {
		getConnection();
		String sql = "select challenge from Authentication  WHERE ID=\'" + id
				+ "\' and CLOUD=\'" + cloud + "\'";
		Statement stat = null;
		ResultSet rs = null;
		stat = Common.con.createStatement();
		rs = stat.executeQuery(sql);
		String challenge = null;
		while (rs.next()) {
			challenge = rs.getString("challenge");
		}
		stat.close();
		rs.close();
		return challenge;
	}

	// insert authentication info in authentication table
	public static void insertAuthentication(String id, String challenge,
			String cloud) throws SQLException, ParseException,
			ClassNotFoundException {
		getConnection();
		String sql = "Insert into AUTHENTICATION (ID,CHALLENGE,CLOUD) VALUES (?,?,?)";
		PreparedStatement pst = Common.con.prepareStatement(sql);
		int idx = 1;
		pst.setString(idx++, id);
		pst.setString(idx++, challenge);
		pst.setString(idx++, cloud);
		pst.executeUpdate();
		pst.close();
	}

	// -----------------------------
	// Connected clouds
	// -----------------------------

	public static boolean checkAuth(String cloudname) throws SQLException,
			ClassNotFoundException {
		getConnection();
		String sql = "select AUTHENTICATED from CONNECTED_CLOUDS where CLOUD_NAME =\'"
				+ cloudname + "\' ";
		Statement stat = null;
		ResultSet rs = null;
		stat = Common.con.createStatement();
		rs = stat.executeQuery(sql);
		String authented = null;
		while (rs.next()) {
			authented = rs.getString("AUTHENTICATED");
		}
		boolean flag = false;
		if (authented.equals("0")) {
			flag = true;
		}
		stat.close();
		rs.close();
		return flag;
	}

	// update authentication in connected clouds
	public static void updateAuth(String cloudname, boolean auth)
			throws SQLException, ParseException, ClassNotFoundException {
		getConnection();
		int flag;
		if (auth) {
			flag = 0;
		} else {
			flag = 1;
		}
		String sql = "update CONNECTED_CLOUDS set AUTHENTICATED=" + flag
				+ " where CLOUD_NAME =? ";
		PreparedStatement pst = Common.con.prepareStatement(sql);
		int idx = 1;
		pst.setString(idx++, cloudname);
		pst.executeUpdate();
		pst.close();
	}

	public static Map<String, Cloud> getFriends() throws SQLException,
			ClassNotFoundException {
		getConnection();
		String sql = "select CLOUD_NAME,IP,ROLE,AUTHENTICATED from CONNECTED_CLOUDS";
		Statement stat = null;
		ResultSet rs = null;
		stat = Common.con.createStatement();
		rs = stat.executeQuery(sql);
		Map<String, Cloud> friend_list = new HashMap<>();
		while (rs.next()) {
			boolean authenticated = false;
			if (rs.getString("AUTHENTICATED").equalsIgnoreCase("0")) {
				authenticated = true;
			}
			Cloud cloud = new Cloud(rs.getString("IP"),
					rs.getString("CLOUD_NAME"), rs.getString("ROLE"),
					authenticated);
			friend_list.put(rs.getString("CLOUD_NAME"), cloud);
		}
		stat.close();
		rs.close();
		return friend_list;
	}

	// return all authenticated/unauthenticated items in CONNECTED_CLOUDS
	public static Map<String, Cloud> getFriends(boolean isAuth)
			throws SQLException, ClassNotFoundException {
		getConnection();
		int flag = 1;
		if (isAuth) {
			flag = 0;
		}
		String sql = "select CLOUD_NAME,IP,ROLE from CONNECTED_CLOUDS where AUTHENTICATED =\'"
				+ flag + "\' ";
		Statement stat = null;
		ResultSet rs = null;
		stat = Common.con.createStatement();
		rs = stat.executeQuery(sql);
		Map<String, Cloud> friend_list = new HashMap<>();
		while (rs.next()) {
			Cloud cloud = new Cloud(rs.getString("IP"),
					rs.getString("CLOUD_NAME"), rs.getString("ROLE"), isAuth);
			friend_list.put(rs.getString("CLOUD_NAME"), cloud);
		}
		stat.close();
		rs.close();
		return friend_list;
	}

	// add friends in connected _cloud table if exist updated.
	// 0 means authenticated, 1 otherwise;
	public static void addFriend(Cloud cloud) throws SQLException,
			ClassNotFoundException {
		getConnection();
		String sql = "select count(*) as count from CONNECTED_CLOUDS where CLOUD_NAME=\'"
				+ cloud.getName() + "\'";
		Statement stat = null;
		ResultSet rs = null;
		stat = Common.con.createStatement();
		rs = stat.executeQuery(sql);
		boolean flag = false;
		while (rs.next()) {
			if (rs.getString("count").equalsIgnoreCase("0")) {
				flag = false;
			} else {
				flag = true;
			}
		}
		if (!flag) {
			String sql_insert = "insert into CONNECTED_CLOUDS(IP,CLOUD_NAME,AUTHENTICATED,ROLE) values(?,?,1,?)";
			PreparedStatement pst = Common.con.prepareStatement(sql_insert);
			int idx = 1;
			pst.setString(idx++, cloud.getIp());
			pst.setString(idx++, cloud.getName());
			pst.setString(idx++, cloud.getRole());
			pst.executeUpdate();
			pst.close();
		} else {
			String sql_update = "update CONNECTED_CLOUDS set IP =?,Role=? where CLOUD_NAME=? ";
			PreparedStatement pst = Common.con.prepareStatement(sql_update);
			int idx = 1;
			pst.setString(idx++, cloud.getIp());
			pst.setString(idx++, cloud.getName());
			pst.setString(idx++, cloud.getRole());
			pst.executeUpdate();
			pst.close();
		}
		rs.close();
	}

	public static void addFriends(List<Cloud> clouds) throws SQLException,
			ClassNotFoundException {
		int i = 0;
		while (i < clouds.size()) {
			addFriend(clouds.get(i));
			i++;
		}
	}

	// Update LAST_CONNNECTED
	// Note that the date type is sql date
	public static void updateConnected(String cloudName) throws SQLException,
			ClassNotFoundException {
		getConnection();
		String sql = "update CONNECTED_CLOUDS set LAST_CONNECTED=SYSDATE where CLOUD_NAME =? ";
		PreparedStatement pst = Common.con.prepareStatement(sql);
		int idx = 1;
		pst.setString(idx++, cloudName);
		pst.executeUpdate();
		pst.close();
	}

	public static void deleteFriend(String cloudName) throws SQLException,
			ClassNotFoundException {
		getConnection();
		String sql = "delete from CONNECTED_CLOUDS where CLOUD_NAME =? ";
		PreparedStatement pst = Common.con.prepareStatement(sql);
		int idx = 1;
		pst.setString(idx++, cloudName);
		pst.executeUpdate();
		pst.close();
	}

	public static void deleteAllFriends() throws SQLException,
			ClassNotFoundException {
		getConnection();
		String sql = "delete from CONNECTED_CLOUDS ";
		PreparedStatement pst = Common.con.prepareStatement(sql);
		pst.executeUpdate();
		pst.close();
	}

	public static void updateOtherInfo(String role, String cloudName)
			throws SQLException, ClassNotFoundException {
		getConnection();
		String sql = "update CONNECTED_CLOUDS set Role=? where CLOUD_NAME =? ";
		PreparedStatement pst = Common.con.prepareStatement(sql);
		int idx = 1;
		pst.setString(idx++, role);
		pst.setString(idx++, cloudName);
		pst.executeUpdate();
		pst.close();
	}

	public static String getStorageNameOthers(String object, String cloudName)
			throws SQLException, ClassNotFoundException {
		getConnection();
		String sql = "Select OBJECT_NAME from OS_OTHERS_OBJECTS where OWNER_CLOUD=\'"
				+ cloudName + "\' and OWNER_OBJECT_NAME =\'" + object + "\' ";
		Statement stat = null;
		ResultSet rs = null;
		stat = Common.con.createStatement();
		rs = stat.executeQuery(sql);
		String objectname = null;
		while (rs.next()) {
			objectname = rs.getString("OBJECT_NAME");
		}
		stat.close();
		rs.close();
		return objectname;
	}

	public static long getPid(long id) throws SQLException,
			ClassNotFoundException {
		getConnection();
		String sql = "Select PID from ID_MAP where RID=\'" + id + "\'   ";
		Statement stat = null;
		ResultSet rs = null;
		stat = Common.con.createStatement();
		rs = stat.executeQuery(sql);
		long pid = -1;
		while (rs.next()) {
			pid = rs.getLong("PID");
		}
		stat.close();
		rs.close();
		return pid;
	}

	public static boolean checkDuplicateByCloud(String object_name,
			String owner_cloud) throws SQLException, ClassNotFoundException {
		getConnection();
		boolean flag;
		String count = null;
		String sql = "select count(*) as count from OS_OTHERS_OBJECTS where OWNER_OBJECT_NAME=\'"
				+ object_name + "\' and OWNER_CLOUD= \'" + owner_cloud + "\'";
		Statement stat = null;
		ResultSet rs = null;
		stat = Common.con.createStatement();
		rs = stat.executeQuery(sql);
		while (rs.next()) {
			count = rs.getString("count");
		}
		if (count.equals("0")) {
			flag = false;
		} else {
			flag = true;
		}
		stat.close();
		rs.close();
		return flag;
	}

	public static void insertId(long pid, long rid) throws SQLException,
			ClassNotFoundException {
		getConnection();
		String sql = "Insert into ID_MAP (PID,RID,STATUS) VALUES (?,?,?)";
		PreparedStatement pst = Common.con.prepareStatement(sql);
		int idx = 1;
		pst.setLong(idx++, pid);
		pst.setLong(idx++, rid);
		pst.setLong(idx++, 0);
		pst.executeUpdate();
		pst.close();
	}

	public static void deleteOthersObjectTable(String digest, String cloudName)
			throws SQLException, ClassNotFoundException {
		getConnection();
		String sql = "delete from OS_OTHERS_OBJECTS where DIGEST=? and OWNER_CLOUD=?";
		PreparedStatement pst = Common.con.prepareStatement(sql);
		pst.setString(1, digest);
		pst.setString(2, cloudName);
		pst.executeUpdate();
		pst.close();
	}

	// Triggered by housekeeper
	public static void insertConnectedCloud(String cloudname, Cloud cloud)
			throws SQLException, ClassNotFoundException {
		getConnection();
		String sql = "INSERT INTO CONNECTED_CLOUDS VALUE(CLOUD_NAME,IP,AUTHENTICATED,ROLE) VALUES(?,?,?,?)";
		PreparedStatement pst = Common.con.prepareStatement(sql);
		pst.setString(1, cloudname);
		pst.setString(2, cloud.getIp());
		String auth = "1";
		if (cloud.getAuth()) {
			auth = "0";
		}
		pst.setString(3, auth);
		pst.setString(4, cloud.getRole());
		pst.executeUpdate();
		pst.close();
	}

	public static void deleteOthersObjectTableByCloud(String objectname,
			String cloudName) throws SQLException, ClassNotFoundException {
		getConnection();
		String sql = "delete from OS_OTHERS_OBJECTS where OWNER_OBJECT_NAME=? and OWNER_CLOUD=?";
		PreparedStatement pst = Common.con.prepareStatement(sql);
		pst.setString(1, objectname);
		pst.setString(2, cloudName);
		pst.executeUpdate();
		pst.close();
	}

	// Used in housekeeping
	public static void clearConnectedClouds() throws SQLException,
			ClassNotFoundException {
		getConnection();
		String sql = "delete from CONNECTED_CLOUDS ";
		PreparedStatement pst = Common.con.prepareStatement(sql);
		pst.executeUpdate();
		pst.close();
	}

	public static List<String> getOldVersionObjectName(String objectname,
			String owner) throws SQLException, ClassNotFoundException {
		getConnection();
		String sql = "select object_name from OS_OTHERS_OBJECTS where  OWNER_OBJECT_NAME=\'"
				+ objectname + "\' and OWNER_CLOUD=\'" + owner + "\'";
		Statement stat = null;
		ResultSet rs = null;
		stat = Common.con.createStatement();
		rs = stat.executeQuery(sql);
		List<String> name = new ArrayList<>();
		while (rs.next()) {
			name.add(rs.getString("object_name"));
		}
		stat.close();
		rs.close();
		return name;
	}

	// Digest is the identity and name is used in overwrite
	public static int countOwnership(String objectname, String owner)
			throws SQLException, ClassNotFoundException {
		getConnection();
		// As object name = digest
		String sql = "select count(*) as count from OS_OTHERS_OBJECTS where OBJECT_NAME=\'"
				+ objectname + "\' and  OWNER_CLOUD<>\'" + owner + "\'";
		Statement stat = null;
		ResultSet rs = null;
		stat = Common.con.createStatement();
		rs = stat.executeQuery(sql);
		String count = null;
		while (rs.next()) {
			count = rs.getString("count");
		}
		stat.close();
		rs.close();
		return Integer.parseInt(count);
	}

	public static boolean checkIfOldVersionIsSame(String objectname,
			String owner, String digest) throws SQLException,
			ClassNotFoundException {
		getConnection();
		String sql = "select count(*) as count from OS_OTHERS_OBJECTS where  OWNER_OBJECT_NAME=\'"
				+ objectname
				+ "\' and OWNER_CLOUD=\'"
				+ owner
				+ "\' and DIGEST=\'" + digest + "\'";
		Statement stat = null;
		ResultSet rs = null;
		stat = Common.con.createStatement();
		rs = stat.executeQuery(sql);
		String count = null;
		boolean flag = false;
		while (rs.next()) {
			count = rs.getString("count");
		}
		if (count.equals("0")) {
			flag = false;
		} else {
			flag = true;
		}
		stat.close();
		rs.close();
		return flag;
	}

	public static void insertOthersVMTable(String id, String vmname,
			String cloud, String original) throws SQLException,
			ClassNotFoundException {
		getConnection();
		String sql = "Insert into VM_OTHERS_VMS (ID,VMNAME,OWNER_CLOUD,OWNER_VMNAME) VALUES (?,?,?,?)";
		PreparedStatement pst = Common.con.prepareStatement(sql);
		int idx = 1;
		pst.setString(idx++, id);
		pst.setString(idx++, vmname);
		pst.setString(idx++, cloud);
		pst.setString(idx++, original);
		pst.executeUpdate();
		pst.close();
	}

	public static Map<Long, String> getVMOthersMap(String owner)
			throws SQLException, ClassNotFoundException {
		getConnection();
		Map<Long, String> list = new HashMap<>();
		String sql = "select VMNAME, OWNER_VMNAME from VM_OTHERS_VMS where OWNER_CLOUD=\'"
				+ owner + "\'";
		Statement stat = null;
		ResultSet rs = null;
		stat = Common.con.createStatement();
		rs = stat.executeQuery(sql);
		while (rs.next()) {
			list.put(rs.getLong("VMNAME"), rs.getString("OWNER_VMNAME"));
		}
		stat.close();
		rs.close();
		return list;
	}

	public static List<Long> getLatestVMName(String owner_vmname,
			String owner_cloud) throws SQLException, ClassNotFoundException {
		getConnection();
		List<Long> list = new ArrayList<>();
		String sql = "select VMNAME from VM_OTHERS_VMS where OWNER_VMNAME=\'"
				+ owner_vmname + "\' AND OWNER_CLOUD=\'" + owner_cloud + "\'";
		Statement stat = null;
		ResultSet rs = null;
		stat = Common.con.createStatement();
		rs = stat.executeQuery(sql);
		while (rs.next()) {
			list.add(rs.getLong("VMNAME"));
		}
		Collections.reverse(list);
		stat.close();
		rs.close();
		return list;
	}

	public static String getVMOwnName(String vmname) throws SQLException,
			ClassNotFoundException {
		getConnection();
		String sql = "select VMNAME from VM_OWN_VMS where STORAGE_VMNAME=\'"
				+ vmname + "\'";
		Statement stat = null;
		ResultSet rs = null;
		stat = Common.con.createStatement();
		rs = stat.executeQuery(sql);
		String name = null;
		while (rs.next()) {
			name = rs.getString("VMNAME");
		}
		stat.close();
		rs.close();
		return name;
	}

	public static String getVMStorageName(String vmname) throws SQLException,
			ClassNotFoundException {
		getConnection();
		String sql = "select STORAGE_VMNAME from VM_OWN_VMS where VMNAME=\'"
				+ vmname + "\'";
		Statement stat = null;
		ResultSet rs = null;
		stat = Common.con.createStatement();
		rs = stat.executeQuery(sql);
		String name = null;
		while (rs.next()) {
			name = rs.getString("STORAGE_VMNAME");
		}
		stat.close();
		rs.close();
		return name;
	}

	public static void insertOwnVMTable(String id, String vmname, String cloud)
			throws SQLException, ClassNotFoundException {
		getConnection();
		String sql = "Insert into VM_OWN_VMS (ID,VMNAME,STORAGE_CLOUD,STORAGE_VMNAME) VALUES (?,?,?,?)";
		PreparedStatement pst = Common.con.prepareStatement(sql);
		int idx = 1;
		pst.setString(idx++, id);
		pst.setString(idx++, vmname);
		pst.setString(idx++, cloud);
		pst.setString(idx++, "null");
		pst.executeUpdate();
		pst.close();
	}

	public static void updateOwnVMTable(String id, String storage_vm)
			throws SQLException, ClassNotFoundException {
		getConnection();
		String sql = "update VM_OWN_VMS set STORAGE_VMNAME=? where ID =? ";
		PreparedStatement pst = Common.con.prepareStatement(sql);
		int idx = 1;
		pst.setString(idx++, storage_vm);
		pst.setString(idx++, id);
		pst.executeUpdate();
		pst.close();
	}

	/*-harry 
	 * For delete records commands. 
	 * Table: OS_OTHERS_OBJECTS
	 */
	public static void deleteOthersObjectTable(String ownercloud,
			String digest, String owner_objectname) throws SQLException,
			ClassNotFoundException {
		getConnection();
		String sql = "delete from OS_OTHERS_OBJECTS where OWNER_CLOUD =? and DIGEST=? and OWNER_OBJECT_NAME=?";
		PreparedStatement pst = Common.con.prepareStatement(sql);
		pst.setString(1, ownercloud);
		pst.setString(2, digest);
		pst.setString(3, owner_objectname);
		pst.executeUpdate();
		pst.close();
	}

	/*- harry
	 * For delete records commands.
	 * Table: OS_OWN_OBJECTS
	 */
	public static void deleteOwnObjectTable(String digest, String objectName,
			String storageCloud) throws SQLException, ClassNotFoundException {
		getConnection();
		String sql = "delete from OS_OWN_OBJECTS where DIGEST=? and OBJECT_NAME=? and STORAGE_CLOUD = ?";
		PreparedStatement pst = Common.con.prepareStatement(sql);
		pst.setString(1, digest);
		pst.setString(2, objectName);
		pst.setString(3, storageCloud);
		pst.executeUpdate();
		pst.close();
	}

	/*- harry
	 * Use for updating OS_OWN_OBJECTS if "overwrite" is true. 
	 * Table: OS_OWN_OBJECTS
	 */
	public static void updateOwnObjects(String id, String storageCloud,
			String objectName, String digest) throws SQLException,
			ClassNotFoundException {
		getConnection();
		String sql = "Update OS_OWN_OBJECTS set DIGEST = ?, ID=? where STORAGE_CLOUD = ? and OBJECT_NAME = ?";
		PreparedStatement pst = Common.con.prepareStatement(sql);
		pst.setString(1, digest);
		pst.setString(2, id);
		pst.setString(3, storageCloud);
		pst.setString(4, objectName);
		pst.executeUpdate();
		pst.close();
	}

	/*- harry
	 * Use for updating OS_OTHERS_OBJECTS if "overwrite" is true. 
	 * Table: OS_OTHERS_OBJECTS
	 */
	public static void updateOthersObjectTable2(String id, String ownerCloud,
			String objectName, String ownerObjectName, String digest)
			throws SQLException, ClassNotFoundException {
		getConnection();
		String sql = "Update OS_OTHERS_OBJECTS set ID = ?,OBJECT_NAME = ?, DIGEST=? where OWNER_CLOUD=? and OWNER_OBJECT_NAME = ?";
		PreparedStatement pst = Common.con.prepareStatement(sql);
		pst.setString(1, id);
		pst.setString(2, objectName);
		pst.setString(3, digest);
		pst.setString(4, ownerCloud);
		pst.setString(5, ownerObjectName);
		pst.executeUpdate();
		pst.close();
	}

	/*- harry
	 * check if objects with same digest from same cloud exist.
	 * Table: OS_OTHERS_OBJECTS
	 */
	public static boolean checkDuplicateCloud(String objectName, String digest,
			String owner_cloud) throws SQLException, ClassNotFoundException {
		getConnection();
		boolean flag;
		String count = null;
		String sql = "select count(*) as count from OS_OTHERS_OBJECTS where DIGEST=\'"
				+ digest
				+ "\' and OWNER_CLOUD= \'"
				+ owner_cloud
				+ "\' and OWNER_OBJECT_NAME=\'" + objectName + "\'";
		Statement stat = null;
		ResultSet rs = null;
		stat = Common.con.createStatement();
		rs = stat.executeQuery(sql);
		while (rs.next()) {
			count = rs.getString("count");
		}
		if (count.equals("0")) {
			flag = false;
		} else {
			flag = true;
		}
		stat.close();
		rs.close();
		return flag;
	}

	/*- harry
	 * if this method returns true, object with this digest can be deleted.
	 * Table: OS_OTHERS_OBJECTS
	 */
	public static boolean countOthersDigest(String digest) throws SQLException,
			ClassNotFoundException {
		getConnection();
		boolean flag = false;
		String count = null;
		String sql = "select count(*) as count from OS_OTHERS_OBJECTS where DIGEST=\'"
				+ digest + "\' ";
		Statement stat = null;
		ResultSet rs = null;
		stat = Common.con.createStatement();
		rs = stat.executeQuery(sql);
		while (rs.next()) {
			count = rs.getString("count");
		}
		if (!count.equals("0")) {
			flag = true;
		}
		stat.close();
		rs.close();
		return flag;
	}

	/*-harry
	 * Select name of object whose OWNER_CLOUD is ownerCloud and DIGEST is digest.
	 * Table: OS_OTHERS_OBJECTS
	 */
	public static String findOthersObject(String ownerCloud, String digest)
			throws SQLException, ClassNotFoundException {
		getConnection();
		String sql = "Select OBJECT_NAME from OS_OTHERS_OBJECTS where DIGEST=\'"
				+ digest + "\' and OWNER_CLOUD =\'" + ownerCloud + "\'";
		Statement stat = null;
		ResultSet rs = null;
		stat = Common.con.createStatement();
		rs = stat.executeQuery(sql);
		String objectName = null;
		while (rs.next()) {
			objectName = rs.getString("OBJECT_NAME");
		}
		stat.close();
		rs.close();
		return objectName;
	}

	/*-harry
	 * Get the object name of the version.
	 * Table: OS_OTHER_OBJECTS
	 */
	public static String getLatest(String version, String ownerCloud,
			String ownerObjectName) throws SQLException, ParseException,
			ClassNotFoundException {
		getConnection();
		String objectName = null;
		Date date = new SimpleDateFormat("yyyyMMddHHmmss").parse(version);
		java.sql.Timestamp sqlDate = new java.sql.Timestamp(date.getTime());
		String sql = "Select * from ( select OBJECT_NAME from OS_OTHERS_OBJECTS where OWNER_CLOUD =? and OWNER_OBJECT_NAME = ? and CREATE_TIME <= ? order by CREATE_TIME ASC) where rownum = 1";
		PreparedStatement pst = Common.con.prepareStatement(sql);
		pst.setString(1, ownerCloud);
		pst.setString(2, ownerObjectName);
		pst.setTimestamp(3, sqlDate);
		ResultSet rs = pst.executeQuery();
		while (rs.next()) {
			objectName = rs.getString("OBJECT_NAME");
		}
		pst.close();
		rs.close();
		return objectName;
	}

	/*-harry
	 * Get the digest of the version.
	 * Table: OS_OWN_OBJECTS
	 */
	public static String getLatestDigest(String ownerObjectName,
			String storageCloud, String version) throws ParseException,
			SQLException, ClassNotFoundException {
		getConnection();
		String digest = null;
		Date date = new SimpleDateFormat("yyyyMMddHHmmss").parse(version);
		java.sql.Timestamp sqlDate = new java.sql.Timestamp(date.getTime());
		String sql = "Select * from ( select DIGEST from OS_OWN_OBJECTS where OBJECT_NAME = ? and STORAGE_CLOUD = ? and CREATE_TIME <= ? order by CREATE_TIME ASC) where rownum = 1";
		PreparedStatement pst = Common.con.prepareStatement(sql);
		pst.setString(1, ownerObjectName);
		pst.setString(2, storageCloud);
		pst.setTimestamp(3, sqlDate);
		ResultSet rs = pst.executeQuery();
		while (rs.next()) {
			digest = rs.getString("DIGEST");
		}
		pst.close();
		rs.close();
		return digest;
	}

	/*-harry
	 * Get all digests of the designated object name.
	 */
	public static String[] getDigestofObject(String ownerCloud,
			String ownerObjectName) throws SQLException, ClassNotFoundException {
		getConnection();
		ArrayList<String> digests = new ArrayList<>();
		String sql = "Select DIGEST from OS_OTHERS_OBJECTS where OWNER_CLOUD = ? and OWNER_OBJECT_NAME = ?";
		PreparedStatement pst = Common.con.prepareStatement(sql);
		pst.setString(1, ownerCloud);
		pst.setString(2, ownerObjectName);
		ResultSet rs = pst.executeQuery();
		while (rs.next()) {
			digests.add(rs.getString("DIGEST"));
		}
		pst.close();
		rs.close();
		return digests.toArray(new String[0]);
	}

	/*-priere
	 * Get all object details owned by ownerCloud.
	 */
	public static String[] getObjectList(String ownerCloud)
			throws SQLException, ClassNotFoundException {
		getConnection();
		ArrayList<String> objects = new ArrayList<>();
		String sql = "Select OWNER_OBJECT_NAME, DIGEST, CREATE_TIME from OS_OTHERS_OBJECTS where OWNER_CLOUD = ?";
		PreparedStatement pst = Common.con.prepareStatement(sql);
		pst.setString(1, ownerCloud);
		ResultSet rs = pst.executeQuery();
		while (rs.next()) {
			StringBuilder sb = new StringBuilder();
			sb.append(rs.getString("OWNER_OBJECT_NAME") + ";");
			sb.append(rs.getString("DIGEST") + ";");
			sb.append(rs.getString("CREATE_TIME"));
			objects.add(sb.toString());
		}
		pst.close();
		rs.close();
		return objects.toArray(new String[0]);
	}

}
