package tregression.handler;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
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

import jmutation.MutationFramework;
import jmutation.model.MutationResult;
import jmutation.model.Project;
import jmutation.model.TestCase;
import jmutation.model.TestIO;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.recommendation.ChosenVariableOption;
import microbat.recommendation.UserFeedback;
import microbat.stepvectorizer.StepVectorizer;
import microbat.util.JavaUtil;
import tracediff.TraceDiff;
import tracediff.model.PairList;
import tracediff.model.TraceNodePair;
import microbat.Activator;
import microbat.baseline.encoders.NodeFeedbackPair;
import microbat.baseline.encoders.ProbabilityEncoder;
import tregression.StepChangeType;
import tregression.StepChangeTypeChecker;
import tregression.autofeedback.AutoFeedbackMethods;
import tregression.empiricalstudy.RootCauseFinder;
import tregression.empiricalstudy.RootCauseNode;
import tregression.empiricalstudy.config.ConfigFactory;
import tregression.preference.TregressionPreference;
import tregression.separatesnapshots.DiffMatcher;
import tregression.views.BuggyTraceView;
import tregression.views.CorrectTraceView;
import tregression.views.Visualizer;

public class TestingHandler extends AbstractHandler {

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
				final String srcFolderPath = "src\\main\\java";
				final String testFolderPath = "src\\main\\test";
				
				// Mutation framework will mutate the target project
				MutationFramework mutationFramework = new MutationFramework();

				mutationFramework.setProjectPath("C:/Users/arkwa/git/java-mutation-framework/sample/math_70");
				mutationFramework.setDropInsDir("C:/Users/arkwa/git/java-mutation-framework/lib");
				mutationFramework.setMicrobatConfigPath("C:\\Users\\arkwa\\git\\java-mutation-framework\\sampleMicrobatConfig.json");
				
