package tregression.handler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

import baseline.AskingAgent;
import microbat.baseline.encoders.NodeFeedbackPair;
import microbat.baseline.encoders.ProbabilityEncoder;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.recommendation.ChosenVariableOption;
import microbat.recommendation.UserFeedback;
import microbat.util.JavaUtil;
import tregression.StepChangeType;
import tregression.StepChangeTypeChecker;
import tregression.empiricalstudy.RootCauseFinder;
import tregression.empiricalstudy.Simulator;
import tregression.model.PairList;
import tregression.separatesnapshots.DiffMatcher;
import tregression.views.BuggyTraceView;
import tregression.views.CorrectTraceView;

public class BaselineHandler extends AbstractHandler {
	
	private BuggyTraceView buggyView;
	private CorrectTraceView correctView;
	
	public static String MUTATED_PROJECT_PATH = null;
	public static String ORIGINAL_PROJECT_PATH = null;
	
	public static TraceNode rootCause = null;
	
	public static List<VarValue> inputs = null;
	public static List<VarValue> outputs = null;
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		JavaUtil.sourceFile2CUMap.clear();
		Job job = new Job("Testing Tregression") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				
				// Call setup before isReady method
				setup();
				if (!isReady()) {
					throw new RuntimeException("Baseline Handler is not ready");
				}

				final Trace buggyTrace = buggyView.getTrace();
				final Trace correctTrace = correctView.getTrace();
				
				final PairList pairList = buggyView.getPairList();
				final DiffMatcher matcher = buggyView.getDiffMatcher();

				final int maxItr = Math.min((int) (buggyTrace.size() * 0.75), 20);
				int noOfFeedbacks = 0;
				
				// Set up the probability encoder
				ProbabilityEncoder encoder = new ProbabilityEncoder(buggyTrace);
				encoder.setInputVars(BaselineHandler.inputs);
				encoder.setOutputVars(BaselineHandler.outputs);
				encoder.setup();
				
				// getSlicedExecutionList should be called after encoder.setup is called
				AskingAgent askingAgent = new AskingAgent(encoder.getSlicedExecutionList());
				
				// Set up type checker and root cause finder for feedback
				StepChangeTypeChecker typeChecker = new StepChangeTypeChecker(buggyTrace, correctTrace);
				Simulator simulator = new Simulator(false, false, 3);
				simulator.prepare(buggyTrace, correctTrace, pairList, matcher);
				RootCauseFinder finder = new RootCauseFinder();
				finder.setRootCauseBasedOnDefects4J(pairList, matcher, buggyTrace, correctTrace);
				finder.checkRootCause(simulator.getObservedFault(), buggyTrace, correctTrace, pairList, matcher);
				
				while (noOfFeedbacks <= maxItr) {
					System.out.println("---------------------------------- " + noOfFeedbacks + " iteration");
					
					// Encoder the probability
					encoder.encode();
					
					// Prediction root cause
					TraceNode prediction = encoder.getMostErroneousNode();
					
					// Visualize the prediction
					jumpToNode(prediction);
					
					// Check is root cause is correct
					System.out.println("Ground Truth: " + rootCause.getOrder() + ", Prediction: " + prediction.getOrder());
					if (prediction.getOrder() == rootCause.getOrder()) {
						// Baseline have found the root cause !
						break;
					}
					
					boolean isVisitedNode = askingAgent.isVisitedNode(prediction);
					TraceNode nextNode = prediction;
					if (isVisitedNode) {
						int nextNodeOrder = askingAgent.getNodeOrderToBeAsked();
						nextNode = buggyTrace.getTraceNode(nextNodeOrder);
					}
					
					System.out.println("Asking feedback for node: " + nextNode.getOrder());
					
					// Collect feedback from correct trace
					StepChangeType type = typeChecker.getType(nextNode, true, buggyView.getPairList(), buggyView.getDiffMatcher());
					UserFeedback feedback = typeToFeedback(type, nextNode, true, finder);
					System.out.println("Feedback for node: " + nextNode.getOrder() + " is " + feedback);
					
					// Add feedback information into probability encoder
					NodeFeedbackPair pair = new NodeFeedbackPair(nextNode, feedback);
					ProbabilityEncoder.addFeedback(pair);
					
					noOfFeedbacks += 1;
					askingAgent.addVisistedNodeOrder(nextNode.getOrder());
				}
				
