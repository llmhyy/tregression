package tregression.handler;

import java.util.ArrayList;
import java.util.Collection;
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
import microbat.baseline.probpropagation.StepwisePropagator;
import microbat.handler.RequireIO;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.recommendation.UserFeedback;
import tregression.views.BuggyTraceView;
import tregression.views.CorrectTraceView;
import tregression.views.StepDetailIOUI;

public class StepwisePropagationHandler extends AbstractHandler {

	private BuggyTraceView buggyView;
	private CorrectTraceView correctView;
	
//	private List<VarValue> inputs = new ArrayList<>();
//	private List<VarValue> outputs = new ArrayList<>();
	
//	private static boolean registerFlag = false;
//	
//	private static UserFeedback manualFeedback = null;
//	private static TraceNode feedbackNode = null;
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		Job job = new Job("Testing Tregression") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				
//				if (!StepwisePropagationHandler.registerFlag) {
//					registerHandler();
//					return Status.OK_STATUS;
//				}
				
				setup();
				
				if (!isReady()) {
					return Status.OK_STATUS;
				}
				
				List<VarValue> inputs = DebugInfo.getInputs();
				List<VarValue> outputs = DebugInfo.getOutputs();
				StepwisePropagator propagator = new StepwisePropagator(buggyView.getTrace(), inputs, outputs);
				
				int feedbackCounts = 0;
				final int maxItr = (int) buggyView.getTrace().size();
				while (feedbackCounts<=maxItr) {
					System.out.println("---------------------------------- " + feedbackCounts + " iteration");
					System.out.println("Propagation Start");
					propagator.propagate();
					System.out.println();
					System.out.println("Propagation End");
					TraceNode rootCause = propagator.proposeRootCause();
					jumpToNode(rootCause);
					System.out.println("Proposed Root Cause: " + rootCause.getOrder());
					
					
					System.out.println("Please give a feedback");
					DebugInfo.waitForFeedback();
					NodeFeedbackPair nodeFeedbackPair = DebugInfo.getNodeFeedbackPair();
//					while (!StepwisePropagationHandler.isManualFeedbackReady()) {
//						try {
//							Thread.sleep(200);
//						} catch (InterruptedException e) {
//							
//						}
//					}
//					
//					UserFeedback feedback = StepwisePropagationHandler.manualFeedback;
//					TraceNode feedbackNode = StepwisePropagationHandler.feedbackNode;
//					NodeFeedbackPair nodeFeedbackPair = new NodeFeedbackPair(feedbackNode, feedback);
//					StepwisePropagationHandler.resetManualFeedback();
					
					propagator.responseToFeedback(nodeFeedbackPair);
					feedbackCounts += 1;
				}

				return Status.OK_STATUS;
			}
			
		};
		
		job.schedule();
		return null;
	}
	
	private boolean isReady() {
		if (this.buggyView == null) {
			throw new RuntimeException("StepwisePropagationHandler: Buggy view is not ready");
		}
		
		if (this.correctView == null) {
			throw new RuntimeException("StepwisePropagationHandler: Correct view is not ready");
		}
		
		if (DebugInfo.getInputs().isEmpty()) {
			throw new RuntimeException("StepwisePropagationHandler: There are no inputs");
		}
		
		if (DebugInfo.getOutputs().isEmpty()) {
			throw new RuntimeException("StepwisePropagationHandler: There are no outputs");
		}
		
		return true;
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

	private void jumpToNode(final TraceNode targetNode) {
		Display.getDefault().asyncExec(new Runnable() {
		    @Override
		    public void run() {
				Trace buggyTrace = buggyView.getTrace();
				buggyView.jumpToNode(buggyTrace, targetNode.getOrder(), true);
		    }
		});
	}
	
//	@Override
//	public void registerHandler() {
//		StepDetailIOUI.registerHandler(this);
//		StepwisePropagationHandler.registerFlag = true;
//		
//		System.out.println();
//		System.out.println("StepwisePropagationHandler is now registered to buttons");
//		System.out.println("Please select inputs and outputs");
//	}
//
//	@Override
//	public void addInputs(Collection<VarValue> inputs) {
//		this.inputs.addAll(inputs);		
//		for (VarValue input : this.inputs) {
//			System.out.println("StepwisePropagationHandler: Selected Inputs: " + input.getVarID());
//		}
//	}
//
//	@Override
//	public void addOutputs(Collection<VarValue> outputs) {
//		this.outputs.addAll(outputs);
//		
//		for (VarValue output : this.outputs) {
//			System.out.println("StepwisePropagationHandler: Selected Outputs: " + output.getVarID());
//		}
//	}
//
//	@Override
//	public void printIO() {
//		for (VarValue input : this.inputs) {
//			System.out.println("StepwisePropagationHandler: Selected Inputs: " + input.getVarID());
//		}
//		for (VarValue output : this.outputs) {
//			System.out.println("StepwisePropagationHandler: Selected Outputs: " + output.getVarID());
//		}
//	}
//
//	@Override
//	public void clearData() {
//		this.inputs.clear();
//		this.outputs.clear();
//		System.out.println("Clear Data");
//	}
//	
//	public static boolean isManualFeedbackReady() {
//		return StepwisePropagationHandler.manualFeedback != null && StepwisePropagationHandler.feedbackNode != null;
//	}
//	
//	public static void setManualFeedback(UserFeedback manualFeedback, TraceNode node) {
//		StepwisePropagationHandler.manualFeedback = manualFeedback;
//		StepwisePropagationHandler.feedbackNode = node;
//	}
//	
//	public static void resetManualFeedback() {
//		StepwisePropagationHandler.manualFeedback = null;
//		StepwisePropagationHandler.feedbackNode = null;
//	}
	
}
