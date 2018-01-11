package tregression.handler;

import java.io.File;

import microbat.Activator;
import tregression.preference.TregressionPreference;

public class PathConfiguration {
	public static String getBugPath(){
		String repoPath = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.REPO_PATH);
		String projectPath = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.PROJECT_NAME);
		String bugID = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.BUG_ID);
		
		String path = repoPath + File.separator + projectPath + File.separator + bugID;
		return path;
	}
	
	public static String getBuggyPath(){
		String bugPath = getBugPath();
		String path = bugPath + File.separator + "bug";
		return path;
	}
	
	public static String getCorrectPath(){
		String bugPath = getBugPath();
		String path = bugPath + File.separator + "fix";
		return path;
	}
}
