package tregression.handler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import microbat.util.Settings;
import tregression.SimulatorWithSingleLineModification;
import tregression.model.PairList;
import tregression.separatesnapshots.DiffMatcher;
import tregression.separatesnapshots.PathConfiguration;
import tregression.separatesnapshots.RunningResult;
import tregression.separatesnapshots.TraceCollector;
import tregression.tracematch.ControlPathBasedTraceMatcher;
import tregression.tracematch.LCSBasedTraceMatcher;
import tregression.views.Visualizer;

public class SeparateVersionHandler extends AbstractHandler{

	private RunningResult cachedBuggyRS;
	private RunningResult cachedCorrectRS;
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Job job = new Job("Do evaluation") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				TraceCollector collector = new TraceCollector();
				boolean isReuse = false;
				
				PathConfiguration.buggyPath = "/mnt/linyun/bug_code/Chart/6/bug";
				PathConfiguration.fixPath = "/mnt/linyun/bug_code/Chart/6/fix";
				
				try {
					TestCase tc = retrieveD4jFailingTestCase(PathConfiguration.buggyPath);
					
					RunningResult buggyRS;
					RunningResult correctRs;
					if(cachedBuggyRS!=null && cachedCorrectRS!=null && isReuse){
						buggyRS = cachedBuggyRS;
						correctRs = cachedCorrectRS;
					}
					else{
						Settings.compilationUnitMap.clear();
						buggyRS = collector.run(PathConfiguration.buggyPath, tc.testClass, tc.testMethod);
						
						Settings.compilationUnitMap.clear();
						correctRs = collector.run(PathConfiguration.fixPath, tc.testClass, tc.testMethod);
						
						cachedBuggyRS = buggyRS;
						cachedCorrectRS = correctRs;
					}
					
					DiffMatcher diffMatcher = new DiffMatcher("source", "tests",
							PathConfiguration.buggyPath, PathConfiguration.fixPath);
					diffMatcher.matchCode();
					
//					LCSBasedTraceMatcher traceMatcher = new LCSBasedTraceMatcher();
					ControlPathBasedTraceMatcher traceMatcher = new ControlPathBasedTraceMatcher();
					PairList pairList = traceMatcher.matchTraceNodePair(buggyRS.getRunningTrace(), 
							correctRs.getRunningTrace(), diffMatcher); 
					
					Visualizer visualizer = new Visualizer();
					visualizer.visualize(buggyRS.getRunningTrace(), correctRs.getRunningTrace(), pairList, diffMatcher);
					
					
					
					
					
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				
				return Status.OK_STATUS;
			}
		};
		
		job.schedule();
		
		return null;
	}
	
	private void clearOldData(){
		Settings.interestedVariables.clear();
		Settings.wrongPathNodeOrder.clear();
		Settings.localVariableScopes.clear();
		Settings.potentialCorrectPatterns.clear();
	}

	class TestCase{
		public String testClass;
		public String testMethod;
		public TestCase(String testClass, String testMethod) {
			super();
			this.testClass = testClass;
			this.testMethod = testMethod;
		}
	}
	
	public TestCase retrieveD4jFailingTestCase(String buggyVersionPath) throws IOException{
		String failingFile = buggyVersionPath + File.separator + "failing_tests";
		File file = new File(failingFile);
		
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line = reader.readLine();
		reader.close();
		
		String testClass = line.substring(line.indexOf(" ")+1, line.indexOf("::"));
		String testMethod = line.substring(line.indexOf("::")+2, line.length());
		
		TestCase tc = new TestCase(testClass, testMethod);
		return tc;
	}

}
