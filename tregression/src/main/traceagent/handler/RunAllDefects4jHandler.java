/**
 * 
 */
package traceagent.handler;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import experiment.utils.report.ExperimentReportComparisonReporter;
import experiment.utils.report.rules.TextComparisonRule;
import microbat.codeanalysis.runtime.InstrumentationExecutor;
import microbat.codeanalysis.runtime.PreCheckInformation;
import microbat.codeanalysis.runtime.RunningInformation;
import microbat.codeanalysis.runtime.StepLimitException;
import microbat.instrumentation.output.RunningInfo;
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
				String reportFile = "Agent_Defect4j.xlsx";
				try {
					runAll(reportFile, monitor);
					System.out.println("Complete!");
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					String oldDefects4jFile = "Agent_Defect4j_benchmark.xlsx";
					File newReport = new File(reportFile);
					if (newReport != null && newReport.exists() && oldDefects4jFile != null
							&& new File(oldDefects4jFile).exists()) {
						Map<String, List<String>> keys = new HashMap<String, List<String>>();
						keys.put("testcase", Arrays.asList("PROJECT_NAME", "BUG_ID", "TEST_CASE", "IS_BUG_TRACE"));
						ExperimentReportComparisonReporter.reportChange("Agent_Defect4j_compare.xlsx", oldDefects4jFile, newReport.getAbsolutePath(),
								Arrays.asList(new TextComparisonRule(null)), keys);
					}
				}
				return Status.OK_STATUS;
			}
		};
		job.schedule();
		return null;
	}

	protected void runAll(String reportFile, IProgressMonitor monitor) throws Exception {
		String[] projects = {"Chart", "Closure", "Lang", "Math", "Mockito", "Time"};
		int[] bugNum = {26, 133, 65, 106, 38, 27};
		AgentDefects4jReport report = new AgentDefects4jReport(new File(reportFile));
		TestcaseFilter filter = new TestcaseFilter(false); 
		for (int i = 0; i < projects.length; i++) {
			String project = projects[i];
			if (monitor.isCanceled()) {
				return;
			}
			for (int j = 0; j <= bugNum[i]; j++) {
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
		
		RunningInfo info = null;
		try {
			info = executor.run();
		} catch (StepLimitException e) {
			e.printStackTrace();
		}
		PreCheckInformation precheckInfo = executor.getPrecheckInfo();
		return new TraceTrial(workingDir, precheckInfo, info, timer.getExecutionTime(), isBuggy);
	}
}
