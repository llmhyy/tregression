/**
 * 
 */
package traceagent.handler;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import microbat.codeanalysis.runtime.InstrumentationExecutor;
import microbat.codeanalysis.runtime.PreCheckInformation;
import microbat.codeanalysis.runtime.RunningInformation;
import microbat.preference.AnalysisScopePreference;
import microbat.util.MicroBatUtil;
import sav.common.core.utils.SingleTimer;
import sav.strategies.dto.AppJavaClassPath;
import traceagent.report.AgentDefects4jReport;
import traceagent.report.BugCaseTrial;
import traceagent.report.BugCaseTrial.TraceTrial;
import tregression.empiricalstudy.Defects4jProjectConfig;
import tregression.empiricalstudy.TestCase;
import tregression.handler.PathConfiguration;
import tregression.separatesnapshots.AppClassPathInitializer;

/**
 * @author LLT
 *
 */
public class RunAllDefects4jHandler  extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Job job = new Job("Run Trace Agent On Defects4j") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					runAll(monitor);
					System.out.println("Complete!");
				} catch (Exception e) {
					e.printStackTrace();
				}
				return Status.OK_STATUS;
			}
		};
		job.schedule();
		return null;
	}

	protected void runAll(IProgressMonitor monitor) throws Exception {
		String[] projects = {"Chart", "Closure", "Lang", "Math", "Mockito", "Time"};
		int[] bugNum = {26, 133, 65, 106, 38, 27};
		AgentDefects4jReport report = new AgentDefects4jReport(new File("Agent_Defect4j.xlsx"));
		TestcaseFilter filter = new TestcaseFilter("Agent_Defect4j.xlsx");
		for (int i = 0; i < projects.length; i++) {
			String project = projects[i];
			for (int j = 0; j < bugNum[i]; j++) {
				if (monitor.isCanceled()) {
					return;
				}
				System.out.println("working on the " + j + "th bug of " + project + " project.");
				Defects4jProjectConfig d4jConfig = Defects4jProjectConfig.getD4JConfig(project, j);
				try {
					runSingleBug(d4jConfig, report, null, filter, monitor);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	void runSingleBug(Defects4jProjectConfig config, AgentDefects4jReport report, List<TestCase> tcs,
			TestcaseFilter filter, IProgressMonitor monitor)
			throws IOException {
		String projectName = config.projectName;
		String bugID = String.valueOf(config.bugID);
		String buggyPath = PathConfiguration.getBuggyPath(projectName, bugID);
		String fixPath = PathConfiguration.getCorrectPath(projectName, bugID);
		if (tcs == null) {
			tcs = Defects4jProjectConfig.retrieveD4jFailingTestCase(buggyPath);
		}
		List<String> includeLibs = AnalysisScopePreference.getIncludedLibList();
		List<String> excludeLibs = AnalysisScopePreference.getExcludedLibList();
		for (TestCase tc : tcs) {
			if (monitor.isCanceled()) {
				return;
			}
			BugCaseTrial trial = new BugCaseTrial(projectName, bugID, tc);
			SingleTimer timer = SingleTimer.start("run buggy test");
			if (!filter.filter(projectName, bugID, tc.getName(), true)) {
				TraceTrial bugTrace = run(buggyPath, tc, config, includeLibs, excludeLibs, true);
				trial.setBugTrace(bugTrace);
			}
			timer.startNewTask("run correct test");
			if (!filter.filter(projectName, bugID, tc.getName(), false)) {
				TraceTrial correctTrace = run(fixPath, tc, config, includeLibs, excludeLibs, false);
				trial.setFixedTrace(correctTrace);
			}
			
			report.record(trial);
		}
	}
	
	public TraceTrial run(String workingDir, TestCase tc, Defects4jProjectConfig config, List<String> includeLibs,
			List<String> excludeLibs, boolean isBuggy) {
		SingleTimer timer = SingleTimer.start(String.format("run %s test", isBuggy ? "buggy" : "correct"));
		AppJavaClassPath appClassPath = AppClassPathInitializer.initialize(workingDir, tc, config);
		
		String traceDir = MicroBatUtil.generateTraceDir(config.projectName, String.valueOf(config.bugID));
		String traceName = isBuggy ? "bug" : "fix";
		InstrumentationExecutor executor = new InstrumentationExecutor(appClassPath, traceDir, traceName, includeLibs,
				excludeLibs);
		
		RunningInformation info = executor.run();
		PreCheckInformation precheckInfo = executor.getPrecheckInfo();
		
		return new TraceTrial(workingDir, precheckInfo, info, timer.getExecutionTime(), isBuggy);
	}
}
