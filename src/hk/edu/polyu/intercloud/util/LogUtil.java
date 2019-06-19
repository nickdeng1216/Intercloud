package hk.edu.polyu.intercloud.util;

import hk.edu.polyu.intercloud.common.Common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.diogonunes.jcdp.color.ColoredPrinter;
import com.diogonunes.jcdp.color.api.Ansi.Attribute;
import com.diogonunes.jcdp.color.api.Ansi.BColor;
import com.diogonunes.jcdp.color.api.Ansi.FColor;

public class LogUtil {

	public static final SimpleDateFormat SDF = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss");
	public static final String LINE = System.lineSeparator();

	public static void logPerformance(String operation, String remarks,
			long timestamp, long duration) {
		if (Common.perfLog) {
			if (remarks == null) {
				remarks = "";
			}
			try {
				String date = SDF.format(new Date());
				StringBuilder sb = new StringBuilder();
				sb.append(date).append(", ").append(operation).append(", ")
						.append(remarks).append(", ").append(timestamp)
						.append(", ").append(duration).append(LINE);
				String text = sb.toString();
				// Print
				System.out.print(">>>>> PERF LOG: " + text);
				// Log
				Files.write(Paths.get(Common.perfLogFile), text.getBytes(),
						StandardOpenOption.APPEND);
			} catch (IOException e) {
				LogUtil.logException(e);
			}
		}
	}

	public static void logException(Exception e) {
		try {
			String date = SDF.format(new Date());
			StringBuilder sb = new StringBuilder();
			sb.append(date).append("\t").append(ExceptionUtils.getMessage(e))
					.append(LINE).append(ExceptionUtils.getStackTrace(e))
					.append(LINE);
			String text = sb.toString();
			// Print
			System.err.println("XXXXX EXCEPTION: " + date
					+ e.getClass().getCanonicalName());
			e.printStackTrace();
			// Log
			Files.write(Paths.get(Common.errLogFile), text.getBytes(),
					StandardOpenOption.APPEND);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public static void logError(String msg) {
		try {
			String date = SDF.format(new Date());
			StringBuilder sb = new StringBuilder();
			sb.append(date).append("\t").append(msg).append(LINE);
			String text = sb.toString();
			// Print
			System.err.print("XXXXX ERROR: " + text);
			// Log
			Files.write(Paths.get(Common.errLogFile), text.getBytes(),
					StandardOpenOption.APPEND);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public static void logWatchdog(long usedMemory, long totalMemory,
			long usedSystemMemory, long totalSystemMemory, double cpuLoad,
			double systemCpuLoad) {
		try {
			String date = SDF.format(new Date());
			StringBuilder sb = new StringBuilder();
			// JVM Memory
			sb.append(date).append(", JVM memory usage, ").append(usedMemory)
					.append("MB, JVM total memory, ").append(totalMemory)
					.append("MB").append(LINE);
			// System Memory
			sb.append(date).append(", System memory usage, ")
					.append(usedSystemMemory)
					.append("MB, System total memory, ")
					.append(totalSystemMemory).append("MB").append(LINE);
			// CPU
			sb.append(date).append(", JVM CPU load, ")
					.append((int) (100 * cpuLoad))
					.append("%, System CPU load, ")
					.append((int) (100 * systemCpuLoad)).append('%')
					.append(LINE);
			String text = sb.toString();
			// Print
			System.out.println("---------- WATCHDOG ----------" + LINE + text
					+ "------------------------------");
			// Log
			Files.write(Paths.get(Common.watchdogLogFile), text.getBytes(),
					StandardOpenOption.APPEND);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	// NOTE: This method does not work in Eclipse!
	public static void colorPrint(String message, Attribute a, FColor fc,
			BColor bc) {
		ColoredPrinter cp = new ColoredPrinter.Builder(3, false).attribute(a)
				.foreground(fc).background(bc).build();
		cp.println(message);
		cp.clear();
	}

}