				BaselineHandler.clearData();
				return Status.OK_STATUS;
			}
			
		};
		
		job.schedule();
		return null;
	}
	
	private UserFeedback typeToFeedback(StepChangeType type, TraceNode node, boolean isOnBefore, RootCauseFinder finder) {
		UserFeedback feedback = new UserFeedback();
		switch(type.getType()) {
		case StepChangeType.IDT:
			feedback.setFeedbackType(UserFeedback.CORRECT);
			break;
		case StepChangeType.CTL:
			feedback.setFeedbackType(UserFeedback.WRONG_PATH);
			break;
		case StepChangeType.DAT:
			feedback.setFeedbackType(UserFeedback.WRONG_VARIABLE_VALUE);
			VarValue wrongVar = type.getWrongVariable(node, isOnBefore, finder);
			feedback.setOption(new ChosenVariableOption(wrongVar, null));
			break;
		case StepChangeType.SRC:
			feedback.setFeedbackType(UserFeedback.UNCLEAR);
			break;
		}
		return feedback;
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
	
	private boolean isReady() {
		return 	BaselineHandler.MUTATED_PROJECT_PATH != null &&
				BaselineHandler.ORIGINAL_PROJECT_PATH != null &&
				BaselineHandler.rootCause != null &&
				BaselineHandler.inputs != null &&
				BaselineHandler.outputs != null &&
				this.buggyView != null &&
				this.correctView != null;
	}
	
	public static void setMutatedProPath(String path) {
		BaselineHandler.MUTATED_PROJECT_PATH = path;
	}
	
	public static void setOriginalProPath(String path) {
		BaselineHandler.ORIGINAL_PROJECT_PATH = path;
	}
	
	public static void setRootCause(TraceNode rootCause) {
		BaselineHandler.rootCause = rootCause;
	}
	
	public static void setInputs(List<VarValue> inputs) {
		BaselineHandler.inputs = inputs;
	}
	
	public static void setOutputs(List<VarValue> outputs) {
		BaselineHandler.outputs = outputs;
	}
	
	public static void clearIO() {
		if (BaselineHandler.inputs != null) {
			BaselineHandler.inputs.clear();
		}
		
		if (BaselineHandler.outputs != null) {
			BaselineHandler.outputs.clear();
		}
		
		System.out.println("BaselineHandler: Clear IO");
	}
	
	public static void addInputs(List<VarValue> inputs) {
		if (BaselineHandler.inputs == null) {
			BaselineHandler.inputs = new ArrayList<>();
		}
		BaselineHandler.inputs.addAll(inputs);
		
		for (VarValue input : BaselineHandler.inputs) {
			System.out.println("BaselineHandler: Selected Inputs: " + input.getVarID());
		}
	}
	
	public static void printIO() {
		for (VarValue input : BaselineHandler.inputs) {
			System.out.println("BaselineHandler: Selected Inputs: " + input.getVarID());
		}
		for (VarValue output : BaselineHandler.outputs) {
			System.out.println("BaselineHandler: Selected Outputs: " + output.getVarID());
		}
	}
	
	public static void addOutpus(List<VarValue> outputs) {
		if (BaselineHandler.outputs == null) {
			BaselineHandler.outputs = new ArrayList<>();
		}
		BaselineHandler.outputs.addAll(outputs);
		
		for (VarValue output : BaselineHandler.outputs) {
			System.out.println("BaselineHandler: Selected Outputs: " + output.getVarID());
		}
	}
	
	public static void clearData() {
		BaselineHandler.MUTATED_PROJECT_PATH = null;
		BaselineHandler.ORIGINAL_PROJECT_PATH = null;
		
		BaselineHandler.rootCause = null;
		
		BaselineHandler.inputs = null;
		BaselineHandler.outputs = null;
	}

}
