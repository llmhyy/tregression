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
			boolean requireVisualization, Defects4jProjectConfig config) {
		List<TestCase> list;
		try {
			list = retrieveD4jFailingTestCase(buggyPath);
			for(TestCase tc: list) {
				System.out.println("working on test case " + tc.testClass + "::" + tc.testMethod);
				List<EmpiricalTrial> trials = new ArrayList<>();
				boolean isNormal = analyzeTestCase(buggyPath, fixPath,isReuse, trials, tc, config, requireVisualization);
				if (isNormal) {
					return trials;
				}
				else {
					System.out.println("[NOTICE] Two traces have the same length, change a new test case.");
				}
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}

		return new ArrayList<>();
	}

	private boolean analyzeTestCase(String buggyPath, String fixPath, boolean isReuse, List<EmpiricalTrial> trials, 
			TestCase tc, Defects4jProjectConfig config, boolean requireVisualization) {
		TraceCollector collector = new TraceCollector();
		try {
			long time1 = 0;
			long time2 = 0;

			RunningResult buggyRS = null;
			RunningResult correctRs = null;

			DiffMatcher diffMatcher = null;
			PairList pairList = null;

			int matchTime = -1;

			if (cachedBuggyRS != null && cachedCorrectRS != null && isReuse) {
				buggyRS = cachedBuggyRS;
				correctRs = cachedCorrectRS;
				diffMatcher = cachedDiffMatcher;
				pairList = cachedPairList;
			} else {
				Settings.compilationUnitMap.clear();
				buggyRS = collector.preCheck(buggyPath, tc.testClass, tc.testMethod, config);

				if (buggyRS != null) {
					Settings.compilationUnitMap.clear();
					correctRs = collector.preCheck(fixPath, tc.testClass, tc.testMethod, config);
				}
				
				if (correctRs != null && buggyRS.getChecker().getStepNum()==correctRs.getChecker().getStepNum()) {
					return false;
				}
				
				Settings.compilationUnitMap.clear();
				buggyRS = collector.run(buggyRS);
				
				Settings.compilationUnitMap.clear();
				correctRs = collector.run(correctRs);

				if (buggyRS != null && correctRs != null) {
					cachedBuggyRS = buggyRS;
					cachedCorrectRS = correctRs;

					System.out
							.println("start matching trace..., buggy trace length: " + buggyRS.getRunningTrace().size()
									+ ", correct trace length: " + correctRs.getRunningTrace().size());
					time1 = System.currentTimeMillis();
					diffMatcher = new DiffMatcher(config.srcSourceFolder, config.srcTestFolder, buggyPath, fixPath);
					diffMatcher.matchCode();

					ControlPathBasedTraceMatcher traceMatcher = new ControlPathBasedTraceMatcher();
					pairList = traceMatcher.matchTraceNodePair(buggyRS.getRunningTrace(), correctRs.getRunningTrace(),
							diffMatcher);
					time2 = System.currentTimeMillis();
					matchTime = (int) (time2 - time1);
					System.out.println("finish matching trace, taking " + matchTime / 1000 + "s");

					cachedDiffMatcher = diffMatcher;
					cachedPairList = pairList;
				}

			}

			if (buggyRS != null && correctRs != null) {
				Trace buggyTrace = buggyRS.getRunningTrace();
				Trace correctTrace = correctRs.getRunningTrace();

				if (requireVisualization) {
					Visualizer visualizer = new Visualizer();
					visualizer.visualize(buggyTrace, correctTrace, pairList, diffMatcher);
				}

				time1 = System.currentTimeMillis();
				System.out.println("start simulating debugging...");
				SimulatorWithCompilcatedModification simulator = new SimulatorWithCompilcatedModification();
				simulator.prepare(buggyTrace, correctTrace, pairList, diffMatcher);

				List<EmpiricalTrial> trials0 = simulator.detectMutatedBug(buggyTrace, correctTrace, diffMatcher, 0);
				time2 = System.currentTimeMillis();
				int simulationTime = (int) (time2 - time1);
				System.out.println("finish simulating debugging, taking " + simulationTime / 1000 + "s");

				for (EmpiricalTrial trial : trials0) {
					trial.setTraceCollectionTime(buggyTrace.getConstructTime() + correctTrace.getConstructTime());
					trial.setTraceMatchTime(matchTime);
					trial.setSimulationTime(simulationTime);
				}
				
				trials.addAll(trials0);
			}
		} catch (SimulationFailException e) {
			e.printStackTrace();
		}
		
		return true;
	}

	class TestCase {
		public String testClass;
		public String testMethod;

		public TestCase(String testClass, String testMethod) {
			super();
			this.testClass = testClass;
			this.testMethod = testMethod;
		}
	}

	public List<TestCase> retrieveD4jFailingTestCase(String buggyVersionPath) throws IOException {
		String failingFile = buggyVersionPath + File.separator + "failing_tests";
		File file = new File(failingFile);

		BufferedReader reader = new BufferedReader(new FileReader(file));

		List<TestCase> list = new ArrayList<>();
		String line = null;
		while ((line = reader.readLine()) != null) {
			if (line.startsWith("---")) {
				String testClass = line.substring(line.indexOf(" ") + 1, line.indexOf("::"));
				String testMethod = line.substring(line.indexOf("::") + 2, line.length());
				
				TestCase tc = new TestCase(testClass, testMethod);
				list.add(tc);
			}
			
		}

		reader.close();

		return list;
	}
}
