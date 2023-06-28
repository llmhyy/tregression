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
import debuginfo.NodeFeedbacksPair;
import microbat.Activator;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.util.JavaUtil;
import tregression.model.PairList;
import tregression.preference.TregressionPreference;
import tregression.separatesnapshots.DiffMatcher;
import tregression.views.BuggyTraceView;
import tregression.views.CorrectTraceView;
import tregression.auto.AutoDebugAgent;
import tregression.empiricalstudy.DeadEndRecord;
import tregression.empiricalstudy.EmpiricalTrial;
import tregression.auto.result.DebugResult;
import tregression.auto.result.RunResult;

public class AutoFeedbackHandler extends AbstractHandler {

	private BuggyTraceView buggyView;
	private CorrectTraceView correctView;	

	public static EmpiricalTrial trial = null;
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		JavaUtil.sourceFile2CUMap.clear();
		
		Job job = new Job("Do evaluation") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				setup();
				execute();
				return Status.OK_STATUS;
			}
		};
		
		job.schedule();
		return null;
	}
	
	protected void execute() {
		if (!this.isIOReady()) {
			System.out.println("Please provide the inputs and outputs");
			return;
		}
		
		if (AutoFeedbackHandler.trial == null) {
			System.out.println("Trial is null");
			return;
		}
		
		
		// Get input and output from user
		final List<VarValue> inputs = DebugInfo.getInputs();
		final List<VarValue> outputs = DebugInfo.getOutputs();
		VarValue output = outputs.get(0);
		TraceNode outputNode = null;
		if (output.getVarID().startsWith("CR_")) {
			NodeFeedbacksPair initPair = DebugInfo.getNodeFeedbackPair();
			outputNode = initPair.getNode();
		} else {
			outputNode = this.getStartingNode(this.buggyView.getTrace(), output);
		}
		
		// Project configuration
		String projectName = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.PROJECT_NAME);
		String id = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.BUG_ID);
		
		// Store basic information 
		RunResult result = new RunResult();
		result.projectName = projectName;
		result.bugID = Integer.valueOf(id);
		result.traceLen = this.buggyView.getTrace().size();
		result.isOmissionBug = trial.getBugType() == EmpiricalTrial.OVER_SKIP;
		result.rootCauseOrder = trial.getRootcauseNode().getOrder();
		for (DeadEndRecord record : trial.getDeadEndRecordList()) {
			result.solutionName += record.getSolutionPattern().getTypeName() + ":";
		}
		
		// AutoFeedbackHandler.trail should be defined at SeparateVersionHandler
		AutoDebugAgent agent = new AutoDebugAgent(AutoFeedbackHandler.trial, inputs, outputs, outputNode);
		DebugResult debugResult = agent.startDebug(result);
		System.out.println(debugResult.getFormattedInfo());
	}

	protected void setup() {
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
	
	protected void jumpToNode(final TraceNode targetNode) {
		Display.getDefault().asyncExec(new Runnable() {
		    @Override
		    public void run() {
				Trace buggyTrace = buggyView.getTrace();
				buggyView.jumpToNode(buggyTrace, targetNode.getOrder(), true);
		    }
		});
	}
	
	protected boolean isIOReady() {
		return !DebugInfo.getInputs().isEmpty() && !(DebugInfo.getOutputs().isEmpty());
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
}
