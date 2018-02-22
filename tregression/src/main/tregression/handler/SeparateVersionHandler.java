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
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.recommendation.calculator.Dependency;
import microbat.recommendation.calculator.DependencyCalculator;
import microbat.recommendation.calculator.Traverse;
import microbat.recommendation.calculator.TraversingDistanceCalculator;
import tregression.empiricalstudy.DeadEndRecord;
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
				String buggyPath = PathConfiguration.getBuggyPath();
				String fixPath = PathConfiguration.getCorrectPath();
				
				String projectName = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.PROJECT_NAME);
				String id = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.BUG_ID);
				
				String testcase = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.TEST_CASE);
				
				System.out.println("working on the " + id + "th bug of " + projectName + " project.");
				Defects4jProjectConfig config = Defects4jProjectConfig.getD4JConfig(projectName, Integer.valueOf(id));
				
				List<EmpiricalTrial> trials = generator.generateTrials(buggyPath, fixPath, 
						false, true, true, false, config, testcase);
				
				System.out.println("all the trials");
				for(int i=0; i<trials.size(); i++) {
					System.out.println("Trial " + (i+1));
					System.out.println(trials.get(i));
					
					EmpiricalTrial t = trials.get(i);
					Trace trace = t.getBuggyTrace();
					for(DeadEndRecord r: t.getDeadEndRecordList()){
						TraceNode breakStep = trace.getTraceNode(r.getBreakStepOrder());
//						TraceNode occurStep = trace.getTraceNode(91);
						TraceNode occurStep = trace.getTraceNode(r.getOccurOrder());
						
						TraversingDistanceCalculator cal = new TraversingDistanceCalculator(trace.getAppJavaClassPath());
						Traverse tra = cal.calculateASTTravsingDistance(occurStep.getBreakPoint(), breakStep.getBreakPoint());
						
						DependencyCalculator dCal = new DependencyCalculator(trace.getAppJavaClassPath());
						Dependency dep = dCal.calculateDependency(occurStep.getBreakPoint(), breakStep.getBreakPoint());
						
						System.currentTimeMillis();
						break;
					}
				}
				
//				try {
//					TrialRecorder recorder = new TrialRecorder();
//					recorder.export(trials, projectName, Integer.valueOf(id));
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
