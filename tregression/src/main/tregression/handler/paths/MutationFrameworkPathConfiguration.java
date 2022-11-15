package tregression.handler.paths;

import java.io.File;

public class MutationFrameworkPathConfiguration extends PathConfiguration {

	@Override
	public String getBuggyPath(String projectName, String bugId) {
		StringBuilder result = new StringBuilder(getBugPath(projectName, bugId));
		result.append(File.separator);
		result.append("bug");
		return result.toString();
	}

	@Override
	public String getCorrectPath(String projectName, String bugId) {
		StringBuilder result = new StringBuilder(getBugPath(projectName, bugId));
		result.delete(result.length() - bugId.length(), result.length());
		result.append("fix");
		return result.toString();
	}

}
