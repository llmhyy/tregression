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
	
	private DiffMatcher cachedDiffMatcher;
	private PairList cachedPairList;
	
	
	public List<EmpiricalTrial> generateTrials(String buggyPath, String fixPath, boolean isReuse, 
			boolean requireVisualization, Defects4jProjectConfig config){
		List<EmpiricalTrial> trials = new ArrayList<>();
		TraceCollector collector = new TraceCollector();
		
		try {
			TestCase tc = retrieveD4jFailingTestCase(buggyPath);
			
			long time1 = 0;
			long time2 = 0;
			
			RunningResult buggyRS = null;
			RunningResult correctRs = null;
			
			DiffMatcher diffMatcher = null;
			PairList pairList = null;
			
			if(cachedBuggyRS!=null && cachedCorrectRS!=null && isReuse){
				buggyRS = cachedBuggyRS;
				correctRs = cachedCorrectRS;
				diffMatcher = cachedDiffMatcher;
				pairList = cachedPairList;
			}
			else{
				Settings.compilationUnitMap.clear();
				buggyRS = collector.run(buggyPath, tc.testClass, tc.testMethod, config);
				
				if (buggyRS!=null) {
					Settings.compilationUnitMap.clear();
					correctRs = collector.run(fixPath, tc.testClass, tc.testMethod, config);
				}
				
				
				if (buggyRS!=null && correctRs!=null) {
					cachedBuggyRS = buggyRS;
					cachedCorrectRS = correctRs;
					
					System.out.println("start matching trace..., buggy trace length: " + buggyRS.getRunningTrace().size()
							+ ", correct trace length: " + correctRs.getRunningTrace().size());
					time1 = System.currentTimeMillis();
					diffMatcher = new DiffMatcher(config.srcSourceFolder, config.srcTestFolder, buggyPath, fixPath);
					diffMatcher.matchCode();
					
					ControlPathBasedTraceMatcher traceMatcher = new ControlPathBasedTraceMatcher();
					pairList = traceMatcher.matchTraceNodePair(buggyRS.getRunningTrace(), 
							correctRs.getRunningTrace(), diffMatcher); 
					time2 = System.currentTimeMillis();
					System.out.println("finish matching trace, taking " + (time2-time1)/1000 + "s");
					
					cachedDiffMatcher = diffMatcher;
					cachedPairList = pairList;
				}
				
			}
			
			if (buggyRS!=null && correctRs!=null) {
				Trace buggyTrace = buggyRS.getRunningTrace();
				Trace correctTrace = correctRs.getRunningTrace();
				
				if(requireVisualization) {
					Visualizer visualizer = new Visualizer();
					visualizer.visualize(buggyTrace, correctTrace, pairList, diffMatcher);
				}
				
				time1 = System.currentTimeMillis();
				System.out.println("start simulating debugging...");
				SimulatorWithCompilcatedModification simulator = new SimulatorWithCompilcatedModification();
				simulator.prepare(buggyTrace, correctTrace, pairList, diffMatcher);
				
				trials = simulator.detectMutatedBug(buggyTrace, correctTrace, diffMatcher, 0);
				time2 = System.currentTimeMillis();
				System.out.println("finish simulating debugging, taking " + (time2-time1)/1000 + "s");
			}
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
