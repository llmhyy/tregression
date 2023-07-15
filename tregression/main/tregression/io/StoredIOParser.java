package tregression.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import iodetection.IODetector.InputsAndOutput;

public class StoredIOParser {
	private String directoryPath;
	private String projectName;
	private String bugID;
	
	public StoredIOParser(String directoryPath, String projectName, String bugID) {
		this.directoryPath = directoryPath;
		this.projectName = projectName;
		this.bugID = bugID;
	}
	
	private File getFile() {
		String projectDirectoryPath = this.directoryPath + File.separator + this.projectName;
		String filePath = projectDirectoryPath + File.separator + this.bugID + ".txt";
		File projectDirectory = new File(projectDirectoryPath);
		if (!projectDirectory.exists()) {
			projectDirectory.mkdirs();
		}
		File file = new File(filePath);
		return file;
	}
	
	public HashMap<String, List<String[]>> getStoredIO() {
		try {
			File file = getFile();
			if (!file.exists()) {
				return null;
			}
			HashMap<String, List<String[]>> IO = new HashMap<>();
			FileReader fileReader = new FileReader(file);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			String line = bufferedReader.readLine();
			String key = "";
			while (line != null) {
				if (line.equals(InputsAndOutput.INPUTS_KEY)) {
					key = InputsAndOutput.INPUTS_KEY;
					IO.put(key, new ArrayList<>());
				} else if (line.equals(InputsAndOutput.OUTPUT_KEY)) {
					key = InputsAndOutput.OUTPUT_KEY;
					IO.put(key, new ArrayList<>());
				} else {
					String[] entries = line.split(" ");
					IO.get(key).add(entries);
				}
				line = bufferedReader.readLine();
			}
			bufferedReader.close();
			fileReader.close();
			return IO;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public void storeIO(Optional<InputsAndOutput> ioOptional) {
		InputsAndOutput IO = ioOptional.get();
		String IOString = IO.toString();
		try {
			File file = getFile();
			if (!file.exists()) {
				file.createNewFile();
			}
			FileWriter fileWriter = new FileWriter(file);
			fileWriter.append(IOString);
			fileWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
