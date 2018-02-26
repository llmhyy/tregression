package tregression.empiricalstudy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.recommendation.DebugState;
import microbat.recommendation.UserFeedback;
import microbat.util.Settings;
import sav.strategies.dto.AppJavaClassPath;
import tregression.SimulationFailException;
import tregression.empiricalstudy.solutionpattern.PatternIdentifier;
import tregression.io.RegressionRecorder;
import tregression.model.PairList;
import tregression.model.StepOperationTuple;
import tregression.separatesnapshots.AppClassPathInitializer;
import tregression.separatesnapshots.DiffMatcher;
import tregression.separatesnapshots.RunningResult;
import tregression.separatesnapshots.TraceCollector0;
import tregression.tracematch.ControlPathBasedTraceMatcher;
import tregression.views.Visualizer;

public class TrialGenerator0 {
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
			boolean requireVisualization, boolean allowMultiThread, boolean isRecordDB, 
			Defects4jProjectConfig config, String testcase) {
		List<TestCase> tcList;
		EmpiricalTrial trial = null;
		TestCase workingTC = null;
		try {
			tcList = retrieveD4jFailingTestCase(buggyPath);
			
			if(testcase!=null){
				tcList = filterSpecificTestCase(testcase, tcList);
			}
			
			for (TestCase tc : tcList) {
				System.out.println("working on test case " + tc.testClass + "#" + tc.testMethod);
				workingTC = tc;

				trial = analyzeTestCase(buggyPath, fixPath, isReuse, allowMultiThread,
						tc, config, requireVisualization, true, isRecordDB);
				if(!trial.isDump()){
					break;					
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		if (trial == null) {
			trial = EmpiricalTrial.createDumpTrial("runtime exception occurs");
			trial.setTestcase(workingTC.testClass + "::" + workingTC.testMethod);
		}

		List<EmpiricalTrial> list = new ArrayList<>();
		list.add(trial);
		return list;
	}

	private List<TestCase> filterSpecificTestCase(String testcase, List<TestCase> tcList) {
		List<TestCase> filteredList = new ArrayList<>();
		for(TestCase tc: tcList){
			String tcName = tc.testClass + "#" + tc.testMethod;
			if(tcName.equals(testcase)){
				filteredList.add(tc);
			}
		}
		
		if(filteredList.isEmpty()){
			filteredList = tcList;
		}
		
		return filteredList;
	}

	private List<EmpiricalTrial> runMainMethodVersion(String buggyPath, String fixPath, boolean isReuse, boolean allowMultiThread,
			boolean requireVisualization, Defects4jProjectConfig config, TestCase tc) throws SimulationFailException {
		List<EmpiricalTrial> trials;
		generateMainMethod(buggyPath, tc, config);
		recompileD4J(buggyPath, config);
		generateMainMethod(fixPath, tc, config);
		recompileD4J(fixPath, config);
		
		trials = new ArrayList<>();
		EmpiricalTrial trial = analyzeTestCase(buggyPath, fixPath, isReuse, allowMultiThread, tc, config, requireVisualization, false, false);
		trials.add(trial);
		return trials;
	}

	private void recompileD4J(String workingPath, Defects4jProjectConfig config) {
		File pathToExecutable = new File(config.rootPath);
		ProcessBuilder builder = new ProcessBuilder(pathToExecutable.getAbsolutePath(), "compile");
		builder.directory(new File(workingPath).getAbsoluteFile() ); // this is where you set the root folder for the executable to run with
		builder.redirectErrorStream(true);
		Process process;
		try {
			process = builder.start();
			Scanner s = new Scanner(process.getInputStream());
			StringBuilder text = new StringBuilder();
			while (s.hasNextLine()) {
				text.append(s.nextLine());
				text.append("\n");
			}
			s.close();
			
			int result = process.waitFor();
			
			System.out.printf( "Process exited with result %d and output %s%n", result, text );
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}

		
	}

	private void generateMainMethod(String workingPath, TestCase tc, Defects4jProjectConfig config) {
		MainMethodGenerator generator = new MainMethodGenerator();
		AppJavaClassPath appCP = AppClassPathInitializer.initialize(workingPath, tc, config);
		String relativePath = tc.testClass.replace(".", File.separator) + ".java";
		String sourcePath = appCP.getTestCodePath() + File.separator + relativePath;
		
		generator.generateMainMethod(sourcePath, tc);
		System.currentTimeMillis();
	}
	
	private EmpiricalTrial analyzeTestCase(String buggyPath, String fixPath, boolean isReuse, boolean allowMultiThread, 
			TestCase tc, Defects4jProjectConfig config, 
			boolean requireVisualization, boolean isRunInTestCaseMode, boolean isRecordDB) throws SimulationFailException {
		TraceCollector0 buggyCollector = new TraceCollector0();
		TraceCollector0 correctCollector = new TraceCollector0();
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
//			System.out.println("finish matching trace, taking " + matchTime + "ms");
//			cachedDiffMatcher = diffMatcher;
//			cachedPairList = pairList;

			diffMatcher = cachedDiffMatcher;
			pairList = cachedPairList;
		} else {
			Settings.compilationUnitMap.clear();
			Settings.iCompilationUnitMap.clear();
			buggyRS = buggyCollector.run(buggyPath, tc, config, isRunInTestCaseMode, allowMultiThread);
			if (buggyRS.getRunningType() != NORMAL) {
				EmpiricalTrial trial = EmpiricalTrial.createDumpTrial(getProblemType(buggyRS.getRunningType()));
				return trial;
			}

			Settings.compilationUnitMap.clear();
			Settings.iCompilationUnitMap.clear();
			correctRs = correctCollector.run(fixPath, tc, config, isRunInTestCaseMode, allowMultiThread);
			if (correctRs.getRunningType() != NORMAL) {
				EmpiricalTrial trial = EmpiricalTrial.createDumpTrial(getProblemType(correctRs.getRunningType()));
				return trial;
			}
			
			if (buggyRS != null && correctRs != null) {
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
				System.out.println("finish matching trace, taking " + matchTime + "ms");

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
		Simulator simulator = new Simulator();
		simulator.prepare(buggyTrace, correctTrace, pairList, diffMatcher);
		addAdditionalObservedFault(simulator, config, buggyTrace);

		RootCauseFinder rootcauseFinder = new RootCauseFinder();
		rootcauseFinder.getRootCauseBasedOnDefects4J(pairList, diffMatcher, buggyTrace, correctTrace);
		
		if(rootcauseFinder.getRealRootCaseList().isEmpty()){
			EmpiricalTrial trial = EmpiricalTrial.createDumpTrial("cannot find real root cause");
			if(buggyTrace.isMultiThread() || correctTrace.isMultiThread()){
				trial.setMultiThread(true);
				StepOperationTuple tuple = new StepOperationTuple(simulator.getObservedFault(), 
						new UserFeedback(UserFeedback.UNCLEAR), simulator.getObservedFault(), DebugState.UNCLEAR);
				trial.getCheckList().add(tuple);
			}
			
			return trial;
		}
		
		if(simulator.getObservedFault()==null){
			EmpiricalTrial trial = EmpiricalTrial.createDumpTrial("cannot find observable fault");
			return trial;
		}
		
		List<EmpiricalTrial> trials0 = simulator.detectMutatedBug(buggyTrace, correctTrace, diffMatcher, 0);

		time2 = System.currentTimeMillis();
		int simulationTime = (int) (time2 - time1);
		System.out.println("finish simulating debugging, taking " + simulationTime / 1000 + "s");
		
		
		for (EmpiricalTrial trial : trials0) {
			trial.setTestcase(tc.testClass + "#" + tc.testMethod);
			trial.setTraceCollectionTime(buggyTrace.getConstructTime() + correctTrace.getConstructTime());
			trial.setTraceMatchTime(matchTime);
			trial.setBuggyTrace(buggyTrace);
			trial.setFixedTrace(correctTrace);
			trial.setPairList(pairList);
			trial.setDiffMatcher(diffMatcher);
			
			PatternIdentifier identifier = new PatternIdentifier();
			identifier.identifyPattern(trial);
		}

		EmpiricalTrial trial = trials0.get(0);
		
		//TODO generate a folder with a name of the regression, and store the traces
//		if(isRecordDB){
//			DBRecording dbRecording = new DBRecording(trial, buggyTrace, correctTrace, diffMatcher, pairList, config);
//			new Thread(dbRecording).start();
//		}
		
		return trial;
	}
	
	public class DBRecording implements Runnable{

		EmpiricalTrial trial;
		Trace buggyTrace;
		Trace correctTrace;
		DiffMatcher diffMatcher;
		PairList pairList;
		Defects4jProjectConfig config;
		
		public DBRecording(EmpiricalTrial trial, Trace buggyTrace, Trace correctTrace, DiffMatcher diffMatcher,
				PairList pairList, Defects4jProjectConfig config) {
			super();
			this.trial = trial;
			this.buggyTrace = buggyTrace;
			this.correctTrace = correctTrace;
			this.diffMatcher = diffMatcher;
			this.pairList = pairList;
			this.config = config;
		}



		@Override
		public void run() {
			try {
				new RegressionRecorder().record(trial, buggyTrace, correctTrace, pairList, config.projectName, 
						String.valueOf(config.bugID));
			} catch (SQLException e) {
				e.printStackTrace();
			}	
			
		}
		
	}

	private void addAdditionalObservedFault(Simulator simulator, Defects4jProjectConfig config,
			Trace buggyTrace) {
		if(config.projectName.equals("Chart")&&config.bugID==6){
			TraceNode node = buggyTrace.getExecutionList().get(297);
			simulator.setObservedFault(node);
		}
		
		if(config.projectName.equals("Math")&&config.bugID==78){
			TraceNode node = buggyTrace.getExecutionList().get(640);
			simulator.setObservedFault(node);
		}
		
		if(config.projectName.equals("Math")&&config.bugID==40){
			TraceNode node = buggyTrace.getExecutionList().get(11268);
			simulator.setObservedFault(node);
		}
		
		if(config.projectName.equals("Math")&&config.bugID==49){
			TraceNode node = buggyTrace.getExecutionList().get(449);
			simulator.setObservedFault(node);
		}
		
		if(config.projectName.equals("Math")&&config.bugID==1){
			TraceNode node = buggyTrace.getExecutionList().get(1478);
			simulator.setObservedFault(node);
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
