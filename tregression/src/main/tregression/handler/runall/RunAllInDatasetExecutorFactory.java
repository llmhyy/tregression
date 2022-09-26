package tregression.handler.runall;

import tregression.constants.Dataset;

public class RunAllInDatasetExecutorFactory {
	public static RunAllInDatasetExecutor createExecutor(Dataset datasetType) {
		if (datasetType.equals(Dataset.DEFECTS4J)) {
			return new AllDefects4jExecutor();
		}
		return new AllRegs4jHandlerExecutor();
	}
}
