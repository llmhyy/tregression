package tregression.empiricalstudy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.INVOKEINTERFACE;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;

import microbat.codeanalysis.bytecode.ByteCodeParser;
import microbat.codeanalysis.bytecode.MethodFinderByLine;
import microbat.codeanalysis.runtime.PreCheckInformation;
import microbat.model.BreakPoint;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.preference.AnalysisScopePreference;
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
	public static final int OVER_LONG_INSTRUMENTATION_METHOD = 5;
	public static final int EXPECTED_STEP_NOT_MET = 6;

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
		case OVER_LONG_INSTRUMENTATION_METHOD:
			return "over long instrumented byte code method";
		case EXPECTED_STEP_NOT_MET:
			return "expected steps are not met";
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
		TraceCollector0 buggyCollector = new TraceCollector0(true);
		TraceCollector0 correctCollector = new TraceCollector0(false);
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
			
			EmpiricalTrial trial = simulateDebuggingWithCatchedObjects(buggyRS.getRunningTrace(), 
					correctRs.getRunningTrace(), pairList, diffMatcher, requireVisualization);
			return trial;
		} else {
			
			int trialLimit = 10;
			int trialNum = 0;
			boolean isDataFlowComplete = false;
			EmpiricalTrial trial = null;
			List<String> includedClassNames = AnalysisScopePreference.getIncludedLibList();
			List<String> excludedClassNames = AnalysisScopePreference.getExcludedLibList();
			
			while(!isDataFlowComplete && trialNum<trialLimit){
				trialNum++;
				
				Settings.compilationUnitMap.clear();
				Settings.iCompilationUnitMap.clear();
				buggyRS = buggyCollector.run(buggyPath, tc, config, isRunInTestCaseMode, 
						allowMultiThread, includedClassNames, excludedClassNames);
				if (buggyRS.getRunningType() != NORMAL) {
					trial = EmpiricalTrial.createDumpTrial(getProblemType(buggyRS.getRunningType()));
					return trial;
				}

				Settings.compilationUnitMap.clear();
				Settings.iCompilationUnitMap.clear();
				correctRs = correctCollector.run(fixPath, tc, config, isRunInTestCaseMode, 
						allowMultiThread, includedClassNames, excludedClassNames);
				if (correctRs.getRunningType() != NORMAL) {
					trial = EmpiricalTrial.createDumpTrial(getProblemType(correctRs.getRunningType()));
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
				
				Trace buggyTrace = buggyRS.getRunningTrace();
				Trace correctTrace = correctRs.getRunningTrace();
				
				RootCauseFinder rootcauseFinder = new RootCauseFinder();
				rootcauseFinder.setRootCauseBasedOnDefects4J(pairList, diffMatcher, buggyTrace, correctTrace);
				
				Simulator simulator = new Simulator();
				simulator.prepare(buggyTrace, correctTrace, pairList, diffMatcher);
				if(rootcauseFinder.getRealRootCaseList().isEmpty()){
					trial = EmpiricalTrial.createDumpTrial("cannot find real root cause");
					if(buggyTrace.isMultiThread() || correctTrace.isMultiThread()){
						trial.setMultiThread(true);
						StepOperationTuple tuple = new StepOperationTuple(simulator.getObservedFault(), 
								new UserFeedback(UserFeedback.UNCLEAR), simulator.getObservedFault(), DebugState.UNCLEAR);
						trial.getCheckList().add(tuple);
					}
					
					return trial;
				}
				
				if(simulator.getObservedFault()==null){
					trial = EmpiricalTrial.createDumpTrial("cannot find observable fault");
					return trial;
				}
				
				rootcauseFinder.checkRootCause(simulator.getObservedFault(), buggyTrace, correctTrace, pairList, diffMatcher);
				TraceNode rootCause = rootcauseFinder.retrieveRootCause(pairList, diffMatcher, buggyTrace, correctTrace);
				
				if(rootCause==null){
					List<TraceNode> buggySteps = rootcauseFinder.getStopStepsOnBuggyTrace();
					List<TraceNode> correctSteps = rootcauseFinder.getStopStepsOnCorrectTrace();
					
					List<String> newIncludedClassNames = new ArrayList<>();
					List<String> newIncludedBuggyClassNames = identifyIncludedClassNames(buggySteps, buggyRS.getPrecheckInfo(), rootcauseFinder.getRegressionNodeList());
					List<String> newIncludedCorrectClassNames = identifyIncludedClassNames(correctSteps, correctRs.getPrecheckInfo(), rootcauseFinder.getCorrectNodeList());
					newIncludedClassNames.addAll(newIncludedBuggyClassNames);
					newIncludedClassNames.addAll(newIncludedCorrectClassNames);
					boolean includedClassChanged = false;
					for(String name: newIncludedClassNames){
						if(!includedClassNames.contains(name)){
							includedClassNames.add(name);
							includedClassChanged = true;
						}
					}
					
					if(!includedClassChanged) {
						trialNum = trialLimit + 1;
					}
					else {
						continue;						
					}
				}
				
				if (requireVisualization) {
					Visualizer visualizer = new Visualizer();
					visualizer.visualize(buggyTrace, correctTrace, pairList, diffMatcher);
				}
				
				isDataFlowComplete = true;
				System.out.println("start simulating debugging...");
				List<EmpiricalTrial> trials0 = simulator.detectMutatedBug(buggyTrace, correctTrace, diffMatcher, 0);
				time2 = System.currentTimeMillis();
				int simulationTime = (int) (time2 - time1);
				System.out.println("finish simulating debugging, taking " + simulationTime / 1000 + "s");
				
				for (EmpiricalTrial t : trials0) {
					t.setTestcase(tc.testClass + "#" + tc.testMethod);
					t.setTraceCollectionTime(buggyTrace.getConstructTime() + correctTrace.getConstructTime());
					t.setTraceMatchTime(matchTime);
					t.setBuggyTrace(buggyTrace);
					t.setFixedTrace(correctTrace);
					t.setPairList(pairList);
					t.setDiffMatcher(diffMatcher);
					
					PatternIdentifier identifier = new PatternIdentifier();
					identifier.identifyPattern(t);
				}

				trial = trials0.get(0);
				return trial;
			}

		}

		return null;
	}
	
	private EmpiricalTrial simulateDebuggingWithCatchedObjects(Trace buggyTrace, Trace correctTrace, PairList pairList,
			DiffMatcher diffMatcher, boolean requireVisualization) throws SimulationFailException {
		Simulator simulator = new Simulator();
		simulator.prepare(buggyTrace, correctTrace, pairList, diffMatcher);
		RootCauseFinder rootcauseFinder = new RootCauseFinder();
		rootcauseFinder.setRootCauseBasedOnDefects4J(pairList, diffMatcher, buggyTrace, correctTrace);
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
		
		System.out.println("start simulating debugging...");
		long time1 = System.currentTimeMillis();
		List<EmpiricalTrial> trials0 = simulator.detectMutatedBug(buggyTrace, correctTrace, diffMatcher, 0);
		long time2 = System.currentTimeMillis();
		int simulationTime = (int) (time2 - time1);
		System.out.println("finish simulating debugging, taking " + simulationTime / 1000 + "s");
		
		if (requireVisualization) {
			Visualizer visualizer = new Visualizer();
			visualizer.visualize(buggyTrace, correctTrace, pairList, diffMatcher);
		}
		
		for (EmpiricalTrial t : trials0) {
			t.setTraceCollectionTime(buggyTrace.getConstructTime() + correctTrace.getConstructTime());
			t.setBuggyTrace(buggyTrace);
			t.setFixedTrace(correctTrace);
			t.setPairList(pairList);
			t.setDiffMatcher(diffMatcher);
			
			PatternIdentifier identifier = new PatternIdentifier();
			identifier.identifyPattern(t);
		}

		EmpiricalTrial trial = trials0.get(0);
		return trial;
	}

	private List<String> identifyIncludedClassNames(List<TraceNode> stopSteps,
			PreCheckInformation precheckInfo, List<TraceNode> visitedSteps) {
		
		List<BreakPoint> parsedBreakPoints = new ArrayList<>();
		List<String> classes = new ArrayList<>();
		
		for(TraceNode stopStep: stopSteps){
			AppJavaClassPath appClassPath = stopStep.getTrace().getAppJavaClassPath();
			
			List<TraceNode> range = identifyEnhanceRange(stopStep, visitedSteps);
			range.add(stopStep);
			
			for(TraceNode rangeStep: range) {
				BreakPoint point = rangeStep.getBreakPoint();
				if(parsedBreakPoints.contains(point)){
					continue;
				}
				parsedBreakPoints.add(point);
				
				String clazz = point.getClassCanonicalName();
				
				MethodFinderByLine finder = new MethodFinderByLine(point);
				ByteCodeParser.parse(clazz, finder, appClassPath);
				Method method = finder.getMethod();
				List<InstructionHandle> insList = finder.getHandles();
				
				List<String> visitedLibClasses = findInvokedLibClasses(rangeStep, insList, method, precheckInfo);
				for(String str: visitedLibClasses){
					if(!classes.contains(str)){
						classes.add(str);
					}
				}
			}
		}
		
		return classes;
	}

	private List<TraceNode> identifyEnhanceRange(TraceNode stopStep, List<TraceNode> visitedSteps){
		TraceNode closetStep = findClosestStep(stopStep, visitedSteps);
		List<TraceNode> list = new ArrayList<>();
		
		if(closetStep==null){
			return list;
		}
		
		Trace trace = stopStep.getTrace();
		for(int i=closetStep.getOrder(); i>stopStep.getOrder(); i--) {
			TraceNode step = trace.getTraceNode(i);
			list.add(step);
		}
		
		return list;
	}
	
	private TraceNode findClosestStep(TraceNode stopStep, List<TraceNode> visitedSteps) {
		TraceNode closestStep = null;
		int distance = -1;
		for(TraceNode step: visitedSteps) {
			if(step.getOrder()>stopStep.getOrder()) {
				if(closestStep==null) {
					closestStep = step;
					distance = step.getOrder() - stopStep.getOrder();
				}
				else {
					int newDis = step.getOrder() - stopStep.getOrder();
					if(newDis < distance) {
						closestStep = step;
						distance = newDis;
					}
				}
			}
		}
		
		return closestStep;
	}

	private List<String> findInvokedLibClasses(TraceNode step, List<InstructionHandle> insList, Method method,
			PreCheckInformation precheckInfo) {
		List<String> list = new ArrayList<>();
		if(step.getInvocationChildren().isEmpty()){
			ConstantPoolGen cGen = new ConstantPoolGen(method.getConstantPool());
			for(InstructionHandle handle: insList){
				Instruction ins = handle.getInstruction();
				if(ins instanceof InvokeInstruction){
					InvokeInstruction iIns = (InvokeInstruction)ins;
					String className = iIns.getClassName(cGen);
					
					appendSuperClass(className, step.getTrace().getAppJavaClassPath(), list);
					
					if(!list.contains(className)){
						list.add(className);
					}	
					
					//add implementation class
					if(ins instanceof INVOKEINTERFACE) {
						List<String> loadedClassStrings = precheckInfo.getLoadedClasses();
						List<String> implementations = findImplementation(className, 
								loadedClassStrings, step.getTrace().getAppJavaClassPath());
						
						for(String implementation: implementations) {
							list.add(implementation);
							appendSuperClass(className, step.getTrace().getAppJavaClassPath(), list);
						}
					}
					
				}
			}
			
		}
		
		return list;
	}
	
	private void appendSuperClass(String className, AppJavaClassPath appPath, List<String> includedClasses){
		JavaClass javaClazz = ByteCodeParser.parse(className, appPath);
		if(javaClazz==null){
			return;
		}
		
		try {
			for(JavaClass superClass: javaClazz.getSuperClasses()){
				if(!superClass.getClassName().equals("java.lang.Object")){
					if(!includedClasses.contains(superClass.getClassName())){
						includedClasses.add(superClass.getClassName());
					}	
				}
			}
		} catch (ClassNotFoundException e) {
		}
	}

	private List<String> findImplementation(String className, List<String> loadedClassStrings,
			AppJavaClassPath appClassPath) {
		List<String> list = new ArrayList<>();
		for(String loadedClassString: loadedClassStrings) {
			if(loadedClassString.contains("microbat") || loadedClassString.contains("sav.common")
					|| loadedClassString.contains("sun.reflect")
					|| loadedClassString.contains("com.sun")) {
				continue;
			}
			
			JavaClass javaClass = ByteCodeParser.parse(loadedClassString, appClassPath);
			if(javaClass!=null) {
				try {
					for(JavaClass interfaze: javaClass.getAllInterfaces()) {
						if(interfaze.getClassName().equals(className)) {
							list.add(loadedClassString);
							break;
						}
					}
				} catch (ClassNotFoundException e) {
//					e.printStackTrace();
				}
				
			}
		}
		return list;
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
