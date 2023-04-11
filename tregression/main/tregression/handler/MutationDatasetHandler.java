package tregression.handler;

import java.io.IOException;
import java.nio.file.Paths;

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
import microbat.Activator;
import microbat.util.JavaUtil;
import sav.strategies.dto.AppJavaClassPath;
import tregression.empiricalstudy.config.ConfigFactory;
import tregression.empiricalstudy.config.ProjectConfig;
import tregression.model.PairList;
import tregression.preference.TregressionPreference;
import tregression.separatesnapshots.AppClassPathInitializer;
import tregression.views.BuggyTraceView;
import tregression.views.CorrectTraceView;
import tregression.separatesnapshots.DiffMatcher;
import tregression.tracematch.ControlPathBasedTraceMatcher;
import dataset.BugDataset;
import dataset.BugDataset.BugData;
import dataset.TestCase;
import dataset.bug.minimize.ProjectMinimizer;
import dataset.execution.Request;
import dataset.execution.handler.TraceCollectionHandler;



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
				
//				final int largestBugId = 17426;
				int traceCollectionTimeoutSeconds = 60;
				
				boolean useTestCaseID = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.USE_TEST_CASE_ID).equals("true");
				if (!useTestCaseID) {
					throw new RuntimeException("Mutation dataset must locate the bug proejction by bug id. Please check the use test case id box.");
				}
				String testCaseID_str = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.TEST_CASE_ID);
				int testCaseID = Integer.parseInt(testCaseID_str);
				
				System.out.println("Loading mutated projection from " + projectPath + " with bug id " + testCaseID);
				BugDataset bugDataset = new BugDataset(projectPath);
				
				TraceCollectionHandler hanlder = new TraceCollectionHandler(projectRepo, projectName, testCaseID, traceCollectionTimeoutSeconds,0, 0);
				hanlder.handle(new Request(true));
				BugData data = null;
				try {
					data = bugDataset.getData(testCaseID);
				} catch (Exception e){
					e.printStackTrace();
					return Status.OK_STATUS;
				}


				final String srcFolderPath = "src\\main\\java";
				final String testFolderPath = "src\\test\\java";
				
				final String projName = data.getProjectName();
				final String mutatedProjPath = data.getBuggyProjectPath();
				final String originalProjPath = data.getWorkingProjectPath();
				
				Trace buggyTrace = data.getBuggyTrace();
				Trace correctTrace = data.getWorkingTrace();
				TestCase testCase = data.getTestCase();
				final String regressionID = testCase.toString();
				
				ProjectConfig config = ConfigFactory.createConfig(projName, regressionID, mutatedProjPath, originalProjPath);
				tregression.empiricalstudy.TestCase tc = new tregression.empiricalstudy.TestCase(testCase.testClassName(), testCase.testMethodName());
				AppJavaClassPath buggyApp = AppClassPathInitializer.initialize(mutatedProjPath, tc, config);
				AppJavaClassPath correctApp = AppClassPathInitializer.initialize(originalProjPath, tc, config);
				
				buggyTrace.setAppJavaClassPath(buggyApp);
				correctTrace.setAppJavaClassPath(correctApp);
				
				DiffMatcher matcher = new DiffMatcher(srcFolderPath, testFolderPath, mutatedProjPath, originalProjPath);
				matcher.matchCode();
				
				ControlPathBasedTraceMatcher traceMatcher = new ControlPathBasedTraceMatcher();
				PairList pairList = traceMatcher.matchTraceNodePair(buggyTrace, correctTrace, matcher);
				
				updateView(buggyTrace, correctTrace, pairList, matcher);
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
