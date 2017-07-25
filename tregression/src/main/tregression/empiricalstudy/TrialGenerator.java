package tregression.empiricalstudy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import microbat.model.trace.Trace;
import microbat.util.Settings;
import tregression.SimulationFailException;
import tregression.model.PairList;
import tregression.separatesnapshots.DiffMatcher;
import tregression.separatesnapshots.RunningResult;
import tregression.separatesnapshots.TraceCollector;
import tregression.tracematch.ControlPathBasedTraceMatcher;
import tregression.views.Visualizer;

public class TrialGenerator {
	private RunningResult cachedBuggyRS;
	private RunningResult cachedCorrectRS;
	
	public List<EmpiricalTrial> generateTrials(String buggyPath, String fixPath, boolean isReuse, boolean requireVisualization){
		List<EmpiricalTrial> trials = new ArrayList<>();
		TraceCollector collector = new TraceCollector();
		
		try {
			TestCase tc = retrieveD4jFailingTestCase(buggyPath);
			
			RunningResult buggyRS;
			RunningResult correctRs;
			if(cachedBuggyRS!=null && cachedCorrectRS!=null && isReuse){
				buggyRS = cachedBuggyRS;
				correctRs = cachedCorrectRS;
			}
			else{
				Settings.compilationUnitMap.clear();
				buggyRS = collector.run(buggyPath, tc.testClass, tc.testMethod);
				
				Settings.compilationUnitMap.clear();
				correctRs = collector.run(fixPath, tc.testClass, tc.testMethod);
				
				cachedBuggyRS = buggyRS;
				cachedCorrectRS = correctRs;
			}
			
			DiffMatcher diffMatcher = new DiffMatcher("source", "tests", buggyPath, fixPath);
			diffMatcher.matchCode();
			
			ControlPathBasedTraceMatcher traceMatcher = new ControlPathBasedTraceMatcher();
			PairList pairList = traceMatcher.matchTraceNodePair(buggyRS.getRunningTrace(), 
					correctRs.getRunningTrace(), diffMatcher); 
			
			Trace buggyTrace = buggyRS.getRunningTrace();
			Trace correctTrace = correctRs.getRunningTrace();
			
			if(requireVisualization) {
				Visualizer visualizer = new Visualizer();
				visualizer.visualize(buggyTrace, correctTrace, pairList, diffMatcher);
			}
			
			SimulatorWithCompilcatedModification simulator = new SimulatorWithCompilcatedModification();
			simulator.prepare(buggyTrace, correctTrace, pairList, diffMatcher);
			
			trials = simulator.detectMutatedBug(buggyTrace, correctTrace, diffMatcher, 0);
			
		} catch (IOException | SimulationFailException e) {
			e.printStackTrace();
		}
		
		return trials;
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
