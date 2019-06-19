package hk.edu.polyu.intercloud.fileserver.test;

import hk.edu.polyu.intercloud.common.Common;
import hk.edu.polyu.intercloud.fileserver.exceptions.FtpdException;
import hk.edu.polyu.intercloud.fileserver.exceptions.HttpdException;
import hk.edu.polyu.intercloud.fileserver.exceptions.UdtdException;
import hk.edu.polyu.intercloud.fileserver.server.Ftpd;
import hk.edu.polyu.intercloud.fileserver.server.Httpd;
import hk.edu.polyu.intercloud.fileserver.server.Udtd;

import java.io.IOException;

import javax.swing.JOptionPane;

public class FileServerTest {

	public static void main(String[] args) throws IOException, UdtdException,
			FtpdException, HttpdException {

		Udtd.killServer();

		String options[] = { "UDT only", "FTP only", "FTPS only", "HTTP only",
				"HTTP+HTTPS" };
		int choice = JOptionPane.showOptionDialog(null,
				"Please choose an option.", "FTP", 0,
				JOptionPane.QUESTION_MESSAGE, null, options, "List Files");
		int udtport = 9000;
		int ftpport = 21;
		int ftpsport = 990;
		String jksPassword = "password";

		switch (choice) {
		case 0:
			Udtd.startServer(udtport);
			break;
		case 1:
			Ftpd.startFtpServer(ftpport);
			break;
		case 2:
			Ftpd.startFtpsServer(ftpsport, jksPassword);
			break;
		case 3:
			Httpd.startHttpServer("", Common.DOWNLOAD_PATH, "", false);
			break;
		case 4:
			Httpd.startHttpServer(JOptionPane.showInputDialog("Domain name:"),
					Common.DOWNLOAD_PATH, Common.SSL_EMAIL, true);
			break;
		}

		JOptionPane.showMessageDialog(null, "File server (" + options[choice]
				+ ") is running. Stop?", "File Server",
				JOptionPane.INFORMATION_MESSAGE);
		Udtd.killServer();
		Httpd.killHttpServer();
		System.exit(0);
	}

}
