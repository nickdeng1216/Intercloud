package hk.edu.polyu.intercloud.main;

import hk.edu.polyu.intercloud.config.AddFriends;
import hk.edu.polyu.intercloud.config.ConfigFiles;
import hk.edu.polyu.intercloud.config.InitDB;
import hk.edu.polyu.intercloud.config.KeyCert;

import javax.swing.JOptionPane;

/**
 * Configuration class
 * 
 * @author Priere
 *
 */
public class Configuration {

	public static void main(String[] args) {
		try {
			String[] methods = new String[] { "Database", "Key Cert",
					"Add Friends", "Config Files" };
			int m = JOptionPane.showOptionDialog(null, "Select an option.",
					"Options", JOptionPane.DEFAULT_OPTION,
					JOptionPane.PLAIN_MESSAGE, null, methods, methods[0]);
			if (m == 0) {
				InitDB.main(args);
			} else if (m == 1) {
				KeyCert.main(args);
			} else if (m == 2) {
				AddFriends.main(args);
			} else if (m == 3) {
				ConfigFiles.main(args);
			}
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, e.getMessage());
			e.printStackTrace();
		}
	}
}
