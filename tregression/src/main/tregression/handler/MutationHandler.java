package tregression.handler;


import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;

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

import microbat.Activator;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.preference.MicrobatPreference;
import microbat.util.JavaUtil;
import microbat.util.Settings;
import mutation.MutationAgent;
import tregression.preference.TregressionPreference;
import tregression.separatesnapshots.DiffMatcher;
import tregression.views.BuggyTraceView;
import tregression.views.CorrectTraceView;

public class MutationHandler extends AbstractHandler {

	private BuggyTraceView buggyView;
	private CorrectTraceView correctView;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		JavaUtil.sourceFile2CUMap.clear();
		Job job = new Job("Testing Tregression") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				
				// Access the buggy view and correct view
				setup();
		
				// Setup parameter
				final String projectRepo = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.REPO_PATH);
				final String projectName = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.PROJECT_NAME);
				final String projectPath = Paths.get(projectRepo, projectName).toString();
				
				final String java_path = Activator.getDefault().getPreferenceStore().getString(MicrobatPreference.JAVA7HOME_PATH);
				final int stepLimit = Settings.stepLimit;
				
				// Perform mutation
				MutationAgent mutationAgent = new MutationAgent(projectPath, java_path, stepLimit);
				
				// Set the target test case
				boolean useTestCaseID = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.USE_TEST_CASE_ID).equals("true");
				if (useTestCaseID) {
					String testCaseID_str = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.TEST_CASE_ID);
					final int testCaesID = Integer.parseInt(testCaseID_str);
					mutationAgent.setTestCaseID(testCaesID);
				} else {
					String testCaseName = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.TEST_CASE);
					String[] tokens = testCaseName.split("#");
					mutationAgent.setTestCaseInfo(tokens[0], tokens[1]);
				}
				
				try {
					mutationAgent.setup();
				} catch (IOException e) {
					System.out.println("Fail to setup the mutation agent");
					return Status.OK_STATUS;
				}
				
				mutationAgent.startMutation();
				
				// Visualize the mutation result
				updateView(mutationAgent.getBuggyTrace(), mutationAgent.getCorrectTrace(), mutationAgent.getPairList(), mutationAgent.getMatcher());
				
				// Pass mutation result to the BaselineHandler
				BaselineHandler.setInputs(mutationAgent.getInputs());
				BaselineHandler.setOutputs(mutationAgent.getOutputs());
				BaselineHandler.setRootCause(mutationAgent.getRootCause());
				BaselineHandler.setMutatedProPath(mutationAgent.getMutatedProjPath());
				BaselineHandler.setOriginalProPath(mutationAgent.getOriginalProjPath());
				BaselineHandler.setMutationCount(mutationAgent.getMutationCount());
//				BaselineHandler.setTestCaseID(testCaesID);
				BaselineHandler.setTestCaseMethod(mutationAgent.getTestCase().simpleName);
				
				// Print out detected inputs/outputs
				for (VarValue input : mutationAgent.getInputs()) {
					System.out.println("Detected Inputs: " + input.getVarID());
				}
				for (VarValue output : mutationAgent.getOutputs()) {
					System.out.println("Detected Outputs: " + output.getVarID());
				}
				
				// Print root cause order
				String rootCauseIDStr = "";
				Collections.sort(BaselineHandler.rootCause, new Comparator<TraceNode>() {
					@Override
					public int compare(TraceNode node1, TraceNode node2) {
						return node1.getOrder() - node2.getOrder();
					}
				});
				for (TraceNode rootCause : BaselineHandler.rootCause) {
					rootCauseIDStr += rootCause.getOrder() + ",";
				}
				System.out.println("Ground Truth: " + rootCauseIDStr);
				
				System.out.println("Mutation Count: " + mutationAgent.getMutationCount());
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
					// TODO Auto-generated method stub
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
