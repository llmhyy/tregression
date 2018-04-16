package tregression.handler;

import java.io.IOException;
import java.util.ArrayList;
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
import microbat.model.trace.Trace;
import microbat.recommendation.DebugState;
import microbat.recommendation.UserFeedback;
import microbat.util.MicroBatUtil;
import microbat.util.Settings;
import sav.strategies.dto.AppJavaClassPath;
import tregression.SimulationFailException;
import tregression.empiricalstudy.DeadEndCSVWriter;
import tregression.empiricalstudy.DeadEndRecord;
import tregression.empiricalstudy.DeadEndReporter;
import tregression.empiricalstudy.Defects4jProjectConfig;
import tregression.empiricalstudy.EmpiricalTrial;
import tregression.empiricalstudy.Regression;
import tregression.empiricalstudy.RootCauseFinder;
import tregression.empiricalstudy.Simulator;
import tregression.empiricalstudy.TestCase;
import tregression.empiricalstudy.TrialGenerator;
import tregression.empiricalstudy.TrialRecorder;
import tregression.empiricalstudy.solutionpattern.PatternIdentifier;
import tregression.empiricalstudy.training.DED;
import tregression.empiricalstudy.training.TrainingDataTransfer;
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
					boolean isReuse = false;

					Settings.compilationUnitMap.clear();
					Settings.iCompilationUnitMap.clear();

					if (!isReuse || result == null) {
						result = parseResult();
					}

					Visualizer visualizer = new Visualizer();
					visualizer.visualize(result.buggyTrace, result.correctTrace, result.pairList, result.diffMatcher);

					EmpiricalTrial trial = simulate(result.buggyTrace, result.correctTrace, result.pairList,
							result.diffMatcher, true, 3);
					System.out.println(trial);
					
//					try {
//						List<EmpiricalTrial> trials = new ArrayList<>();
//						trials.add(trial);
//						TrialRecorder recorder = new TrialRecorder();
//						recorder.export(trials, "aa", Integer.valueOf(1));
//
//					} catch (IOException e) {
//						e.printStackTrace();
//					}
					
					if (!trial.getDeadEndRecordList().isEmpty()) {
						Repository.clearCache();
						DeadEndRecord record = trial.getDeadEndRecordList().get(0);
						DED datas = new TrainingDataTransfer().transfer(record, result.buggyTrace);
						try {
							new DeadEndReporter().export(datas.getAllData(), Settings.projectName, 2);
							
							String projectName = Activator.getDefault().getPreferenceStore()
									.getString(TregressionPreference.PROJECT_NAME);
							String id = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.BUG_ID);
							new DeadEndCSVWriter().export(datas.getAllData(), projectName, id);
						} catch (NumberFormatException | IOException e) {
							e.printStackTrace();
						}
						System.currentTimeMillis();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

				return Status.OK_STATUS;
			}

			private Result parseResult() throws IOException {
				String buggyPath = PathConfiguration.getBuggyPath();
				String fixPath = PathConfiguration.getCorrectPath();

				String projectName = Activator.getDefault().getPreferenceStore()
						.getString(TregressionPreference.PROJECT_NAME);
				String id = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.BUG_ID);

				Defects4jProjectConfig config = Defects4jProjectConfig.getD4JConfig(projectName, Integer.valueOf(id));

				DiffMatcher diffMatcher = new DiffMatcher(config.srcSourceFolder, config.srcTestFolder, buggyPath,
						fixPath);
				diffMatcher.matchCode();

				List<TestCase> list = new TrialGenerator().retrieveD4jFailingTestCase(buggyPath);
				TestCase tc = list.get(0);

				AppJavaClassPath buggyApp = AppClassPathInitializer.initialize(buggyPath, tc, config);
				AppJavaClassPath fixApp = AppClassPathInitializer.initialize(fixPath, tc, config);

				// Regression regression = new
				// RegressionRetriever().retriveRegression(projectName, id);
				Regression regression = retrieveRegression(config, buggyPath, fixPath);

				Trace buggyTrace = regression.getBuggyTrace();
				buggyTrace.setSourceVersion(true);
				buggyTrace.setAppJavaClassPath(buggyApp);
				buggyTrace.setSourceVersion(true);
				Trace correctTrace = regression.getCorrectTrace();
				correctTrace.setSourceVersion(false);
				correctTrace.setAppJavaClassPath(fixApp);
				correctTrace.setSourceVersion(false);

				// PairList pairList = regression.getPairList();
				regression.fillMissingInfo(config, buggyPath, fixPath);

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
		};
		job.schedule();

		return null;
	}

	private EmpiricalTrial simulate(Trace buggyTrace, Trace correctTrace, PairList pairList, 
			DiffMatcher diffMatcher, boolean useSliceBreaker, int breakerLimit)
			throws SimulationFailException {
		long time1 = System.currentTimeMillis();
		System.out.println("start simulating debugging...");
		Simulator simulator = new Simulator(useSliceBreaker, breakerLimit);
		simulator.prepare(buggyTrace, correctTrace, pairList, diffMatcher);
		// TraceNode node = buggyTrace.getExecutionList().get(8667);
		// simulator.setObservedFault(node);

		RootCauseFinder rootcauseFinder = new RootCauseFinder();
		rootcauseFinder.setRootCauseBasedOnDefects4J(pairList, diffMatcher, buggyTrace, correctTrace);

		if (rootcauseFinder.getRealRootCaseList().isEmpty()) {
			EmpiricalTrial trial = EmpiricalTrial.createDumpTrial("cannot find real root cause");
			if (buggyTrace.isMultiThread() || correctTrace.isMultiThread()) {
				trial.setMultiThread(true);
				StepOperationTuple tuple = new StepOperationTuple(simulator.getObservedFault(),
						new UserFeedback(UserFeedback.UNCLEAR), simulator.getObservedFault(), DebugState.UNCLEAR);
				trial.getCheckList().add(tuple);
			}

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

	private Regression retrieveRegression(Defects4jProjectConfig config, String buggyPath, String fixPath) {
		String projectName = config.projectName;
		String bugId = String.valueOf(config.bugID);
		ExecTraceFileReader execTraceReader = new ExecTraceFileReader();
		String buggyExec = InstrumentationExecutor
				.generateTraceFilePath(MicroBatUtil.generateTraceDir(projectName, bugId), "bug");
		Trace buggyTrace = execTraceReader.read(buggyExec);
		String fixExec = InstrumentationExecutor
				.generateTraceFilePath(MicroBatUtil.generateTraceDir(projectName, bugId), "fix");
		Trace fixTrace = execTraceReader.read(fixExec);

		Regression regression = new Regression(buggyTrace, fixTrace, null);
		return regression;
	}

}
