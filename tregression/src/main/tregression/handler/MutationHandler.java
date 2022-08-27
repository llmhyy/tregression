package tregression.handler;

import java.util.ArrayList;
import java.util.List;

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
import jmutation.MutationFramework;
import jmutation.model.MutationResult;
import jmutation.model.Project;
import jmutation.model.TestCase;
import microbat.Activator;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.util.JavaUtil;
import testio.TestIOFramework;
import testio.model.IOModel;
import testio.model.TestIO;
import tracediff.TraceDiff;
import tracediff.model.PairList;
import tracediff.model.TraceNodePair;
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
//				
//				// Setup parameter
//				final String srcFolderPath = "src\\main\\java";
//				final String testFolderPath = "src\\test\\java";
				final String projectPath = "C:/Users/arkwa/git/java-mutation-framework/sample/math_70";
				final String dropInDir = "C:/Users/arkwa/git/java-mutation-framework/lib";
				final String microbatConfigPath = "C:\\Users\\arkwa\\git\\java-mutation-framework\\sampleMicrobatConfig.json";
////				
//				final int maxMutation = 1;
//				final int maxMutationLimit = 5;
//				// Get the test case id from preference
				String testCaseID_str = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.BUG_ID);
				final int testCaesID = Integer.parseInt(testCaseID_str);
//				System.out.println("testing on test case id: " + testCaesID);
//				
//				// Mutation framework will mutate the target project
//				MutationFramework mutationFramework = new MutationFramework();
//				mutationFramework.setProjectPath(projectPath);
//				mutationFramework.setDropInsDir(dropInDir);
//				mutationFramework.setMicrobatConfigPath(microbatConfigPath);
//				mutationFramework.setMaxNumberOfMutations(maxMutation);
//				
//				TestCase testCase = mutationFramework.getTestCases().get(testCaesID);
//				mutationFramework.setTestCase(testCase);
//				
//				// Mutate project until it fail the test case
//				MutationResult result = null;
//				boolean testCaseFailed = false;
//				for (int count=0; count<maxMutationLimit; count++) {
//					mutationFramework.setSeed(1);
//					result = mutationFramework.startMutationFramework();
//					if (!result.mutatedTestCasePassed()) {
//						testCaseFailed = true;
//						break;
//					}
//				}
//				
//				if (!testCaseFailed) {
//					System.out.println("Cannot fail the test case after mutation");
//					return null;
//				}
//				
//				Project mutatedProject = result.getMutatedProject();
//				Project originalProject = result.getOriginalProject();
//				
//				final Trace buggyTrace = result.getMutatedTrace();
//				buggyTrace.setSourceVersion(true);
//				
//				final Trace correctTrace = result.getOriginalTrace();
//				
//				// Convert tracediff.PairList to tregression.PairList
//				PairList pairList = TraceDiff.getTraceAlignment(srcFolderPath, testFolderPath,
//	                    mutatedProject.getRoot().getAbsolutePath(), originalProject.getRoot().getAbsolutePath(),
//	                    result.getMutatedTrace(), result.getOriginalTrace());
//				List<tregression.model.TraceNodePair> pairLTregression = new ArrayList<>();
//				for (TraceNodePair pair : pairList.getPairList()) {
//					pairLTregression.add(new tregression.model.TraceNodePair(pair.getBeforeNode(), pair.getAfterNode()));
//				}
//				final tregression.model.PairList pairListTregression = new tregression.model.PairList(pairLTregression);
//				
//				// Set up the diffMatcher
//				final DiffMatcher matcher = new DiffMatcher(srcFolderPath, testFolderPath, mutatedProject.getRoot().getAbsolutePath(), originalProject.getRoot().getAbsolutePath());
//				matcher.matchCode();
//				
//				// Update view
//				updateView(buggyTrace, correctTrace, pairListTregression, matcher);
//				
//				// Send paramters to BaselineHandler
//				List<TraceNode> rootCauses = result.getRootCauses();
//				if (rootCauses.isEmpty()) {
//					throw new RuntimeException("Root cause is not found");
//				}
//				BaselineHandler.setRootCause(rootCauses.get(rootCauses.size()-1));
//				
//				TestIOFramework testIOFramework = new TestIOFramework();
//				TestIO testIO = testIOFramework.getBuggyTestIOs(result.getOriginalResult(),
//						result.getOriginalResultWithAssertions(),
//						result.getMutatedResult(),
//						result.getMutatedResultWithAssertions(), originalProject.getRoot(),
//	                    mutatedProject.getRoot(), pairList, result.getTestClass(),
//	                    result.getTestSimpleName());
//				
//				if (testIO.getInputs().isEmpty() || testIO.getOutput() == null) {
//					throw new RuntimeException("No IO");
//				}
//				
//				List<VarValue> inputs = new ArrayList<>();
//				for (IOModel model : testIO.getInputs()) {
//					inputs.add(model.getValue());
//				}
				
//				List<VarValue> inputs = testIOs.get(testIOs.size()-1).getInputs();
//				List<VarValue> outputs = new ArrayList<>();
//				outputs.add(testIO.getOutput());
				
				MutationAgent mutationAgent = new MutationAgent(projectPath, dropInDir, microbatConfigPath);
				mutationAgent.setTestCaseID(testCaesID);
				mutationAgent.startMutation();
				
				updateView(mutationAgent.getBuggyTrace(), mutationAgent.getCorrectTrace(), mutationAgent.getPairList(), mutationAgent.getMatcher());
				
				BaselineHandler.setInputs(mutationAgent.getInputs());
				BaselineHandler.setOutputs(mutationAgent.getOutputs());
				BaselineHandler.setRootCause(mutationAgent.getRootCause().get(0));
				BaselineHandler.setMutatedProPath(mutationAgent.getMutatedProjPath());
				BaselineHandler.setOriginalProPath(mutationAgent.getOriginalProjPath());
				
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
