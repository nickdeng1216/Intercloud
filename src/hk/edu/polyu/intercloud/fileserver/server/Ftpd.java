package hk.edu.polyu.intercloud.fileserver.server;

import hk.edu.polyu.intercloud.fileserver.exceptions.FtpdException;

import java.io.File;

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.ssl.SslConfigurationFactory;
import org.apache.ftpserver.usermanager.Md5PasswordEncryptor;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import rheise.jftpd.Server;

public class Ftpd {

	@Deprecated
	private static FtpServer ftpServer;
	private static FtpServer ftpsServer;

	/**
	 * Start the FTP server
	 * 
	 * @param port
	 * @throws FtpdException
	 */
	public static void startFtpServer(int port) throws FtpdException {
		System.out.println("Starting plain FTP server on port " + port);
		try {
			Thread t = new Thread() {
				@Override
				public void run() {
					try {
						Server.main(new String[] { String.valueOf(port) });
					} catch (Exception e) {
					}
				}
			};
			t.start();
		} catch (Exception e) {
			throw new FtpdException(e.getMessage(), e);
		}
	}

	/**
	 * @deprecated Start the FTP server
	 * 
	 * @param port
	 * @throws FtpdException
	 */
	@Deprecated
	public static void startFtpServer_old(int port) throws FtpdException {
		System.out.println("Starting plain FTP server on port " + port);
		FtpServerFactory serverFactory = new FtpServerFactory();
		ListenerFactory listenerFactory = new ListenerFactory();
		PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory();
		try {
			listenerFactory.setServerAddress("0.0.0.0");
			listenerFactory.setPort(port);
			userManagerFactory.setFile(new File("FtpdUsers.properties"));
			userManagerFactory.setPasswordEncryptor(new Md5PasswordEncryptor());
			serverFactory
					.setUserManager(userManagerFactory.createUserManager());
			serverFactory.addListener("default",
					listenerFactory.createListener());
			ftpServer = serverFactory.createServer();
			ftpServer.start();
		} catch (Exception e) {
			throw new FtpdException(e.getMessage(), e);
		}
	}

	/**
	 * Kill the FTP server
	 */
	public static void killFtpServer() {
		;
	}

	/**
	 * @deprecated Kill the FTP server
	 */
	@Deprecated
	public static void killFtpServer_old() {
		try {
			System.out.println("Killing FTP server.");
			ftpServer.stop();
		} catch (NullPointerException e) {
			System.out.println(e.getMessage());
		}
	}

	/**
	 * Start the FTPS server
	 * 
	 * Please create a Self Signed Certificate using Java Keytool first
	 * https://www.sslshopper.com/article-how-to-create-a-self-signed
	 * -certificate-using-java-keytool.html
	 * 
	 * @throws FtpdException
	 */
	public static void startFtpsServer(int port, String jksPassword)
			throws FtpdException {
		System.out.println("Starting FTP over SSL server on port " + port);
		FileAppender fa = new FileAppender();
		fa.setFile("log/ftp.log");
		fa.setLayout(new PatternLayout("%d %-5p [%c{1}] %m%n"));
		fa.setThreshold(Level.ERROR);
		fa.setAppend(true);
		fa.activateOptions();
		Logger.getRootLogger().addAppender(fa);
		FtpServerFactory serverFactory = new FtpServerFactory();
		ListenerFactory listenerFactory = new ListenerFactory();
		PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory();
		SslConfigurationFactory ssl = new SslConfigurationFactory();
		try {
			ssl.setKeystoreFile(new File("ftpsd.jks")); // FTPS
			ssl.setKeystorePassword(jksPassword); // FTPS
			listenerFactory.setServerAddress("0.0.0.0");
			listenerFactory.setPort(port);
			listenerFactory.setSslConfiguration(ssl.createSslConfiguration()); // FTPS
			listenerFactory.setImplicitSsl(true); // FTPS
			userManagerFactory.setFile(new File("FtpsdUsers.properties"));
			userManagerFactory.setPasswordEncryptor(new Md5PasswordEncryptor());
			serverFactory
					.setUserManager(userManagerFactory.createUserManager());
			serverFactory.addListener("default",
					listenerFactory.createListener());
			ftpsServer = serverFactory.createServer();
			ftpsServer.start();
		} catch (Exception e) {
			throw new FtpdException(e.getMessage(), e);
		}
	}

	/**
	 * Kill the FTPS server
	 */
	public static void killFtpsServer() {
		try {
			ftpsServer.stop();
		} catch (NullPointerException e) {
			System.out.println(e.getMessage());
		}
	}
}
