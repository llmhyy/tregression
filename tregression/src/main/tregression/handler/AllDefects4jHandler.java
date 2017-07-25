package tregression.handler;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import microbat.Activator;
import tregression.empiricalstudy.EmpiricalTrial;
import tregression.empiricalstudy.TrialGenerator;
import tregression.preference.TregressionPreference;

public class AllDefects4jHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Job job = new Job("Do evaluation") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				
				String[] projects = {"Chart", "Closure", "Lang", "Math", "Mockito", "Time"};
				int[] bugNum = {26, 133, 65, 106, 38, 27};
				
				String config = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.BUGGY_PATH);
				String prefix = config.substring(0, config.indexOf(projects[0]));
				
				for(int i=0; i<projects.length; i++) {
					String buggyPath = prefix + projects[i] + "/" + bugNum[i] + "/bug";
					String fixPath = prefix + projects[i] + "/" + bugNum[i] + "/fix";
					TrialGenerator generator = new TrialGenerator();
					List<EmpiricalTrial> trials = generator.generateTrials(buggyPath, fixPath, false, false);
					
					System.out.println("all the trials");
					for(int j=0; j<trials.size(); j++) {
						System.out.println("Trial " + (j+1));
						System.out.println(trials.get(j));
					}
				}
				
				return Status.OK_STATUS;
			}
		};
		
		job.schedule();
		
		return null;
	}

}
