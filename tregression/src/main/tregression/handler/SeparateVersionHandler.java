package tregression.handler;

import java.io.File;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import microbat.Activator;
import tregression.empiricalstudy.Defects4jProjectConfig;
import tregression.empiricalstudy.EmpiricalTrial;
import tregression.empiricalstudy.TrialGenerator;
import tregression.preference.TregressionPreference;

public class SeparateVersionHandler extends AbstractHandler{

	private TrialGenerator generator = new TrialGenerator();
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Job job = new Job("Do evaluation") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				String buggyPath = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.BUGGY_PATH);
				String fixPath = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.CORRECT_PATH);
				
				int startIndex = buggyPath.indexOf(File.separator, buggyPath.indexOf("bug_repo")+1);
				String projectName = buggyPath.substring(startIndex+1, buggyPath.indexOf(File.separator, startIndex+1));
				
				startIndex = buggyPath.indexOf(File.separator, buggyPath.indexOf(projectName)+1);
				String id = buggyPath.substring(startIndex+1, buggyPath.indexOf(File.separator, startIndex+1));
				
				System.out.println("working on the " + id + "th bug of " + projectName + " project.");
				Defects4jProjectConfig config = Defects4jProjectConfig.getD4JConfig(projectName, Integer.valueOf(id));
				
				List<EmpiricalTrial> trials = generator.generateTrials(buggyPath, fixPath, 
						false, true, true, config);
				
				System.out.println("all the trials");
				for(int i=0; i<trials.size(); i++) {
					System.out.println("Trial " + (i+1));
					System.out.println(trials.get(i));
				}
				
				
//				TrialRecorder recorder;
//				try {
//					recorder = new TrialRecorder();
//					recorder.export(trials, "Chart", 21);
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
				
				return Status.OK_STATUS;
			}
		};
		
		job.schedule();
		
		return null;
	}
	
	

}
