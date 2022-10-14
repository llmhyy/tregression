package tregression.handler;

import java.nio.file.Path;
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

import microbat.Activator;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.preference.MicrobatPreference;
import microbat.recommendation.UserFeedback;
import microbat.util.JavaUtil;
import tregression.autofeedback.AutoFeedbackMethod;
import tregression.autofeedback.FeedbackGenerator;
import tregression.autofeedbackevaluation.AccMeasurement;
import tregression.autofeedbackevaluation.AutoDebugEvaluator;
import tregression.autofeedbackevaluation.AvgAccMeasurement;
import tregression.preference.TregressionPreference;
import tregression.views.BuggyTraceView;
import tregression.views.CorrectTraceView;

public class AutoFeedbackHandler extends AbstractHandler {

	private BuggyTraceView buggyView;
	private CorrectTraceView correctView;
	
	private final long sleepTime = 2000;
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		JavaUtil.sourceFile2CUMap.clear();
		
		Job job = new Job("Do evaluation") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				
				// Get the trace view
				getTraveViews();
				
				// Get the buggy trace
				Trace buggyTrace = buggyView.getTrace();
				
				// Determine the method for debugging
				AutoFeedbackMethod method = getMethod();
				
				// Construct feedback generator
				FeedbackGenerator generator = FeedbackGenerator.getFeedbackGenerator(buggyTrace, method);
				generator.setVerbal(true);
				
				System.out.println();
				System.out.println("AutoFeedback: Start debugging");
				/**
				 * By default, we assume that the very last node is the error node
				 */
				TraceNode errorNode = null;
				for (int order = buggyTrace.size(); order>=1; order--) {
					TraceNode node = buggyTrace.getTraceNode(order);
					if (node.getCodeStatement() == "}") {
						continue;
					}
					if (node.isThrowingException()) {
						errorNode = node.getControlDominator();
					} else {
						errorNode = node;
					}
					break;
				}
				
				if (errorNode == null) {
					throw new RuntimeException("Error node cannot be defined");
				}
				
				UserFeedback feedback = new UserFeedback(UserFeedback.WRONG_PATH);
				TraceNode currentNode = errorNode;
				TraceNode prevNode = null;
				TraceNode rootCauseNode = null;
				while (feedback.getFeedbackType() == UserFeedback.WRONG_PATH || feedback.getFeedbackType() == UserFeedback.WRONG_VARIABLE_VALUE) {
					
					focusNode(currentNode);
					
					feedback = generator.giveFeedback(currentNode);
					
					if (feedback == null) {
						rootCauseNode = prevNode;
						break;
					}
					
					if (feedback.getFeedbackType() == UserFeedback.CORRECT || feedback.getFeedbackType() == UserFeedback.UNCLEAR) {
						rootCauseNode = prevNode;
						break;
					} else if (feedback.getFeedbackType() == UserFeedback.WRONG_PATH) {
						prevNode = currentNode;
						currentNode = currentNode.getControlDominator();
					} else {
						prevNode = currentNode;
						VarValue wrongVar = feedback.getOption().getReadVar();
						currentNode = buggyTrace.findDataDependency(currentNode, wrongVar);
					}
					
					if (currentNode == null) {
						rootCauseNode = prevNode;
						break;
					}
					
					if (currentNode.equals(prevNode)) {
						rootCauseNode = prevNode;
						break;
					}
				}
				
				if (rootCauseNode == null) {
					throw new RuntimeException("Cannot find the root cause");
				}
				focusNode(rootCauseNode);
				System.out.println("Root Cause is found to be node: " + rootCauseNode.getOrder());
				return Status.OK_STATUS;
			}
		};
		
		job.schedule();
		
		return null;
	}
	
	private void getTraveViews() {
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
	
	private AutoFeedbackMethod getMethod() {
		String selectedMethodName = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.AUTO_FEEDBACK_METHOD);
		AutoFeedbackMethod selectedMethod = AutoFeedbackMethod.valueOf(selectedMethodName);
		return selectedMethod;
	}
	
	private void focusNode(final TraceNode node) {
		Display.getDefault().asyncExec(new Runnable() {
		    @Override
		    public void run() {
				buggyView.jumpToNode(buggyView.getTrace(), node.getOrder(), true);
				try {
					Thread.sleep(sleepTime);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
		    }
		});
	}
}
