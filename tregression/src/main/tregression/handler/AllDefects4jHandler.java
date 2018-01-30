package tregression.handler;

import java.io.File;
import java.io.IOException;
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
import tregression.empiricalstudy.TrialRecorder;
import tregression.preference.TregressionPreference;

public class AllDefects4jHandler extends AbstractHandler {
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Job job = new Job("Do evaluation") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				
				int skippedNum = 0;
				int endNum = 500;
				
//				String[] projects = {"Chart", "Closure", "Lang", "Math", "Mockito", "Time"};
//				int[] bugNum = {26, 133, 65, 106, 38, 27};
				
				String[] projects = {"Chart", "Lang", "Math", "Time"};
				int[] bugNum = {26, 65, 106, 27};
				
//				String[] projects = {"Lang"};
//				int[] bugNum = {65};
				
//				String[] projects = {"Chart"};
//				int[] bugNum = {26};
				
				String prefix = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.REPO_PATH) + File.separator;
				
				int count = 0;
				for(int i=0; i<projects.length; i++) {
					
					for(int j=1; j<=bugNum[i]; j++) {
						
						count++;
						if(count <= skippedNum || count > endNum) {
							continue;
						}
						
						
						
						System.out.println("working on the " + j + "th bug of " + projects[i] + " project.");
						
						String buggyPath = prefix + projects[i] + File.separator + j + File.separator + "bug";
						String fixPath = prefix + projects[i] + File.separator + j + File.separator + "fix";
						
						System.out.println("analyzing the " + j + "th bug in " + projects[i] + " project.");
						
						TrialGenerator generator = new TrialGenerator();
						Defects4jProjectConfig d4jConfig = Defects4jProjectConfig.getD4JConfig(projects[i], j);
						List<EmpiricalTrial> trials = generator.generateTrials(buggyPath, fixPath, 
								false, false, true, true, d4jConfig, null);
						
						TrialRecorder recorder;
						try {
							recorder = new TrialRecorder();
							recorder.export(trials, projects[i], j);
						} catch (IOException e) {
							e.printStackTrace();
						}
						
					}
					
					
//					System.out.println("all the trials");
//					for(int j=0; j<trials.size(); j++) {
//						System.out.println("Trial " + (j+1));
//						System.out.println(trials.get(j));
//					}
				}
				
				return Status.OK_STATUS;
			}
		};
		
		job.schedule();
		
		return null;
	}
	
	

}
