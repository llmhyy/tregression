package tregression.handler;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.bcel.Repository;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import microbat.Activator;
import microbat.agent.ExecTraceFileReader;
import microbat.codeanalysis.runtime.InstrumentationExecutor;
import microbat.model.BreakPoint;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.recommendation.DebugState;
import microbat.recommendation.UserFeedback;
import microbat.util.MicroBatUtil;
import microbat.util.Settings;
import sav.strategies.dto.AppJavaClassPath;
import tregression.SimulationFailException;
import tregression.empiricalstudy.DeadEndCSVWriter;
import tregression.empiricalstudy.DeadEndRecord;
import tregression.empiricalstudy.DeadEndReporter;
import tregression.empiricalstudy.EmpiricalTrial;
import tregression.empiricalstudy.Regression;
import tregression.empiricalstudy.RootCauseFinder;
import tregression.empiricalstudy.Simulator;
import tregression.empiricalstudy.TestCase;
import tregression.empiricalstudy.TrialGenerator;
import tregression.empiricalstudy.config.Defects4jProjectConfig;
import tregression.empiricalstudy.config.ProjectConfig;
import tregression.empiricalstudy.solutionpattern.PatternIdentifier;
import tregression.empiricalstudy.training.DED;
import tregression.model.PairList;
import tregression.model.StepOperationTuple;
import tregression.preference.TregressionPreference;
import tregression.separatesnapshots.AppClassPathInitializer;
import tregression.separatesnapshots.DiffMatcher;
import tregression.tracematch.ControlPathBasedTraceMatcher;
import tregression.views.Visualizer;

public class RegressionRetrieveHandler extends AbstractHandler {

	class Result {
		Trace buggyTrace;
		Trace correctTrace;
		PairList pairList;
		DiffMatcher diffMatcher;

		public Result(Trace cachedBuggyTrace, Trace cachedCorrectTrace, PairList cachedPairList,
				DiffMatcher cachedDiffMatcher) {
			super();
			this.buggyTrace = cachedBuggyTrace;
			this.correctTrace = cachedCorrectTrace;
			this.pairList = cachedPairList;
			this.diffMatcher = cachedDiffMatcher;
		}

	}

