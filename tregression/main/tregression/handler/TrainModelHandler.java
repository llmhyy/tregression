package tregression.handler;

import java.nio.file.Paths;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import microbat.util.JavaUtil;
import tregression.auto.MutationTrainModelRunner;
import tregression.auto.ProjectsRunner;

public class TrainModelHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		JavaUtil.sourceFile2CUMap.clear();
		Job job = new Job("Testing Tregression") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				execute();
				return Status.OK_STATUS;
			}
		};
		job.schedule();
		return null;
	}
	
	protected void execute() {
		final String basePath = "E:\\david\\Mutation_Dataset";
		final String resultPath = Paths.get(basePath, "train_result.txt").toString();
		ProjectsRunner runner = new MutationTrainModelRunner(basePath, resultPath);
		runner.run();
	}
}
