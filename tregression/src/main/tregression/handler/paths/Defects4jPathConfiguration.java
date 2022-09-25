package tregression.handler.paths;

import java.io.File;

public class Defects4jPathConfiguration extends PathConfiguration {
	public String getBuggyPath(String projectName, String bugId){
		StringBuilder result = new StringBuilder(getBugPath(projectName, bugId));
		result.append(File.separator);
		result.append("bug");
		return result.toString();
	}
	
	public String getCorrectPath(String projectName, String bugId){
		StringBuilder result = new StringBuilder(getBugPath(projectName, bugId));
		result.append(File.separator);
		result.append("fix");
		return result.toString();
	}
}
