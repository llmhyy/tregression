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

import com.sun.security.ntlm.Client;

import microbat.debugpilot.DebugPilotInfo;
import microbat.debugpilot.NodeFeedbacksPair;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.util.JavaUtil;
import microbat.util.TraceUtil;
import microbat.util.UniquePriorityQueue;
import tregression.empiricalstudy.EmpiricalTrial;
import tregression.empiricalstudy.TrialGenerator0;
import tregression.empiricalstudy.config.Defects4jProjectConfig;
import tregression.empiricalstudy.config.ProjectConfig;
import tregression.separatesnapshots.DiffMatcher;
import tregression.views.BuggyTraceView;
import tregression.views.CorrectTraceView;
import microbat.recommendation.ChosenVariableOption;
import microbat.recommendation.UserFeedback;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;

import tregression.auto.AutoDebugPilotAgent;
import tregression.auto.result.DebugResult;
//import dataset.BugDataset;
//import dataset.BugDataset.BugData;
//import dataset.bug.minimize.ProjectMinimizer;
import tregression.auto.result.RunResult;
import jmutation.utils.TraceHelper;

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
				
		        execute();
				
				return Status.OK_STATUS;
			}
		};
		job.schedule();
		return null;
	}
	
//	private void execute() {
//		DummyClient client = new DummyClient("127.0.0.5", 8085);
//		try {
//			client.conntectServer();
//			Trace trace = this.buggyView.getTrace();
//			for (TraceNode node : trace.getExecutionList()) {
//				if (!(node.getOrder() >= 530)) {
//					continue;
//				}
//				for (VarValue readVar : node.getReadVariables()) {
//					if (!readVar.isThisVariable()) {
//						client.notifyContinuoue();
//						client.sendVariableVector(readVar);
//						System.out.println(readVar.getVarName());
//					}
//				}
//				
//				for (VarValue writtenVar : node.getWrittenVariables()) {
//					if (!writtenVar.isThisVariable()) {
//						client.notifyContinuoue();
//						client.sendVariableVector(writtenVar);
//						System.out.println(writtenVar.getVarName());
//					}
//				}
//			}
//			client.notifyStop();
//			client.disconnectServer();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//	}
	private void execute() {
		final Trace trace = this.buggyView.getTrace();
		final TraceNode startNode = trace.getTraceNode(338);
		final TraceNode endNode = trace.getTraceNode(237);
		
		UniquePriorityQueue<TraceNode> toVisitNodes = new UniquePriorityQueue<>(new Comparator<TraceNode>() {
			@Override
			public int compare(TraceNode t1, TraceNode t2) {
				return t2.getOrder() - t1.getOrder();
			}
		});
		
		toVisitNodes.add(startNode);
		
		while (!toVisitNodes.isEmpty()) {
			final TraceNode node = toVisitNodes.poll();
			if (node.equals(endNode)) {
				System.out.println("Path exists");
				return;
			}
			for (VarValue readVar : node.getReadVariables()) {
				final TraceNode dataDom = trace.findDataDependency(node, readVar);
				if (dataDom != null) {
					toVisitNodes.add(dataDom);
				}
			}
			
			final TraceNode controlDom = node.getControlDominator();
			if (controlDom != null) {
				toVisitNodes.add(controlDom);
			}
		}
		
		System.out.println("Path does not exists");
		
	}
	
	protected TraceNode getStartingNode(final Trace trace, final VarValue output) {
		for (int order = trace.size(); order>=0; order--) {
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
	private void jumpToNode(final TraceNode targetNode) {
		Display.getDefault().asyncExec(new Runnable() {
		    @Override
		    public void run() {
				Trace buggyTrace = buggyView.getTrace();
				buggyView.jumpToNode(buggyTrace, targetNode.getOrder(), true);
		    }
		});
	}
}
