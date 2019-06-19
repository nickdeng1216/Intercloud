package hk.edu.polyu.intercloud.fileserver.test;

import hk.edu.polyu.intercloud.fileserver.client.Ftp;
import hk.edu.polyu.intercloud.fileserver.exceptions.FtpException;

import javax.swing.JOptionPane;

public class FtpTest {

	public static void main(String args[]) {

		String ip = "127.0.0.1";
		int port = 21;
		String username = "intercloud";
		String password = "p@ssw0rd";
		boolean activeMode = true;
		boolean anonymous = true;
		boolean ssl = false;

		String options[] = { "List Files", "Download", "Upload", "Cancel" };
		int choice = JOptionPane.showOptionDialog(null,
				"Please choose an option.", "FTP", 0,
				JOptionPane.QUESTION_MESSAGE, null, options, options[3]);
		if (choice == 3) {
			return;
		}
		String remoteFile = JOptionPane.showInputDialog("Remote file:");
		String localFile = JOptionPane.showInputDialog("Local file:");
		try {
			if (choice == 0) {
				Ftp.list(ip, port, username, password, remoteFile, activeMode,
						anonymous, ssl);
			} else if (choice == 1) {
				Ftp.download(ip, port, username, password, remoteFile,
						localFile, activeMode, anonymous, ssl);
			} else if (choice == 2) {
				Ftp.upload(ip, port, username, password, remoteFile, localFile,
						activeMode, anonymous, ssl);
			}
		} catch (FtpException e) {
			e.printStackTrace();
		}
	}
}
