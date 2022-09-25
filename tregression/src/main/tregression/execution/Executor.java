package tregression.execution;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import tregression.constants.OperatingSystem;
import tregression.execution.outputhandlers.OutputHandler;

/**
 *
 * @author Yun Lin
 * @author knightsong
 *
 */
public class Executor {
	private static OperatingSystem operatingSystem;

	private final ProcessBuilder pb = new ProcessBuilder();

	private OutputHandler outputHandler = new OutputHandler();

	public Executor(File root) {
		pb.directory(root);
	}

	public static OperatingSystem getOS() {
		if (operatingSystem == null) {
			String osString = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
			if (osString.contains("mac") || osString.contains("darwin")) {
				operatingSystem = OperatingSystem.MACOS;
			} else if (osString.contains("win")) {
				operatingSystem = OperatingSystem.WINDOWS;
			} else {
				operatingSystem = OperatingSystem.LINUX;
			}
		}
		return operatingSystem;
	}

	public static boolean isWindows() {
		return getOS() == OperatingSystem.WINDOWS;
	}

	public void setOutputHandler(OutputHandler outputHandler) {
		this.outputHandler = outputHandler;
	}

	public String exec(String cmd) {
		try {
			return this.exec(cmd, 0);
		} catch (TimeoutException e) {
			e.printStackTrace(); // should not timeout
			return null;
		}
	}

	/**
	 * Run command line and get results,you can combine the multi-command by ";"
	 * for example: mvn test -Dtest="testcase",or git reset;mvn compile
	 *
	 * @param cmd command line
	 * @return return result by exec command
	 */
	protected String exec(String cmd, int timeout) throws TimeoutException{
		StringBuilder builder = new StringBuilder();
		Process process = null;
		InputStreamReader inputStr = null;
		BufferedReader bufferReader = null;
		pb.redirectErrorStream(true); //redirect error stream to standard stream
		try {
			if (getOS() == OperatingSystem.WINDOWS) {
				pb.command("cmd.exe", "/c", cmd);
			} else {
				pb.command("bash", "-c", cmd);
			}
			outputHandler.output(cmd);
			process = pb.start();
			if (timeout > 0) {
				boolean completed = process.waitFor(timeout, TimeUnit.MINUTES);
				if (!completed)
					throw new TimeoutException();
			}
			inputStr = new InputStreamReader(process.getInputStream());
			bufferReader = new BufferedReader(inputStr);
			String line;
			while ((line = bufferReader.readLine()) != null) {
				outputHandler.output(line);
				builder.append("\n").append(line);
			}
		} catch (IOException | InterruptedException ex) {
			ex.printStackTrace();
		} finally {
			try {
				if (process != null) {
					process.destroy();
				}
				if (inputStr != null) {
					inputStr.close();
				}
				if (bufferReader != null) {
					bufferReader.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return builder.toString();
	}

}

