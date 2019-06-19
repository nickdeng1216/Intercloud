package hk.edu.polyu.intercloud.config;

import hk.edu.polyu.intercloud.model.cloud.Cloud;
import hk.edu.polyu.intercloud.util.DatabaseUtil;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

public class AddFriends {

	public static void main(String args[]) throws ClassNotFoundException,
			SQLException {
		List<Cloud> clouds = new ArrayList<>();
		while (JOptionPane
				.showConfirmDialog(null, "Do you want to add clouds?") == JOptionPane.YES_OPTION) {
			String name = JOptionPane.showInputDialog("Name:");
			String roles[] = new String[] { "ROOT", "EXCHANGE", "CLOUD" };
			int r = JOptionPane.showOptionDialog(null, "Role:", "Options",
					JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
					null, roles, roles[2]);
			clouds.add(new Cloud(name, name, roles[r], true));
		}
		DatabaseUtil.addFriends(clouds);
		JOptionPane.showMessageDialog(null, "CONNECTED_CLOUDS populated.");
	}
}
