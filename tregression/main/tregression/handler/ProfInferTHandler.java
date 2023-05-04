package tregression.handler;

import java.util.ArrayList;
import java.util.Collection;
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

import debuginfo.DebugInfo;
import debuginfo.NodeFeedbackPair;
import microbat.Activator;
//import microbat.handler.ProbInferHandler;
import microbat.handler.RequireIO;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.probability.BP.BeliefPropagation;
import microbat.recommendation.ChosenVariableOption;
import microbat.recommendation.UserFeedback;
import microbat.util.JavaUtil;
import microbat.views.TraceView;
import mutation.AskingAgent;
import tregression.StepChangeType;
import tregression.StepChangeTypeChecker;
import tregression.empiricalstudy.RootCauseFinder;
import tregression.empiricalstudy.Simulator;
import tregression.model.PairList;
import tregression.preference.TregressionPreference;
import tregression.separatesnapshots.DiffMatcher;
import tregression.views.BuggyTraceView;
import tregression.views.CorrectTraceView;
import tregression.views.StepDetailIOUI;

public class ProfInferTHandler 
//extends ProbInferHandler 
{
	
	protected TraceView correctView = null;
	
//	@Override
//	protected void setup() {
//		Display.getDefault().syncExec(new Runnable() {
//			@Override
//			public void run() {
//				try {
//					buggyView = (BuggyTraceView)PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(BuggyTraceView.ID);
//					correctView = (CorrectTraceView)PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(CorrectTraceView.ID);
//				} catch (PartInitException e) {
//					buggyView = null;
//					correctView = null;
//					System.out.println("Fail to get the view");
//				}
//			}
//		});
//	}
}