				String testCaseID_str = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.BUG_ID);
				final int testCaesID = Integer.parseInt(testCaseID_str);
				
				System.out.println("testing on test case id: " + testCaesID);
				TestCase testCase = mutationFramework.getTestCases().get(testCaesID);
				mutationFramework.setTestCase(testCase);
				
				MutationResult result = mutationFramework.startMutationFramework();
				
				Project mutatedProject = result.getMutatedProject();
				Project originalProject = result.getOriginalProject();
				
				final Trace buggyTrace = result.getMutatedTrace();
				final Trace correctTrace = result.getOriginalTrace();
				
				// Convert tracediff.PairList to tregression.PairList
				PairList pairList = TraceDiff.getTraceAlignment(srcFolderPath, testFolderPath,
	                    mutatedProject.getRoot().getAbsolutePath(), originalProject.getRoot().getAbsolutePath(),
	                    result.getMutatedTrace(), result.getOriginalTrace());
				List<tregression.model.TraceNodePair> pairLTregression = new ArrayList<>();
				for (TraceNodePair pair : pairList.getPairList()) {
					pairLTregression.add(new tregression.model.TraceNodePair(pair.getBeforeNode(), pair.getAfterNode()));
				}
				final tregression.model.PairList pairListTregression = new tregression.model.PairList(pairLTregression);
				
				// Set up the diffMatcher
				final DiffMatcher matcher = new DiffMatcher(srcFolderPath, testFolderPath, mutatedProject.getRoot().getAbsolutePath(), originalProject.getRoot().getAbsolutePath());
				matcher.matchCode();
				
				// Update view
				updateView(buggyTrace, correctTrace, pairListTregression, matcher);
				
				if (result.getRootCauses().isEmpty()) {
					System.out.println("Skip because root cause is empty: " + testCaesID);
					return null;
				}
				
				if (result.getTestIOs().isEmpty()) {
					System.out.println("Skep because IO is empty: " + testCaesID);
					return null;
				}
				
				// Get the ground truth root cause
				List<TraceNode> rootCauses = result.getRootCauses();
				TraceNode rootCause = rootCauses.get(rootCauses.size()-1);
				
				// Start debugging 
				final int maxItr = Math.min((int) (buggyTrace.size() * 0.75), 20);
				int noOfFeedbacks = 0;
				
				ProbabilityEncoder encoder = new ProbabilityEncoder(buggyTrace);
				
				// Set up input and output variables
				List<VarValue> inputs = result.getTestIOs().get(result.getTestIOs().size()-1).getInputs();
				List<VarValue> outputs = result.getTestIOs().get(result.getTestIOs().size()-1).getOutputs();
				
				for (VarValue inputVar : inputs) {
					System.out.println("Input: " + inputVar.getVarID());
				}
				
				for (VarValue outputVar : outputs) {
					System.out.println("Output: " + outputVar.getVarID());
				}
				
				encoder.setInputVars(inputs);
				encoder.setOutputVars(outputs);
				encoder.setup();
				// Set up visited trace node order which users have already give the feedback
				List<Integer> visitedNodeOrder = new ArrayList<>();
				int startPointer = encoder.getSlicedExecutionList().get(0).getOrder();
				
				// Set up type checker and root cause finder for feedback
				StepChangeTypeChecker typeChecker = new StepChangeTypeChecker(buggyTrace, correctTrace);
				RootCauseFinder finder = new RootCauseFinder();
				finder.setRootCauseBasedOnDefects4J(pairListTregression, matcher, buggyTrace, correctTrace);
				
				while (noOfFeedbacks <= maxItr) {
					System.out.println("---------------------------------- " + noOfFeedbacks + " iteration");
					
					// Make sure that the python server is available before calling this function
					encoder.encode();
					
					// Predicted root cause
					TraceNode prediction = encoder.getMostErroneousNode();
					
					// Visualize the prediction
					jumpToNode(prediction);
					
					System.out.println("Ground Truth: " + rootCause.getOrder() + ", Prediction: " + prediction.getOrder());
					if (prediction.getOrder() == rootCause.getOrder()) {
						// Baseline have found the root cause !
						break;
					}
					
					// If baseline cannot find the root cause, we need to find a node to ask for feedback
					TraceNode nextInspectingNode = prediction;
					if (visitedNodeOrder.contains(nextInspectingNode.getOrder())) {
						while (visitedNodeOrder.contains(startPointer)) {
							startPointer++;
						}
						nextInspectingNode = buggyTrace.getTraceNode(startPointer);
					}
					System.out.println("Asking feedback for node: " + nextInspectingNode.getOrder());
					
					// Collect feedback from correct trace
					StepChangeType type = typeChecker.getType(nextInspectingNode, true, buggyView.getPairList(), buggyView.getDiffMatcher());
					UserFeedback feedback = typeToFeedback(type, nextInspectingNode, true, PlayRegressionLocalizationHandler.finder);
					System.out.println("Feedback for node: " + nextInspectingNode.getOrder() + " is " + feedback);
					
					// Add feedback information into probability encoder
					NodeFeedbackPair pair = new NodeFeedbackPair(nextInspectingNode, feedback);
					ProbabilityEncoder.addFeedback(pair);
					
					noOfFeedbacks += 1;
					visitedNodeOrder.add(nextInspectingNode.getOrder());
				}
				
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
	
	protected List<TraceNode> findObservedFaultList(Trace buggyTrace, Trace correctTrace,
			tregression.model.PairList pairList, DiffMatcher matcher) {
		List<TraceNode> observedFaultList = new ArrayList<>();
		TraceNode initalStep = buggyTrace.getLatestNode();
		TraceNode lastObservableFault = findObservedFault(initalStep, buggyTrace, correctTrace, pairList, matcher);
		
		if (lastObservableFault != null) {
			observedFaultList.add(lastObservableFault);

			StepChangeTypeChecker checker = new StepChangeTypeChecker(buggyTrace, correctTrace);
			TraceNode node = lastObservableFault.getStepOverPrevious();
			
			int times = 5;
			while(observedFaultList.size() < times && node!= null){
				
				StepChangeType changeType = checker.getType(node, true, pairList, matcher);
				if(changeType.getType()!=StepChangeType.IDT){
					observedFaultList.add(node);
				}
				
				node = node.getStepOverPrevious();
			}
		}
		return observedFaultList;
	}
	protected TraceNode findObservedFault(TraceNode node, Trace buggyTrace, Trace correctTrace,
			tregression.model.PairList pairList, DiffMatcher matcher){
		StepChangeTypeChecker checker = new StepChangeTypeChecker(buggyTrace, correctTrace);
		TraceNode firstTearDownNode = firstPreviousNodeInvokedByTearDown(node);
		System.currentTimeMillis();
		if(firstTearDownNode!=null){
			node = firstTearDownNode.getStepInPrevious();
		}
		
		while(node != null) {
			StepChangeType changeType = checker.getType(node, true, pairList, matcher);
			if(changeType.getType()==StepChangeType.CTL) {
				TraceNode cDom = node.getInvocationMethodOrDominator();
				if(cDom==null){
					if(node.isException()) {
						return node;
					}	
					else{
						node = node.getStepInPrevious();
						continue;
					}
				}
				
				StepChangeType cDomType = checker.getType(cDom, true, pairList, matcher);
				if(cDomType.getType()==StepChangeType.IDT){
					TraceNode stepOverPrev = node.getStepOverPrevious();
					if(stepOverPrev!=null){
						if(stepOverPrev.equals(cDom) && stepOverPrev.isBranch() && !stepOverPrev.isConditional()){
							node = node.getStepInPrevious();
							continue;
						}
					}
				}
				
				return node;
			}
			else if(changeType.getType()!=StepChangeType.IDT){
				return node;
			}
			
			node = node.getStepInPrevious();
		}
		
		return null;
	}
	
	private TraceNode firstPreviousNodeInvokedByTearDown(TraceNode node) {
		TraceNode prev = node.getStepInPrevious();
		if(prev==null) {
			return null;
		}
		
		TraceNode returnNode = null;
		
		boolean isInvoked = isInvokedByTearDownMethod(prev);
		if(isInvoked){
			returnNode = prev;
		}
		else{
			return null;
		}
		
		while(prev != null){
			prev = prev.getStepInPrevious();
			if(prev==null){
				return null;
			}
			
			isInvoked = isInvokedByTearDownMethod(prev);
			if(isInvoked){
				if(returnNode==null){
					returnNode = prev;
				}
				else if(prev.getOrder()<returnNode.getOrder()){
					returnNode = prev;					
				}
			}
			else{
				returnNode = prev;
				break;
			}
		}
		
		return returnNode;
	}
	
	private boolean isInvokedByTearDownMethod(TraceNode node) {
		TraceNode n = node;
		while(n!=null) {
			if(n.getMethodSign()!=null && n.getMethodSign().contains("tearDown()V")) {
				return true;
			}
			else {
				n = n.getInvocationParent();
			}
		}
		
		return false;
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
	
	private AutoFeedbackMethods getMethod() {
		String selectedMethodName = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.AUTO_FEEDBACK_METHOD);
		AutoFeedbackMethods selectedMethod = AutoFeedbackMethods.valueOf(selectedMethodName);
		return selectedMethod;
	}
	
	private String getProjectName() {
		return Activator.getDefault().getPreferenceStore().getString(TregressionPreference.PROJECT_NAME);
	}
	
	private String getBugID() {
		return Activator.getDefault().getPreferenceStore().getString(TregressionPreference.BUG_ID);
	}
}
