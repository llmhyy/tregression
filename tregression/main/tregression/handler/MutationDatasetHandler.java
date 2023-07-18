package tregression.handler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.Activator;
import microbat.util.JavaUtil;
import sav.strategies.dto.AppJavaClassPath;
import tregression.empiricalstudy.TrialGenerator0;
import tregression.empiricalstudy.config.ConfigFactory;
import tregression.empiricalstudy.config.MutationDatasetProjectConfig;
import tregression.empiricalstudy.config.ProjectConfig;
import tregression.model.PairList;
import tregression.preference.TregressionPreference;
import tregression.separatesnapshots.AppClassPathInitializer;
import tregression.views.BuggyTraceView;
import tregression.views.CorrectTraceView;
import tregression.separatesnapshots.DiffMatcher;
import tregression.tracematch.ControlPathBasedTraceMatcher;
import jmutation.dataset.BugDataset;
import jmutation.dataset.BugDataset.BugData;
import jmutation.dataset.TestCase;
import jmutation.dataset.bug.minimize.ProjectMinimizer;
import jmutation.dataset.bug.model.path.MutationFrameworkPathConfiguration;
import jmutation.dataset.bug.model.path.PathConfiguration;
import jmutation.dataset.execution.Request;
import jmutation.dataset.execution.handler.TraceCollectionHandler;
import tregression.empiricalstudy.DeadEndRecord;
import tregression.empiricalstudy.EmpiricalTrial;


public class MutationDatasetHandler extends AbstractHandler {
	
	private BuggyTraceView buggyView;
	private CorrectTraceView correctView;
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		JavaUtil.sourceFile2CUMap.clear();
		Job job = new Job("Testing Tregression") {
			

			@Override
			protected IStatus run(IProgressMonitor monitor) {
		        
				setup();
		        
				String projectRepo = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.REPO_PATH);
				String projectName = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.PROJECT_NAME);
				final String projectPath = Paths.get(projectRepo, projectName).toString();

				int traceCollectionTimeoutSeconds = 60;
				
//				boolean useTestCaseID = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.BUG_ID).equals("true");
//				if (!useTestCaseID) {
//					throw new RuntimeException("Mutation dataset must locate the bug proejction by bug id. Please check the use test case id box.");
//				}
				String testCaseID_str = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.BUG_ID);
				int testCaseID = Integer.parseInt(testCaseID_str);
				
				System.out.println("Loading mutated projection from " + projectPath + " with bug id " + testCaseID);
				BugDataset dataset = new BugDataset(projectPath);
				
				try {
					PathConfiguration pathConfig = new MutationFrameworkPathConfiguration(projectRepo);
					dataset.unzip(testCaseID);
					ProjectMinimizer minimizer = dataset.createMinimizer(testCaseID);
					minimizer.maximise();
					
					final String bugFolder = pathConfig.getBuggyPath(projectName, testCaseID_str);
					final String fixFolder = pathConfig.getFixPath(projectName, testCaseID_str);
					final ProjectConfig config = ConfigFactory.createConfig(projectName, testCaseID_str, bugFolder, fixFolder);
					MutationDatasetProjectConfig.executeMavenCmd(Paths.get(bugFolder), "test-compile");
					final TrialGenerator0 generator0 = new TrialGenerator0();
					List<EmpiricalTrial> trails = generator0.generateTrials(bugFolder, fixFolder, false, false, false, 3, true, true, config, "");
					if(trails.size() != 0) {
						PlayRegressionLocalizationHandler.finder = trails.get(0).getRootCauseFinder();					
					}
//					String pathToBug = pathConfig.getBugPath(projectName, testCaseID_str);
//					FileUtils.deleteDirectory(new File(pathToBug));
				} catch (IOException e) {
					e.printStackTrace();
				}
				return Status.OK_STATUS;
			}
			
		};
		job.schedule();
		return null;
	}
	
	private void setup() {
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				try {
					buggyView = (BuggyTraceView)PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(BuggyTraceView.ID);
					correctView = (CorrectTraceView)PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(CorrectTraceView.ID);
				} catch (PartInitException e) {
					buggyView = null;
					correctView = null;
					System.out.println("Fail to get the view");
				}
			}
		});
	}
	
	private void updateView(final Trace buggyTrace, final Trace correctTrace, final tregression.model.PairList pairListTregression, final DiffMatcher matcher) {
		if (this.buggyView != null && this.correctView != null) {
			Display.getDefault().syncExec(new Runnable() {
				@Override
				public void run() {

					buggyView.setMainTrace(buggyTrace);
					buggyView.updateData();
					buggyView.setPairList(pairListTregression);
					buggyView.setDiffMatcher(matcher);
					
					correctView.setMainTrace(correctTrace);
					correctView.updateData();
					correctView.setPairList(pairListTregression);
					correctView.setDiffMatcher(matcher);
				}
			});
		} else {
			System.out.println("buggyView or correctView is null");
		}
	}

}
