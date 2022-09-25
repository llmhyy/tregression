package tregression.handler.paths;

import java.io.File;

public class Regs4jPathConfiguration extends PathConfiguration {
	public String getBuggyPath(String projectName, String bugId){
		StringBuilder result = new StringBuilder(getBugPath(projectName, bugId));
		result.append(File.separator);
		result.append("ric");
		return result.toString();
	}
	
	public String getCorrectPath(String projectName, String bugId){
		StringBuilder result = new StringBuilder(getBugPath(projectName, bugId));
		result.append(File.separator);
		result.append("rfc");
		return result.toString();
	}
}
