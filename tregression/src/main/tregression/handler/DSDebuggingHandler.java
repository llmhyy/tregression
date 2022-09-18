package tregression.handler;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
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
import baseline.MutationAgent;
import jmutation.MutationFramework;
import jmutation.model.MutationResult;
import jmutation.model.TestCase;
import microbat.Activator;
import microbat.baseline.encoders.NodeFeedbackPair;
import microbat.baseline.encoders.ProbabilityEncoder;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.recommendation.ChosenVariableOption;
import microbat.recommendation.UserFeedback;
import microbat.util.JavaUtil;
import testio.TestIOFramework;
import testio.model.IOModel;
import testio.model.TestIO;
import tracediff.TraceDiff;
import tracediff.model.TraceNodePair;
import tregression.StepChangeType;
import tregression.StepChangeTypeChecker;
import tregression.empiricalstudy.RootCauseFinder;
import tregression.empiricalstudy.Simulator;
import tregression.model.PairList;
import tregression.preference.TregressionPreference;
import tregression.separatesnapshots.DiffMatcher;
import tregression.views.BuggyTraceView;
import tregression.views.CorrectTraceView;

/**
 * MutationBaselineHandler will run the baseline debugger
 * on mutated projects. Mutated projects are provided by
 * java-mutation-framework
 * @author David
 *
 */
public class DSDebuggingHandler extends AbstractHandler {
	
	private BuggyTraceView buggyView;
	private CorrectTraceView correctView;
	
	private int slicingCount = 0;
	
	final private double maxFactor = 0.5;
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		JavaUtil.sourceFile2CUMap.clear();
		
		Job job = new Job("Testing Tregression") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				
				// Access the buggy view and correct view
				setup();
				
				// Set up parameters
				String testCaseID_str = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.BUG_ID);
				final int startID = Integer.parseInt(testCaseID_str);
	
				final String projectPath = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.REPO_PATH);
