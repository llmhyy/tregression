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
import microbat.codeanalysis.runtime.StepLimitException;
import microbat.instrumentation.output.RunningInfo;
import microbat.preference.AnalysisScopePreference;
import microbat.util.MicroBatUtil;
import sav.common.core.utils.SingleTimer;
import sav.strategies.dto.AppJavaClassPath;
import traceagent.report.AgentDatasetReport;
import traceagent.report.BugCaseTrial;
import traceagent.report.BugCaseTrial.TraceTrial;
import tregression.constants.Dataset;
import tregression.empiricalstudy.TestCase;
import tregression.empiricalstudy.config.Defects4jProjectConfig;
import tregression.empiricalstudy.config.ProjectConfig;
import tregression.empiricalstudy.config.Regs4jProjectConfig;
import tregression.handler.paths.PathConfiguration;
import tregression.handler.paths.PathConfigurationFactory;
import tregression.separatesnapshots.AppClassPathInitializer;

/**
 * @author LLT
 *
 */
public class RunAllDatasetHandler  extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Job job = new Job("Run Trace Agent On Defects4j") {
			String datasetName = Dataset.getTypeFromPref().getName();
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				String reportFile = "Agent_" + datasetName + ".xlsx";
				try {
					runAll(reportFile, monitor);
					System.out.println("Complete!");
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					String oldFile = "Agent_" + datasetName + "_benchmark.xlsx";
					File newReport = new File(reportFile);
					if (newReport != null && newReport.exists() && oldFile != null
							&& new File(oldFile).exists()) {
						Map<String, List<String>> keys = new HashMap<String, List<String>>();
						keys.put("testcase", Arrays.asList("PROJECT_NAME", "BUG_ID", "TEST_CASE", "IS_BUG_TRACE"));
						ExperimentReportComparisonReporter.reportChange("Agent_" + datasetName + "_compare.xlsx", oldFile, newReport.getAbsolutePath(),
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
		String[] projects = Dataset.getProjectNames();
		int[] bugNum = Dataset.getBugNums();
		AgentDatasetReport report = new AgentDatasetReport(new File(reportFile));
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
				ProjectConfig projectConfig;
				if (Dataset.getTypeFromPref().equals(Dataset.DEFECTS4J)) {
					projectConfig = Defects4jProjectConfig.getConfig(project, String.valueOf(j));
				} else {
					projectConfig = Regs4jProjectConfig.getConfig(project, String.valueOf(j));
				}
				try {
					runSingleBug(projectConfig, report, null, filter, monitor);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	void runSingleBug(ProjectConfig config, AgentDatasetReport report, List<TestCase> tcs,
			TestcaseFilter filter, IProgressMonitor monitor)
			throws IOException {
		String projectName = config.projectName;
		String bugID = config.regressionID;
		PathConfiguration pathConfiguration = PathConfigurationFactory.createPathConfiguration(Dataset.DEFECTS4J);
		String buggyPath = pathConfiguration.getBuggyPath(projectName, bugID);
		String fixPath = pathConfiguration.getCorrectPath(projectName, bugID);
		if (tcs == null) {
			tcs = config.retrieveFailingTestCase(buggyPath);
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
	
	public TraceTrial run(String workingDir, TestCase tc, ProjectConfig config, List<String> includeLibs,
			List<String> excludeLibs, boolean isBuggy) {
		SingleTimer timer = SingleTimer.start(String.format("run %s test", isBuggy ? "buggy" : "correct"));
		AppJavaClassPath appClassPath = AppClassPathInitializer.initialize(workingDir, tc, config);
		
		String traceDir = MicroBatUtil.generateTraceDir(config.projectName, config.regressionID);
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
