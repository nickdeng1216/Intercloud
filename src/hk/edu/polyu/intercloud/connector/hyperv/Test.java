package hk.edu.polyu.intercloud.connector.hyperv;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JOptionPane;

public class Test {

	static HyperVIntercloud h;
	static HyperVUtil u;

	public Test() throws HyperVIntercloudException {
		h = new HyperVIntercloud();
		u = new HyperVUtil();
	}

	public static void main(String[] args) throws HyperVIntercloudException,
			IOException, InterruptedException {

		if (JOptionPane.showConfirmDialog(null, "Delete VM?", "HyperV API",
				JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
			h.deleteVM("OvfHVTest2", null, true);
		}

		if (JOptionPane.showConfirmDialog(null, "New VM?", "HyperV API",
				JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
			String name = "OvfHVTest2";
			int numberOfCPUCores = 2;
			int memoryGB = 2;
			List<VHD> vhds = new ArrayList<VHD>();
			String vhd1path = "C:\\OvfHVTest4\\OvfHVTest4-disk1.vhdx";
			String vhd1size = "32";
			vhds.add(new VHD(vhd1path, Integer.valueOf(vhd1size)));
			List<VHD> newVHDs = new ArrayList<VHD>();
			String vhd2path = "C:\\OvfHVTest4\\OvfHVTest4-diskNew20GB.vhdx";
			String vhd2size = "20";
			newVHDs.add(new VHD(vhd2path, Integer.valueOf(vhd2size)));
			List<String> isos = new ArrayList<String>();
			isos.add("");
			int numberOfNetworkAdapters = 2;
			boolean powerOn = false;
			h.newVM(name, numberOfCPUCores, memoryGB, vhds, newVHDs, isos,
					numberOfNetworkAdapters, powerOn);
		}

		if (JOptionPane.showConfirmDialog(null, "Get VM List?", "HyperV API",
				JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
			System.out.println(h.getVMList());
		}

		if (JOptionPane.showConfirmDialog(null, "Get VM Details?",
				"HyperV API", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
			VM v = h.getVMDetails("VMH19_1");
			System.out.println(v.getMemoryB());
			System.out.println(v.getNumberOfCPUCores());
			System.out.println(v.getNetworkAdapters());
			System.out.println(v.getOs());
			System.out.println(v.getState());
		}
	}

	public static void oldMain() throws HyperVIntercloudException {
		if (JOptionPane.showConfirmDialog(null, "Export VM?", "HyperV API",
				JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
			String vmname = "VMH22_1";
			String format = "hyperv";
			String date = new SimpleDateFormat("yyyyMMddHHmmss")
					.format(new Date());
			String toDirectory = "C:\\VMSaved\\" + date;
			List<String> vmFilepaths = h.getVM(vmname, toDirectory, format);
			System.out.println("VM Files has been stored to " + vmFilepaths);
		}

		if (JOptionPane.showConfirmDialog(null, "Import VM?", "HyperV API",
				JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
			String oldVMname = "VMH22_1";
			String newVMname = "VMH32_1";
			String format = "hyperv";
			String fromDirectory = "C:\\VMSaved\\"
					+ JOptionPane.showInputDialog("Date of exported files?")
					+ "\\" + oldVMname;
			h.putVM(newVMname, fromDirectory, format);
		}
	}
}
