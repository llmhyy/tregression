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
import jmutation.model.TestCase;
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
import microbat.baseline.beliefpropagation.NodeFeedbackPair;
import microbat.baseline.beliefpropagation.PropabilityInference;
import tregression.StepChangeType;
import tregression.StepChangeTypeChecker;
import tregression.autofeedback.AutoFeedbackMethod;
import tregression.empiricalstudy.RootCauseFinder;
import tregression.empiricalstudy.RootCauseNode;
import tregression.empiricalstudy.Simulator;
import tregression.empiricalstudy.config.ConfigFactory;
import tregression.preference.TregressionPreference;
import tregression.separatesnapshots.DiffMatcher;
import tregression.views.BuggyTraceView;
import tregression.views.CorrectTraceView;
import tregression.views.Visualizer;

public class TestingHandler extends AbstractHandler {

	private BuggyTraceView buggyView;
	private CorrectTraceView correctView;
	
	private final int maxMutationLimit = 10;
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		JavaUtil.sourceFile2CUMap.clear();
		Job job = new Job("Testing Tregression") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				// Access the buggy view and correct view
				setup();
				
//				String selection = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.SEED);
//				System.out.println(selection);
				
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
	
	private AutoFeedbackMethod getMethod() {
		String selectedMethodName = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.AUTO_FEEDBACK_METHOD);
		AutoFeedbackMethod selectedMethod = AutoFeedbackMethod.valueOf(selectedMethodName);
		return selectedMethod;
	}
	
	private String getProjectName() {
		return Activator.getDefault().getPreferenceStore().getString(TregressionPreference.PROJECT_NAME);
	}
	
	private String getBugID() {
		return Activator.getDefault().getPreferenceStore().getString(TregressionPreference.BUG_ID);
	}
}
