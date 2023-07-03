package tregression.io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class StoredIOParser {
	private String filePath;
	
	public StoredIOParser(String filePath) {
		this.filePath = filePath;
	}
	
	public String[] getOutputInfo(String projectName, String bugID) {
		FileReader fileReader = null;
		BufferedReader bufferedReader = null;
		try {
			fileReader = new FileReader(this.filePath);
			bufferedReader = new BufferedReader(fileReader);
			String line = bufferedReader.readLine(); // first line: headers
			line = bufferedReader.readLine();
			while (line != null) {
				String[] entries = line.split(",");
				if (projectName.equals(entries[0]) && bugID.equals(entries[1])) {
					bufferedReader.close();
					return entries;
				}
				line = bufferedReader.readLine();
			}
			bufferedReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