	Result result = null;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		Job job = new Job("Recovering Regression ...") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					String projectName = Activator.getDefault().getPreferenceStore()
							.getString(TregressionPreference.PROJECT_NAME);
					String bugId = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.BUG_ID);
					retrieveRegression(projectName, bugId);
				} catch (Exception e) {
					e.printStackTrace();
				}

				return Status.OK_STATUS;
			}
		};
		job.schedule();

		return null;
	}
	
	protected void retrieveRegression(String projectName, String bugId) throws IOException, SimulationFailException {
		boolean isReuse = false;
		System.out.println("working on the " + bugId + "th bug of " + projectName + " project.");
		Settings.compilationUnitMap.clear();
		Settings.iCompilationUnitMap.clear();

		if (!isReuse || result == null) {
			result = parseResult(projectName, bugId);
		}
		
		if (result == null) {
			return;
		}

		Visualizer visualizer = new Visualizer();
		visualizer.visualize(result.buggyTrace, result.correctTrace, result.pairList, result.diffMatcher);

		EmpiricalTrial trial = simulate(result.buggyTrace, result.correctTrace, result.pairList,
				result.diffMatcher, false, false, 3);
		System.out.println(trial);
		
//			try {
//				List<EmpiricalTrial> trials = new ArrayList<>();
//				trials.add(trial);
//				TrialRecorder recorder = new TrialRecorder();
//				recorder.export(trials, "aa", Integer.valueOf(1));
//
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
		
		if (!trial.getDeadEndRecordList().isEmpty()) {
			Repository.clearCache();
			DeadEndRecord record = trial.getDeadEndRecordList().get(0);
			DED datas = record.getTransformedData(result.buggyTrace); 
//				new TrainingDataTransfer().transfer(record, result.buggyTrace);
			record.setTransformedData(datas);
			try {
//				new DeadEndReporter().export(datas.getAllData(), Settings.projectName, 2);
				new DeadEndCSVWriter("_d4j", null).export(datas.getAllData(), projectName, bugId);
			} catch (NumberFormatException | IOException e) {
				e.printStackTrace();
			}
			System.currentTimeMillis();
		}
	}

	private Result parseResult(String projectName, String bugId) throws IOException {
		String buggyPath = PathConfiguration.getBuggyPath(projectName, bugId);
		String fixPath = PathConfiguration.getCorrectPath(projectName, bugId);

		ProjectConfig config = Defects4jProjectConfig.getConfig(projectName, bugId);

		DiffMatcher diffMatcher = new DiffMatcher(config.srcSourceFolder, config.srcTestFolder, buggyPath,
				fixPath);
		diffMatcher.matchCode();

		List<TestCase> list = config.retrieveFailingTestCase(buggyPath);
		TestCase tc = list.get(0);

		AppJavaClassPath buggyApp = AppClassPathInitializer.initialize(buggyPath, tc, config);
		AppJavaClassPath fixApp = AppClassPathInitializer.initialize(fixPath, tc, config);

		// Regression regression = new
		// RegressionRetriever().retriveRegression(projectName, id);
		Regression regression = retrieveRegression(config, buggyPath, fixPath);
		if (regression == null) {
			return null;
		}
		
		Trace buggyTrace = regression.getBuggyTrace();
		buggyTrace.setSourceVersion(true);
		buggyTrace.setAppJavaClassPath(buggyApp);
		buggyTrace.setSourceVersion(true);
		Trace correctTrace = regression.getCorrectTrace();
		correctTrace.setSourceVersion(false);
		correctTrace.setAppJavaClassPath(fixApp);
		correctTrace.setSourceVersion(false);

		// PairList pairList = regression.getPairList();
		fillingMissingInfo(buggyPath, fixPath, config, regression);

		/* PairList */
		System.out.println("start matching trace..., buggy trace length: " + buggyTrace.size()
				+ ", correct trace length: " + correctTrace.size());
		long time1 = System.currentTimeMillis();

		ControlPathBasedTraceMatcher traceMatcher = new ControlPathBasedTraceMatcher();
		PairList pairList = traceMatcher.matchTraceNodePair(buggyTrace, correctTrace, diffMatcher);
		long time2 = System.currentTimeMillis();
		int matchTime = (int) (time2 - time1);
		System.out.println("finish matching trace, taking " + matchTime + "ms");

		return new Result(buggyTrace, correctTrace, pairList, diffMatcher);
	}

	private void fillingMissingInfo(String buggyPath, String fixPath, ProjectConfig config,
			Regression regression) {
		for (TraceNode node : regression.getBuggyTrace().getExecutionList()) {
			BreakPoint point = node.getBreakPoint();
			if (point.getDeclaringCompilationUnitName() == null) {
				point.setDeclaringCompilationUnitName(point.getClassCanonicalName());
			}
		}
		for (TraceNode node : regression.getCorrectTrace().getExecutionList()) {
			BreakPoint point = node.getBreakPoint();
			if (point.getDeclaringCompilationUnitName() == null) {
				point.setDeclaringCompilationUnitName(point.getClassCanonicalName());
			}
		}
		regression.fillMissingInfo(config, buggyPath, fixPath);
	}

	private EmpiricalTrial simulate(Trace buggyTrace, Trace correctTrace, PairList pairList, 
			DiffMatcher diffMatcher, boolean useSliceBreaker, boolean enableRandom, int breakerLimit)
			throws SimulationFailException {
		long time1 = System.currentTimeMillis();
		System.out.println("start simulating debugging...");
		Simulator simulator = new Simulator(useSliceBreaker, enableRandom, breakerLimit);
		simulator.prepare(buggyTrace, correctTrace, pairList, diffMatcher);
		// TraceNode node = buggyTrace.getExecutionList().get(8667);
		// simulator.setObservedFault(node);

		RootCauseFinder rootcauseFinder = new RootCauseFinder();
		rootcauseFinder.setRootCauseBasedOnDefects4J(pairList, diffMatcher, buggyTrace, correctTrace);

		if (rootcauseFinder.getRealRootCaseList().isEmpty()) {
			EmpiricalTrial trial = EmpiricalTrial.createDumpTrial("cannot find real root cause");
			StepOperationTuple tuple = new StepOperationTuple(simulator.getObservedFault(),
					new UserFeedback(UserFeedback.UNCLEAR), simulator.getObservedFault(), DebugState.UNCLEAR);
			trial.getCheckList().add(tuple);

			return trial;
		}

		if (simulator.getObservedFault() == null) {
			EmpiricalTrial trial = EmpiricalTrial.createDumpTrial("cannot find observable fault");
			return trial;
		}

		List<EmpiricalTrial> trials0 = simulator.detectMutatedBug(buggyTrace, correctTrace, diffMatcher, 0);

		long time2 = System.currentTimeMillis();
		int simulationTime = (int) (time2 - time1);
		System.out.println("finish simulating debugging, taking " + simulationTime / 1000 + "s");

		for (EmpiricalTrial trial : trials0) {
			trial.setTraceCollectionTime(buggyTrace.getConstructTime() + correctTrace.getConstructTime());
			trial.setBuggyTrace(buggyTrace);
			trial.setFixedTrace(correctTrace);
			trial.setPairList(pairList);
			trial.setDiffMatcher(diffMatcher);

			PatternIdentifier identifier = new PatternIdentifier();
			identifier.identifyPattern(trial);
		}

		EmpiricalTrial trial = trials0.get(0);
		return trial;
	}

	private Regression retrieveRegression(ProjectConfig config, String buggyPath, String fixPath) {
		String projectName = config.projectName;
		String bugId = config.regressionID;
		ExecTraceFileReader execTraceReader = new ExecTraceFileReader();
		String buggyExec = InstrumentationExecutor
				.generateTraceFilePath(MicroBatUtil.generateTraceDir(projectName, bugId), "bug");
		String fixExec = InstrumentationExecutor
				.generateTraceFilePath(MicroBatUtil.generateTraceDir(projectName, bugId), "fix");
		if (!new File(buggyExec).exists() || !new File(fixExec).exists()) {
			System.out.println(String.format("[%s%s]Missing trace exec files!", projectName, bugId));
			return null;
		}
		Trace buggyTrace = execTraceReader.read(buggyExec);
		Trace fixTrace = execTraceReader.read(fixExec);

		Regression regression = new Regression(buggyTrace, fixTrace, null);
		return regression;
	}

}
