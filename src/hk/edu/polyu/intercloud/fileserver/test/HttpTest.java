package hk.edu.polyu.intercloud.fileserver.test;

import hk.edu.polyu.intercloud.fileserver.client.Http;
import hk.edu.polyu.intercloud.fileserver.exceptions.HttpException;

import javax.swing.JOptionPane;

public class HttpTest {

	public static void main(String[] args) throws HttpException {
		String fullUrl = JOptionPane.showInputDialog("Full URL:");
		String localFile = JOptionPane.showInputDialog("Download to:");
		Http.download(fullUrl, localFile);
	}
}
