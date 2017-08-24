package tregression.empiricalstudy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.util.Settings;
import tregression.SimulationFailException;
import tregression.model.PairList;
import tregression.model.StepOperationTuple;
import tregression.separatesnapshots.DiffMatcher;
import tregression.separatesnapshots.RunningResult;
import tregression.separatesnapshots.TraceCollector;
import tregression.tracematch.ControlPathBasedTraceMatcher;
import tregression.views.Visualizer;

public class TrialGenerator {
	public static final int NORMAL = 0;
	public static final int OVER_LONG = 1;
	public static final int MULTI_THREAD = 2;
	public static final int INSUFFICIENT_TRACE = 3;
	public static final int SAME_LENGTH = 4;

	private RunningResult cachedBuggyRS;
	private RunningResult cachedCorrectRS;

	private DiffMatcher cachedDiffMatcher;
	private PairList cachedPairList;

	private String getProblemType(int type) {
		switch (type) {
		case OVER_LONG:
			return "some trace is over long";
		case MULTI_THREAD:
			return "it's a multi-thread program";
		case INSUFFICIENT_TRACE:
			return "the trace is insufficient";
		case SAME_LENGTH:
			return "two traces are of the same length";
		default:
			break;
		}
		return "I don't know";
	}

	public List<EmpiricalTrial> generateTrials(String buggyPath, String fixPath, boolean isReuse,
			boolean requireVisualization, Defects4jProjectConfig config) {
		List<TestCase> list;
		List<EmpiricalTrial> trials = new ArrayList<>();
		TestCase workingTC = null;
		try {
			list = retrieveD4jFailingTestCase(buggyPath);
			for (TestCase tc : list) {
				System.out.println("working on test case " + tc.testClass + "::" + tc.testMethod);
				workingTC = tc;

				int res = analyzeTestCase(buggyPath, fixPath, isReuse, trials, tc, config, requireVisualization);
				if (res == NORMAL) {
					return trials;
				} else {
					String explanation = getProblemType(res);
					System.out.println("[*NOTICE*] " + explanation);
					EmpiricalTrial trial = new EmpiricalTrial(-1, -1, null, null, null, 0, 0, 0, -1, -1, null, null, 0);
					trial.setTestcase(tc.testClass + "::" + tc.testMethod);
					trial.setExceptionExplanation(explanation);
					trials.add(trial);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		if (trials.isEmpty()) {
			EmpiricalTrial trial = new EmpiricalTrial(-1, -1, null, null, new ArrayList<StepOperationTuple>(), 0, 0, 0,
					-1, -1, null, null, 0);
			trial.setExceptionExplanation("runtime exception occurs");
			trial.setTestcase(workingTC.testClass + "::" + workingTC.testMethod);
			trials.add(trial);
		}

		return trials;
	}

	private int analyzeTestCase(String buggyPath, String fixPath, boolean isReuse, List<EmpiricalTrial> trials,
			TestCase tc, Defects4jProjectConfig config, boolean requireVisualization) throws SimulationFailException {
		TraceCollector collector = new TraceCollector();
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

//			System.out.println("start matching trace..., buggy trace length: " + buggyRS.getRunningTrace().size()
//					+ ", correct trace length: " + correctRs.getRunningTrace().size());
//			time1 = System.currentTimeMillis();
//			diffMatcher = new DiffMatcher(config.srcSourceFolder, config.srcTestFolder, buggyPath, fixPath);
//			diffMatcher.matchCode();
//
//			ControlPathBasedTraceMatcher traceMatcher = new ControlPathBasedTraceMatcher();
//			pairList = traceMatcher.matchTraceNodePair(buggyRS.getRunningTrace(), correctRs.getRunningTrace(),
//					diffMatcher);
//			time2 = System.currentTimeMillis();
//			matchTime = (int) (time2 - time1);
//			System.out.println("finish matching trace, taking " + matchTime / 1000 + "s");
//			cachedDiffMatcher = diffMatcher;
//			cachedPairList = pairList;

			diffMatcher = cachedDiffMatcher;
			pairList = cachedPairList;
		} else {
			Settings.compilationUnitMap.clear();
			Settings.iCompilationUnitMap.clear();
			buggyRS = collector.preCheck(buggyPath, tc.testClass, tc.testMethod, config);
			if (buggyRS.getRunningType() != NORMAL) {
				return buggyRS.getRunningType();
			}

			Settings.compilationUnitMap.clear();
			Settings.iCompilationUnitMap.clear();
			correctRs = collector.preCheck(fixPath, tc.testClass, tc.testMethod, config);
			if (correctRs.getRunningType() != NORMAL) {
				return correctRs.getRunningType();
			}

			if (buggyRS != null && correctRs != null) {
				Settings.compilationUnitMap.clear();
				buggyRS = collector.run(buggyRS);
				Settings.compilationUnitMap.clear();
				correctRs = collector.run(correctRs);

				cachedBuggyRS = buggyRS;
				cachedCorrectRS = correctRs;

				System.out.println("start matching trace..., buggy trace length: " + buggyRS.getRunningTrace().size()
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
		addAdditionalObservedFault(simulator, config, buggyTrace);

		RootCauseNode realcauseNode = new RootCauseFinder().getRootCauseBasedOnDefects4J(pairList, diffMatcher, buggyTrace,
				correctTrace);
		if (realcauseNode == null) {
			return INSUFFICIENT_TRACE;
		}

		List<EmpiricalTrial> trials0 = simulator.detectMutatedBug(buggyTrace, correctTrace, diffMatcher, 0);

		time2 = System.currentTimeMillis();
		int simulationTime = (int) (time2 - time1);
		System.out.println("finish simulating debugging, taking " + simulationTime / 1000 + "s");

		if (trials0 != null) {
			for (EmpiricalTrial trial : trials0) {
				trial.setTestcase(tc.testClass + "#" + tc.testMethod);
				trial.setTraceCollectionTime(buggyTrace.getConstructTime() + correctTrace.getConstructTime());
				trial.setTraceMatchTime(matchTime);
			}

			trials.add(trials0.get(0));
			return NORMAL;
		}
		
		return INSUFFICIENT_TRACE;
	}

	private void addAdditionalObservedFault(SimulatorWithCompilcatedModification simulator, Defects4jProjectConfig config,
			Trace buggyTrace) {
		
		if(config.projectName.equals("Math")&&config.bugID==78){
			simulator.getObservedFaults().clear();
			TraceNode node = buggyTrace.getExectionList().get(640);
			simulator.getObservedFaults().add(node);
		}
		
		if(config.projectName.equals("Math")&&config.bugID==40){
			simulator.getObservedFaults().clear();
			TraceNode node = buggyTrace.getExectionList().get(11268);
			simulator.getObservedFaults().add(node);
		}
		
		if(config.projectName.equals("Math")&&config.bugID==49){
			simulator.getObservedFaults().clear();
			TraceNode node = buggyTrace.getExectionList().get(449);
			simulator.getObservedFaults().add(node);
		}
		
		if(config.projectName.equals("Math")&&config.bugID==1){
			simulator.getObservedFaults().clear();
			TraceNode node = buggyTrace.getExectionList().get(1478);
			simulator.getObservedFaults().add(node);
		}
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
