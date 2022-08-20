package tregression.handler;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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
import microbat.Activator;
import microbat.baseline.encoders.NodeFeedbackPair;
import microbat.baseline.encoders.ProbabilityEncoder;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.recommendation.ChosenVariableOption;
import microbat.recommendation.UserFeedback;
import microbat.util.JavaUtil;
import tracediff.TraceDiff;
import tracediff.model.PairList;
import tracediff.model.TraceNodePair;
import tregression.StepChangeType;
import tregression.StepChangeTypeChecker;
import tregression.empiricalstudy.RootCauseFinder;
import tregression.empiricalstudy.Simulator;
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
public class MutationBaselineHandler extends AbstractHandler {
	
	private BuggyTraceView buggyView;
	private CorrectTraceView correctView;
	
	private final double maxItrFactor = 0.75;
	
	private final int maxMutationLimit = 10;
	
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

				// Set up the recorder
				
				Recorder recorder = new Recorder();
				
				List<Integer> ignoreIdxes = new ArrayList<>();
				ignoreIdxes.add(13);
				
				String testCaseID_str = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.BUG_ID);
				final int testCaesID = Integer.parseInt(testCaseID_str);
				
				
				for (int testCaseIdx = testCaesID; testCaseIdx<testCaesID+10; testCaseIdx++) {
					System.out.println("Working on it");
					String errorMsg = null;
					if (ignoreIdxes.contains(testCaseIdx)) {
						errorMsg = "test case ignored";
						recorder.exportCSV(testCaseIdx, errorMsg);

						continue;
					}
						
					TestCase testCase = mutationFramework.getTestCases().get(testCaseIdx);
					System.out.println("--------------- " + testCaseIdx + " test case");
					mutationFramework.setTestCase(testCase);
					mutationFramework.setMaxNumberOfMutations(1);
					
					MutationResult result = null;
					try {
						boolean testCaseFailed = false;
						for (int count=0; count<maxMutationLimit; count++) {
							mutationFramework.setSeed(1);
							result = mutationFramework.startMutationFramework();
							if (!result.mutatedTestCasePassed()) {
								testCaseFailed = true;
								break;
							}
						}
						
						if (!testCaseFailed) {
							errorMsg = "Test do not failed";
							recorder.exportCSV(testCaseIdx, errorMsg);
							throw new RuntimeException(errorMsg);
						}
					} catch (RuntimeException e) {
						System.out.println("Skipping " + testCaseIdx + " because startMutationFramework throw runtime error");
						e.printStackTrace();
						errorMsg = e.getMessage();
						recorder.exportCSV(testCaseIdx, errorMsg);
						continue;
					}
						
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
					
					/*
					 * Skip the test case if the root cause / input or output variables
					 * cannot be found
					 */
					if (result.getRootCauses().isEmpty()) {
						System.out.println("Skipping test case because no root cause: " + testCaseIdx);
						errorMsg = "No root cause";
						recorder.exportCSV(testCaseIdx, errorMsg);
						continue;
					}
					
					if (result.getTestIOs().isEmpty()) {
						System.out.println("Skipping test case because no IO: " + testCaseIdx);
						errorMsg = "No IO";
						recorder.exportCSV(testCaseIdx, errorMsg);
						continue;
					}
					
					// Get the ground truth root cause
					List<TraceNode> rootCauses = result.getRootCauses();
					TraceNode rootCause = rootCauses.get(rootCauses.size()-1);
					
					// Start debugging 
					final int maxItr = Math.min( (int) (buggyTrace.size() * maxItrFactor), 20);
					int noOfFeedbacks = 0;
					
					ProbabilityEncoder encoder = new ProbabilityEncoder(buggyTrace);
					
					// Set up input and output variables
					List<VarValue> inputs = result.getTestIOs().get(result.getTestIOs().size()-1).getInputs();
					VarValue output = result.getTestIOs().get(result.getTestIOs().size()-1).getOutput();
					
					List<VarValue> outputs = new ArrayList<>();
					outputs.add(output);
					
//					for (VarValue inputVar : inputs) {
//						System.out.println("Input: " + inputVar.getVarID());
//					}
//					
//					for (VarValue outputVar : outputs) {
//						System.out.println("Output: " + outputVar.getVarID());
//					}
					
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
					
					Simulator simulator = new Simulator(false, false, 3);
					simulator.prepare(buggyTrace, correctTrace, pairListTregression, matcher);
					finder.checkRootCause(simulator.getObservedFault(), buggyTrace, correctTrace, pairListTregression, matcher);
					
					
					boolean rootCauseFound = false;
					while (noOfFeedbacks <= maxItr) {
//						System.out.println("---------------------------------- " + noOfFeedbacks + " iteration");
						
						// Make sure that the python server is available before calling this function
						encoder.encode();
						
						// Predicted root cause
						TraceNode prediction = encoder.getMostErroneousNode();
						
						// Visualize the prediction
						jumpToNode(prediction);
						
						System.out.println("Ground Truth: " + rootCause.getOrder() + ", Prediction: " + prediction.getOrder());
						if (prediction.getOrder() == rootCause.getOrder()) {
							// Baseline have found the root cause !
							rootCauseFound = true;
							break;
						}
						
						// If baseline cannot find the root cause, we need to find a node to ask for feedback
						TraceNode nextInspectingNode = prediction;
						int nextOrder = startPointer;
						if (visitedNodeOrder.contains(nextInspectingNode.getOrder())) {
							while (visitedNodeOrder.contains(nextOrder)) {
								startPointer++;
								nextOrder = encoder.getSlicedExecutionList().get(startPointer).getOrder();
							}
							nextInspectingNode = buggyTrace.getTraceNode(nextOrder);
						}
//						System.out.println("Asking feedback for node: " + nextInspectingNode.getOrder());
						
						// Collect feedback from correct trace
						StepChangeType type = typeChecker.getType(nextInspectingNode, true, buggyView.getPairList(), buggyView.getDiffMatcher());
						UserFeedback feedback = typeToFeedback(type, nextInspectingNode, true, finder);
//						System.out.println("Feedback for node: " + nextInspectingNode.getOrder() + " is " + feedback);
						
						// Add feedback information into probability encoder
						NodeFeedbackPair pair = new NodeFeedbackPair(nextInspectingNode, feedback);
						ProbabilityEncoder.addFeedback(pair);
						
						noOfFeedbacks += 1;
						visitedNodeOrder.add(nextInspectingNode.getOrder());
					}
					
					// Record the result into text file
					recorder.exportCSV(testCaseIdx, testCase.signature, buggyTrace.getExecutionList().size(), noOfFeedbacks, rootCauseFound);
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
	
	private class Recorder {
		
		public static final String FILENAME = "baseline_record.txt";
		
		public static final String path_str = "C:\\Users\\arkwa\\Documents\\NUS\\Dissertation\\Baseline_Performance\\" + Recorder.FILENAME;
		
		public static final String DILIMITER = ",";
		
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
