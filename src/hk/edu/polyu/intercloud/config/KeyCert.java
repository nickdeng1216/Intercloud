package hk.edu.polyu.intercloud.config;

import hk.edu.polyu.intercloud.common.Common;
import hk.edu.polyu.intercloud.exceptions.SecurityException;
import hk.edu.polyu.intercloud.fileserver.client.PostJSON;
import hk.edu.polyu.intercloud.security.CSR;
import hk.edu.polyu.intercloud.util.KeyUtil;
import hk.edu.polyu.intercloud.util.ZipUtil;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Properties;

import javax.swing.JOptionPane;

import net.lingala.zip4j.exception.ZipException;

import org.apache.commons.io.FileUtils;

public class KeyCert {

    public static void main(String args[]) throws IOException,
            SecurityException, ZipException {
        String cloudName = getProp("name");
        if (cloudName == null || cloudName.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Please complete configuration first.");
            return;
        }
        genKeyPair();
        csr();
    }

    static void genKeyPair() throws IOException, SecurityException {
        Path privatePem = Paths.get(Common.KEY_PATH + "private.pem");
        Path publicPem = Paths.get(Common.KEY_PATH + "public.pem");
        if ((Files.exists(privatePem) && Files.isRegularFile(privatePem))
                || (Files.exists(publicPem) && Files.isRegularFile(publicPem))) {
            int i = JOptionPane.showConfirmDialog(null,
                    "Pem files already exist. Overwrite?");
            if (i != JOptionPane.YES_OPTION) {
                return;
            }
        }
        KeyUtil.generateKeyPair();
    }

    static void csr() throws SecurityException, IOException, ZipException {
        String cloudName = getProp("name");
        String email = getProp("email");

        String cerName = Common.KEY_PATH + cloudName + ".csr";
        String csr = CSR.generatePKCS10("CN=" + cloudName
                        + ", L=PolyU, C=HKSAR", Common.KEY_PATH + "private.pem",
                Common.KEY_PATH + "public.pem");
        KeyUtil.writefile(cerName, csr);
        String url = Common.ca_domain + "/upload";
        String pk = readToString(Common.KEY_PATH + "public.pem").replaceAll("\r\n","");
        String csrZip = csr.replaceAll("\r\n","");
        String params = "{\"domain\":\"" + cloudName + "\","
                + "\"csr\":\"" + csrZip + "\","
                + "\"email\":\"" + email + "\","
                + "\"publickey\":\"" + pk + "\"" + "}";
        PostJSON.Post(url, params);
        JOptionPane.showMessageDialog(null, "CSR generated. Please wait 10 minutes.");
    }

    static public String readToString(String fileName) {
        String encoding = "ISO-8859-1";
        File file = new File(fileName);
        Long filelength = file.length();
        byte[] filecontent = new byte[filelength.intValue()];
        try {
            FileInputStream in = new FileInputStream(file);
            in.read(filecontent);
            in.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            return new String(filecontent, encoding);
        } catch (UnsupportedEncodingException e) {
            System.err.println("The OS does not support " + encoding);
            e.printStackTrace();
            return null;
        }
    }

    //    static void csr() throws SecurityException, IOException, ZipException {
//        String cloudName = JOptionPane.showInputDialog("Name:");
//        String csr = CSR.generatePKCS10("CN=" + cloudName
//                        + ", L=PolyU, C=HKSAR", Common.KEY_PATH + "private.pem",
//                Common.KEY_PATH + "public.pem");
//        KeyUtil.writefile(Common.KEY_PATH + cloudName + ".csr", csr);
//        File file = new File(cloudName + "_csr.zip");
//        if (file.exists() && file.isFile()) {
//            FileUtils.forceDelete(file);
//        }
//        ZipUtil.create(cloudName + "_csr.zip", Common.KEY_PATH + cloudName
//                + ".csr", ZipUtil.HIGH_COMPRESSION, cloudName);
//        JOptionPane.showMessageDialog(null,
//                "CSR generated. Please send the file '" + cloudName
//                        + "_csr.zip" + "' to the administrator's e-mail.");
//    }
    static String getProp(String item) throws IOException {
        Properties properties = new Properties();
        InputStream inputStream;
        String prop = "";
        inputStream = new FileInputStream(Common.GW_PROP_FILE);
        properties.load(inputStream);
        prop = properties.getProperty(item);
        System.out.println(item + " = " + prop);
        return prop;
    }
}
