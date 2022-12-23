package tregression.handler;

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

import debuginfo.DebugInfo;
import debuginfo.NodeFeedbackPair;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.probability.SPP.StepwisePropagator;
import microbat.recommendation.UserFeedback;
import microbat.util.JavaUtil;
import tregression.views.BuggyTraceView;
import tregression.views.CorrectTraceView;

public class StepwisePropagationHandler extends AbstractHandler {

	private BuggyTraceView buggyView;
	private CorrectTraceView correctView;
	
	private TraceNode currentNode = null;
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		JavaUtil.sourceFile2CUMap.clear();
		Job job = new Job("Testing Tregression") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				
				setup();
				
				System.out.println();
				System.out.println("---------------------------------------------");
				System.out.println("\t Stepwise Probability Propagation");
				System.out.println();
				
				// Check is the trace ready
				if (buggyView.getTrace() == null) {
					System.out.println("Please setup the trace before propagation");
					return Status.OK_STATUS;
				}
				
				// Check is the IO ready
				if (!isIOReady()) {
					System.out.println("Please provide the inputs and the outputs");
					return Status.OK_STATUS;
				}
				
				// Obtain the inputs and outputs from users
				List<VarValue> inputs = DebugInfo.getInputs();
				List<VarValue> outputs = DebugInfo.getOutputs();
				
				currentNode = getStartingNode(buggyView.getTrace(), outputs.get(0));
				
				// Set up the propagator that perform propagation
				StepwisePropagator propagator = new StepwisePropagator(buggyView.getTrace(), inputs, outputs);
				
				int feedbackCounts = 0;
				
				while(!DebugInfo.isRootCauseFound()) {
					System.out.println("---------------------------------- " + feedbackCounts + " iteration");
					System.out.println("Propagation Start");
					
					// Start back propagation
					propagator.backPropagate();
					
					System.out.println("Propagation End");
					
					UserFeedback feedback = propagator.giveFeedback(currentNode);
					
					jumpToNode(currentNode);
					
					System.out.println();
					System.out.println("Prediction for node: " + currentNode.getOrder());
					System.out.println(feedback);
			
//					// Get the predicted root cause
//					TraceNode rootCause = propagator.proposeRootCause();
//					System.out.println("Proposed Root Cause: " + rootCause.getOrder());
//					
//					// Visualization
//					jumpToNode(rootCause);
//					
					// Obtain feedback from user
					System.out.println("Please give a feedback");
					DebugInfo.waitForFeedbackOrRootCause();
					
					if (DebugInfo.isRootCauseFound()) {
						printReport(feedbackCounts);
						break;
					}
					
					NodeFeedbackPair nodeFeedbackPair = DebugInfo.getNodeFeedbackPair();
					propagator.responseToFeedback(nodeFeedbackPair);
					
					final UserFeedback gtFeedback = nodeFeedbackPair.getFeedback();
					
					System.out.println("Groud Truth Feedback: " + gtFeedback);
					
					if (gtFeedback.equals(feedback)) {
						System.out.println("Predicted feedback correctly");
					} else {
						feedbackCounts++;
					}
					
					if (gtFeedback.getFeedbackType() == UserFeedback.WRONG_PATH) {
						currentNode = currentNode.getControlDominator();
					} else if (gtFeedback.getFeedbackType() == UserFeedback.WRONG_VARIABLE_VALUE) {
						VarValue wrongVar = gtFeedback.getOption().getReadVar();
						currentNode = buggyView.getTrace().findDataDependency(currentNode, wrongVar);
					}
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
	
	private boolean isIOReady() {
		return !DebugInfo.getInputs().isEmpty() && !DebugInfo.getOutputs().isEmpty();
	}
	
	private void jumpToNode(final TraceNode targetNode) {
		Display.getDefault().asyncExec(new Runnable() {
		    @Override
		    public void run() {
				Trace buggyTrace = buggyView.getTrace();
				buggyView.jumpToNode(buggyTrace, targetNode.getOrder(), true);
		    }
		});
	}

	private void printReport(final int noOfFeedbacks) {
		System.out.println("---------------------------------");
		System.out.println("Number of feedbacks: " + noOfFeedbacks);
		System.out.println("---------------------------------");
	}
	
	private TraceNode getStartingNode(final Trace trace, final VarValue output) {
		for (int order = trace.size()-1; order>=0; order--) {
			TraceNode node = trace.getTraceNode(order);
			final String varID = output.getVarID();
			if (node.isReadVariablesContains(varID)) {
				return node;
			} else if (node.isWrittenVariablesContains(varID)) {
				return node;
			}
		}
		return null;
	}
}
