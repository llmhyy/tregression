package tregression.empiricalstudy.config;

import tregression.constants.Dataset;

public class ConfigFactory {
	public static ProjectConfig createConfig(String projectName, String regressionID, String buggyPath, String fixPath, Dataset dataset) {
		if(dataset.equals(Dataset.DEFECTS4J)) {
			return Defects4jProjectConfig.getConfig(projectName, regressionID);
		}
		return Regs4jProjectConfig.getConfig(projectName, regressionID);
	}
}
