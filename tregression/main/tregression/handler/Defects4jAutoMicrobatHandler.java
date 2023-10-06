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
import tregression.auto.Defects4jMicrobatRunner;
import tregression.auto.ProjectsRunner;

public class Defects4jAutoMicrobatHandler extends AbstractHandler {

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
	
	private void execute() {
		final String basePath = "D:\\Defects4j";
		final String resultPath = Paths.get("D:\\david\\result_version_3", "temp.txt").toString();
		final ProjectsRunner runner = new Defects4jMicrobatRunner(basePath, resultPath, 0.0d, 120l);
		runner.run();
	}

}
