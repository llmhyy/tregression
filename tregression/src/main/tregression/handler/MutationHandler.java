package tregression.handler;


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

import baseline.MutationAgent;
import microbat.Activator;
import microbat.model.trace.Trace;
import microbat.model.value.VarValue;
import microbat.util.JavaUtil;
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
				final String projectPath = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.REPO_PATH);
				final String dropInDir = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.DROP_IN_FOLDER);
				final String microbatConfigPath = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.CONFIG_PATH);

				String testCaseID_str = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.BUG_ID);
				final int testCaesID = Integer.parseInt(testCaseID_str);
				
				String seed_str = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.SEED);
				final int seed = Integer.parseInt(seed_str);
				
				// Perform mutation
				MutationAgent mutationAgent = new MutationAgent(projectPath, dropInDir, microbatConfigPath);
				mutationAgent.setTestCaseID(testCaesID);
				mutationAgent.setSeed(seed);
				mutationAgent.startMutation();
				
				// Visualize the mutation result
				updateView(mutationAgent.getBuggyTrace(), mutationAgent.getCorrectTrace(), mutationAgent.getPairList(), mutationAgent.getMatcher());
				
				// Pase mutation result to the BaselineHandler
				BaselineHandler.setInputs(mutationAgent.getInputs());
				BaselineHandler.setOutputs(mutationAgent.getOutputs());
				BaselineHandler.setRootCause(mutationAgent.getRootCause());
				BaselineHandler.setMutatedProPath(mutationAgent.getMutatedProjPath());
				BaselineHandler.setOriginalProPath(mutationAgent.getOriginalProjPath());
				BaselineHandler.setMutationCount(mutationAgent.getMutationCount());
				BaselineHandler.setTestCaseID(testCaesID);
				
				// Print out detected inputs/outputs
				for (VarValue input : mutationAgent.getInputs()) {
					System.out.println("Detected Inputs: " + input.getVarID());
				}
				for (VarValue output : mutationAgent.getOutputs()) {
					System.out.println("Detected Outputs: " + output.getVarID());
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
