package hk.edu.polyu.intercloud.config;

import hk.edu.polyu.intercloud.util.CmdExecutor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

public class InitDB {

	private static List<String> commands = new ArrayList<>();

	public static void main(String[] args) throws IOException, Exception {
		String[] methods = new String[] { "Initialize", "Drop All" };
		int m = JOptionPane.showOptionDialog(null, "Select a method.",
				"Options", JOptionPane.DEFAULT_OPTION,
				JOptionPane.PLAIN_MESSAGE, null, methods, methods[0]);
		if (m == 0) {
			init();
		} else if (m == 1) {
			drop();
		}
		List<String> output = CmdExecutor.runBatchCmd(commands);
		output.stream().forEach(System.out::println);
		JOptionPane.showMessageDialog(null, "Finished!");
	}

	static void init() throws IOException, Exception {
		commands.add("echo exit | sqlplus / as sysdba @sql/setpw.sql");
		commands.add("echo exit | sqlplus INTERCLOUD/\\\"p@ssw0rd\\\"@XE @sql/import.sql");
		commands.add("echo exit | sqlplus INTERCLOUD/\\\"p@ssw0rd\\\"@XE @sql/update_20160905.sql");
		commands.add("echo exit | sqlplus INTERCLOUD/\\\"p@ssw0rd\\\"@XE @sql/update_20161128.sql");
		commands.add("echo exit | sqlplus INTERCLOUD/\\\"p@ssw0rd\\\"@XE @sql/update_20161221.sql");
		commands.add("echo exit | sqlplus INTERCLOUD/\\\"p@ssw0rd\\\"@XE @sql/update_20170104.sql");
		commands.add("echo exit | sqlplus INTERCLOUD/\\\"p@ssw0rd\\\"@XE @sql/update_20170327.sql");
		commands.add("echo exit | sqlplus INTERCLOUD/\\\"p@ssw0rd\\\"@XE @sql/update_20170720.sql");
	}

	static void drop() throws IOException, Exception {
		commands.add("echo exit | sqlplus INTERCLOUD/\\\"p@ssw0rd\\\"@XE @sql/drop.sql");
	}
}
