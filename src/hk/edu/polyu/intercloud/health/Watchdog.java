package hk.edu.polyu.intercloud.health;

import hk.edu.polyu.intercloud.common.Common;
import hk.edu.polyu.intercloud.util.LogUtil;

import java.lang.management.ManagementFactory;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import com.sun.management.OperatingSystemMXBean;

public class Watchdog {

	public static OperatingSystemMXBean operatingSystemMXBean = (OperatingSystemMXBean) ManagementFactory
			.getOperatingSystemMXBean();
	// JVM Memory
	static long totalMemory, freeMemory, usedMemory;
	// System Memory
	static long totalSystemMemory, freeSystemMemory, usedSystemMemory;
	// CPU
	static double cpuLoad, systemCpuLoad;

	public static void start() {
		System.out
				.println("Watchdog runs every "
						+ Common.watchdogInterval
						+ "s."
						+ System.lineSeparator()
						+ "You may modify \"watchdog_interval\" in the properties file.");
		System.out.println("Detected system: " + getOS());
		Runnable r = new Runnable() {
			@Override
			public void run() {
				while (true) {
					try {
						update();

						TimeUnit.SECONDS.sleep(Common.watchdogInterval);
					} catch (InterruptedException e) {
					}
				}
			}
		};
		new Thread(r, "Watchdog").start();
	}

	@SuppressWarnings("restriction")
	static void update() {
		// JVM Memory
		totalMemory = Runtime.getRuntime().totalMemory() / 1024 / 1024;
		freeMemory = Runtime.getRuntime().freeMemory() / 1024 / 1024;
		usedMemory = totalMemory - freeMemory;
		// System Memory
		totalSystemMemory = operatingSystemMXBean.getTotalPhysicalMemorySize() / 1024 / 1024;
		freeSystemMemory = operatingSystemMXBean.getFreePhysicalMemorySize() / 1024 / 1024;
		usedSystemMemory = totalSystemMemory - freeSystemMemory;
		// CPU
		cpuLoad = operatingSystemMXBean.getProcessCpuLoad();
		systemCpuLoad = operatingSystemMXBean.getSystemCpuLoad();
		// Log
		LogUtil.logWatchdog(usedMemory, totalMemory, usedSystemMemory,
				totalSystemMemory, cpuLoad, systemCpuLoad);
	}

	static String getOS() {
		String os = operatingSystemMXBean.getName().toLowerCase(Locale.ENGLISH);
		if (os.contains("mac") || os.contains("darwin")) {
			return "Mac";
		} else if (os.contains("win")) {
			return "Windows";
		} else if (os.contains("nux")) {
			return "Linux";
		}
		return "Others";
	}
}