//				final String dropInDir = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.DROP_IN_FOLDER);
//				final String microbatConfigPath = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.CONFIG_PATH);
//
//				String seed_str = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.SEED);
//				final int seed = Integer.parseInt(seed_str);
				
				Recorder recorder = new Recorder();
				
				MutationAgent mutationAgent = new MutationAgent(projectPath);
				
				String message = "";
				
				for (int testCaseID = startID; testCaseID < startID + 10; testCaseID++) {
					
					try {
						
						// Perform mutation
						mutationAgent.setTestCaseID(testCaseID);
//						mutationAgent.setSeed(seed);
						mutationAgent.startMutation();
						
						// Visualize the mutation result
						updateView(mutationAgent.getBuggyTrace(), mutationAgent.getCorrectTrace(), mutationAgent.getPairList(), mutationAgent.getMatcher());
						
						// Set up trace information
						slicingCount = 0;
						final Trace buggyTrace = buggyView.getTrace();
						final Trace correctTrace = correctView.getTrace();
						final PairList pairList = buggyView.getPairList();
						final DiffMatcher matcher = buggyView.getDiffMatcher();
						
						List<TraceNode> rootCauses = mutationAgent.getRootCause();
						List<VarValue> outputs = mutationAgent.getOutputs();
						
						StepChangeTypeChecker typeChecker = new StepChangeTypeChecker(buggyTrace, correctTrace);
						
						Simulator simulator = new Simulator(false, false, 3);
						simulator.prepare(buggyTrace, correctTrace, pairList, matcher);
						RootCauseFinder finder = new RootCauseFinder();
						finder.setRootCauseBasedOnDefects4J(pairList, matcher, buggyTrace, correctTrace);
						finder.checkRootCause(simulator.getObservedFault(), buggyTrace, correctTrace, pairList, matcher);
						
						boolean rootCauseFound = false;
						Set<TraceNode> toVisitNodes = extractOutputNode(buggyTrace, outputs);
						
						for (TraceNode node : toVisitNodes) {
							
							if (rootCauseFound) {
								break;
							}
							
							rootCauseFound = true;
							TraceNode nextNode = node;
							jumpToNode(nextNode);
							
							// Keep slicing until root cause is found
							while (!rootCauses.contains(nextNode) && slicingCount <= buggyTrace.size() * maxFactor) {
								StepChangeType type = typeChecker.getType(nextNode, true, buggyView.getPairList(), buggyView.getDiffMatcher());
								UserFeedback feedback = typeToFeedback(type, nextNode, true, finder);
								System.out.println("Current Node: " + nextNode.getOrder() + " with feedback: " + feedback);
								if (feedback.getFeedbackType().equals(UserFeedback.WRONG_PATH)) {
									nextNode = nextNode.getControlDominator();
									slicingCount += 1;
								} else if (feedback.getFeedbackType().equals(UserFeedback.WRONG_VARIABLE_VALUE)) {
									VarValue var = feedback.getOption().getReadVar();
									nextNode = buggyTrace.findDataDependency(nextNode, var);
									slicingCount += 1;
								} else {
									rootCauseFound = false;
									break;
								}
							}
						}
						
						recorder.exportCSV(testCaseID, mutationAgent.getTestCase().simpleName, buggyTrace.size(), slicingCount, rootCauseFound);
						printReport(testCaseID, mutationAgent.getTestCase().simpleName);
					
					} catch (Exception e) {
						message =e.toString();
						recorder.exportCSV(testCaseID, message);
						if (message.equals("java.lang.RuntimeException: Failed to clone project to C:\\Users\\arkwa\\AppData\\Local\\Temp\\mutation")) {
							testCaseID -= 1;
						}
					}
				}
				
				return Status.OK_STATUS;
			}
		};
		
		job.schedule();
		return null;
	}
	
	private void printReport(final int testCaseID, final String testCaseMethod) {
		System.out.println("-----------------------------------");
		System.out.println("Test Case: " + testCaseID + "," + testCaseMethod);
		System.out.println("Feedback Count: " + this.slicingCount);
		System.out.println("-----------------------------------");
	}

	/**
	 * Extract the node that contain the output variable
	 * 
	 * If the output variable is provided, it will search
	 * the execution trace backward to find the node
	 * 
	 * If the output variable is not provided, it will search
	 * the execution trace backward to find the first node
	 * that contain a written variable
	 * 
	 * @param trace Trace
	 * @param outputs List of output variables
	 * @return List of output nodes
	 */
	private Set<TraceNode> extractOutputNode(final Trace trace, List<VarValue> outputs) {
		Set<TraceNode> outputNodes = new HashSet<>();
		
		List<VarValue> outputsCopy = new ArrayList<>();
		outputsCopy.addAll(outputs);
		for (int order = trace.getLatestNode().getOrder(); order>=1; order--) {
			TraceNode node = trace.getTraceNode(order);

			Iterator<VarValue> iter = outputsCopy.iterator();
			while(iter.hasNext()) {
				VarValue output = iter.next();
				if (node.isReadVariablesContains(output.getVarID()) || node.isWrittenVariablesContains(output.getVarID())) {
					outputNodes.add(node);
					iter.remove();
				}
			}
			
			// Early stopping: when all output has been found
			if (outputsCopy.isEmpty()) {
				break;
			}
		}
		
		return outputNodes;
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
				try {
					Thread.sleep(50);
				} catch (Exception e) {
					e.printStackTrace();
				}
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
	
//	/**
//	 * DSDebuggingHandler is ready when the root causes is given
//	 * @return True if the DSDebuggingHandler is ready
//	 */
//	private boolean isReady() {
//		if (DSDebuggingHandler.rootCauses == null) {
//			return false;
//		} else {
//			if (DSDebuggingHandler.rootCauses.isEmpty()) {
//				return false;
//			}
//		}
//		
//		return true;
//	}
	
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
	
	
//	public static boolean isOutputProvided() {
//		if (DSDebuggingHandler.outputs == null) {
//			return false;
//		}
//		
//		if (DSDebuggingHandler.outputs.isEmpty()) {
//			return false;
//		}
//		
//		return true;
//	}
	
//	public static void setRootCauses(List<TraceNode> rootCauses) {
//		DSDebuggingHandler.rootCauses = rootCauses;
//	}
//	
//	public static void setRootCause(TraceNode rootCause) {
//		List<TraceNode> rootCauses = new ArrayList<>();
//		rootCauses.add(rootCause);
//		DSDebuggingHandler.setRootCauses(rootCauses);
//	}
//	
//	public static void setOutputs(List<VarValue> outputs) {
//		DSDebuggingHandler.outputs = outputs;
//	}
//	
//	public static void setOutput(VarValue output) {
//		List<VarValue> outputs = new ArrayList<>();
//		outputs.add(output);
//		DSDebuggingHandler.setOutputs(outputs);
//	}
//	
//	public static void setTestCaseID(int testCaseID) {
//		DSDebuggingHandler.testCaseID = testCaseID;
//	}
//	
//	public static void setTestCaseMethod(String testCaseMethod) {
//		DSDebuggingHandler.testCaseMethod = testCaseMethod;
//	}
	
	private class Recorder {
		
		public static final String FILENAME = "dynamic_slicing.txt";
		
		public static final String path_str = "C:\\Users\\arkwa\\Documents\\NUS\\Dissertation\\Performance\\" + Recorder.FILENAME;
		
		public static final String DILIMITER = ",";
		
		/**
		 * Write performance to text file
		 * @param testCaseID Test Case ID
		 * @param testCaseName Test Case Method Name
		 * @param traceLen Trace Length
		 * @param noOfFeedbackNeeded Number of Feedback Needed
		 * @param rootCauseFound True if the root cause is found
		 */
		public void exportCSV(final int testCaseID, final String testCaseName, final int traceLen, final int noOfFeedbackNeeded, final boolean rootCauseFound) {
			Path path = Paths.get(Recorder.path_str);
			this.exportCSV(testCaseID, path, testCaseName, traceLen, noOfFeedbackNeeded, rootCauseFound, null);
		}
		
		public void exportCSV(final int testCaseID, final String message) {
			Path path = Paths.get(Recorder.path_str);
			this.exportCSV(testCaseID, path, " ", -1, -1, false, message);
		}
		
		public void exportCSV(final int testCaseID, final Path path, final String testCaseName, final int traceLen, final int noOfFeedbackNeeded, final boolean rootCauseFound, final String message) {
			try {
				File file = path.toFile();
				
				// Create a new file if the file don't exist
				file.createNewFile();
				
				// Create file write in append mode
				FileWriter writer = new FileWriter(file, true);
				
				StringBuilder strBuilder = new StringBuilder();
				
				strBuilder.append(testCaseID);
				strBuilder.append(Recorder.DILIMITER);
				
				strBuilder.append(testCaseName);
				strBuilder.append(Recorder.DILIMITER);
				
				strBuilder.append(traceLen);
				strBuilder.append(Recorder.DILIMITER);
				
				strBuilder.append(noOfFeedbackNeeded);
				strBuilder.append(Recorder.DILIMITER);
				
				strBuilder.append(rootCauseFound);
				strBuilder.append(Recorder.DILIMITER);
				
				if (message != null) {
					strBuilder.append(message);
					strBuilder.append(Recorder.DILIMITER);
				}
				
				strBuilder.append("\n");
				
				writer.write(strBuilder.toString());
				writer.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
