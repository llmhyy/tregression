package tregression.handler.runall;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.jobs.Job;

import tregression.constants.Dataset;

public class RunAllInDatasetHandler extends AbstractHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		RunAllInDatasetExecutor executor = RunAllInDatasetExecutorFactory.createExecutor(Dataset.getTypeFromPref());
		executor.execute();
		return null;
	}
}
