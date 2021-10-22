package tregression.empiricalstudy.config;

import java.io.File;

public class ConfigFactory {
	public static ProjectConfig createConfig(String projectName, String regressionID, String path) {
		if(isDefects4JProject(projectName)) {
			ProjectConfig config = Defects4jProjectConfig.getConfig(projectName, regressionID);
			return config;
		}
		
		if (MavenProjectConfig.check(path)) {
			File pom = new File(path + File.separator + "pom.xml");
			return MavenProjectConfig.getConfig(pom, projectName, regressionID);
		}
		return null;
	}

	private static boolean isDefects4JProject(String projectName) {
		return projectName.equals("Chart") ||
				projectName.equals("Closure") ||
				projectName.equals("Lang") ||
				projectName.equals("Math") ||
				projectName.equals("Mockito") ||
				projectName.equals("Time");
	}
}
