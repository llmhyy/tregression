package tregression.handler.paths;

import tregression.constants.Dataset;

public class PathConfigurationFactory {
	public static PathConfiguration createPathConfiguration(Dataset datasetType) {
		if (datasetType.equals(Dataset.DEFECTS4J)) {
			return new Defects4jPathConfiguration();
		}
		return new Regs4jPathConfiguration();
	}
}
