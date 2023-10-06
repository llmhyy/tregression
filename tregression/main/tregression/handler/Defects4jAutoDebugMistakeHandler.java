package tregression.handler;

import java.nio.file.Paths;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import microbat.Activator;
import microbat.util.JavaUtil;
import tregression.auto.Defects4jDebugRunner;
import tregression.auto.ProjectsRunner;
import tregression.preference.TregressionPreference;

public class Defects4jAutoDebugMistakeHandler extends AbstractHandler {

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
//		System.out.println(Activator.getDefault().getPreferenceStore().getString(TregressionPreference.INPUT_FOLDER_KEY));
//		System.out.println(Activator.getDefault().getPreferenceStore().getString(TregressionPreference.OUTPUT_PATH_KEY));
//		System.out.println(Activator.getDefault().getPreferenceStore().getString(TregressionPreference.MISTAKE_PROBABILITY_KEY));
//		System.out.println(Activator.getDefault().getPreferenceStore().getString(TregressionPreference.AUTO_SIMULATION_METHOD_KEY));
		
		
		
		final String basePath = "D:\\Defects4j";
		final String resultPath = Paths.get("D:", "temp.txt").toString();
		final ProjectsRunner runner = new Defects4jMistakeDebugPIlotRunner(basePath, resultPath, 0.0d, 120l);
		runner.run();
	}

}
