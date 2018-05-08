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

import microbat.preference.AnalysisScopePreference;
import sav.common.core.utils.SingleTimer;
import traceagent.report.AgentDefects4jReport;
import traceagent.report.BugCaseTrial;
import traceagent.report.BugCaseTrial.TraceTrial;
import tregression.empiricalstudy.Defects4jProjectConfig;
import tregression.empiricalstudy.TestCase;
import tregression.handler.PathConfiguration;
import tregression.separatesnapshots.RunningResult;
import tregression.separatesnapshots.TraceCollector0;

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
		for (int i = 0; i < projects.length; i++) {
			String project = projects[i];
			for (int j = 0; j < bugNum[i]; j++) {
				System.out.println("working on the " + j + "th bug of " + project + " project.");
				Defects4jProjectConfig d4jConfig = Defects4jProjectConfig.getD4JConfig(project, j);
				try {
					runSingleBug(d4jConfig, report, null);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	void runSingleBug(Defects4jProjectConfig config, AgentDefects4jReport report, List<TestCase> tcs)
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
			BugCaseTrial trial = new BugCaseTrial(projectName, bugID, tc);
			TraceCollector0 buggyCollector = new TraceCollector0(true);
			TraceCollector0 correctCollector = new TraceCollector0(false);
			SingleTimer timer = SingleTimer.start("run buggy test");
			RunningResult buggyRS = buggyCollector.run(buggyPath, tc, config, true, true, includeLibs, excludeLibs);
			TraceTrial bugTrace = new TraceTrial(buggyPath, buggyRS, timer.getExecutionTime());
			
			timer.startNewTask("run correct test");
			RunningResult correctRs = correctCollector.run(fixPath, tc, config, true, true, includeLibs, excludeLibs);
			TraceTrial correctTrace = new TraceTrial(fixPath, correctRs, timer.getExecutionTime());
			
			trial.setBugTrace(bugTrace);
			trial.setFixedTrace(correctTrace);
			report.record(trial);
		}
	}
	
}
