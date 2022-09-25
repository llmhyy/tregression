package tregression.handler.paths;

import java.io.File;

import microbat.Activator;
import tregression.constants.Dataset;
import tregression.preference.TregressionPreference;

public abstract class PathConfiguration {

	public String getBugPath(String projectName, String bugId){
		StringBuilder repoPath = new StringBuilder(Activator.getDefault().getPreferenceStore().getString(TregressionPreference.REPO_PATH));
		return repoPath.append(File.separator).append(projectName).append(File.separator).append(bugId).toString();
	}
	
	abstract public String getBuggyPath(String projectName, String bugId);
	
	abstract public String getCorrectPath(String projectName, String bugId);
	
	public String getProjectName(String path) {
		String repoPath = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.REPO_PATH);
		int startOfProjName = repoPath.length() + 1;
		int endOfProjectName = path.indexOf(File.separator, startOfProjName);
		return path.substring(startOfProjName, endOfProjectName);
	}
	
	public String getBugId(String path) {
		String repoPath = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.REPO_PATH);
		int repoPathLen = repoPath.length();
		int endOfProjectName = path.indexOf(File.separator, repoPathLen + 1);
		int startOfBugId = endOfProjectName + 1;
		int endOfBugId = path.indexOf(File.separator, startOfBugId);
		return path.substring(startOfBugId, endOfBugId);
	}
}
