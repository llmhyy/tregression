/**
 * 
 */
package traceagent.handler;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import microbat.Activator;
import sav.common.core.utils.StringUtils;
import traceagent.report.AgentDatasetReport;
import tregression.constants.Dataset;
import tregression.empiricalstudy.TestCase;
import tregression.empiricalstudy.config.Defects4jProjectConfig;
import tregression.empiricalstudy.config.ProjectConfig;
import tregression.empiricalstudy.config.Regs4jProjectConfig;
import tregression.preference.TregressionPreference;

/**
 * @author LLT
 *
 */
public class RunSingleTestcaseHandler extends RunAllDatasetHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Job job = new Job("Run Trace Agent On Defects4j bug") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					String projectName = Activator.getDefault().getPreferenceStore()
							.getString(TregressionPreference.PROJECT_NAME);
					String id = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.BUG_ID);
					String testcase = Activator.getDefault().getPreferenceStore()
							.getString(TregressionPreference.TEST_CASE);
					List<TestCase> tcs = null;
					if (!StringUtils.isEmpty(testcase)) {
						tcs = Arrays.asList(new TestCase(testcase));
					}
					System.out.println("working on the " + id + "th bug of " + projectName + " project.");
					ProjectConfig projectConfig;
					if (Dataset.getTypeFromPref().equals(Dataset.DEFECTS4J)) {
						projectConfig = Defects4jProjectConfig.getConfig(projectName, String.valueOf(id));
					} else {
						projectConfig = Regs4jProjectConfig.getConfig(projectName, String.valueOf(id));
					}
					AgentDatasetReport report = new AgentDatasetReport(new File("Agent_Defect4j_tc.xlsx"));
					runSingleBug(projectConfig, report, tcs, new TestcaseFilter(false), monitor);
				} catch (Exception e) {
					e.printStackTrace();
				}
				return Status.OK_STATUS;
			}
		};
		job.schedule();
		return null;
	}

}
