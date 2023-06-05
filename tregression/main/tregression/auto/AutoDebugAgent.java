package tregression.auto;

import java.util.List;
import java.util.ArrayList;
import java.util.Stack;
import java.util.stream.Collectors;

import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.probability.SPP.SPP;
import microbat.recommendation.UserFeedback;
import microbat.util.TraceUtil;
import microbat.model.trace.Trace;
import tregression.auto.result.DebugResult;
import tregression.auto.result.RunResult;
import tregression.empiricalstudy.EmpiricalTrial;
import tregression.empiricalstudy.RootCauseFinder;
import tregression.model.PairList;
import tregression.separatesnapshots.DiffMatcher;
import debuginfo.NodeFeedbacksPair;

public class AutoDebugAgent {
	
	private final Trace buggyTrace;
	private final AutoFeedbackAgent feedbackAgent;
	private final List<VarValue> inputs;
	private final List<VarValue> outputs;
	private final TraceNode outputNode;
	
	public AutoDebugAgent(final EmpiricalTrial trial, List<VarValue> inputs, List<VarValue> outputs, TraceNode outputNode) {
		this.buggyTrace = trial.getBuggyTrace();
		this.feedbackAgent = new AutoFeedbackAgent(trial);
		this.inputs = inputs;
		this.outputs = outputs;
		this.outputNode = outputNode;
	}
	
	public AutoDebugAgent(final Trace buggyTrace, final Trace correctTrace, 
			final DiffMatcher matcher, final PairList pairList, final RootCauseFinder finder,
			List<VarValue> inputs, List<VarValue> outputs, TraceNode outputNode) {
		this.buggyTrace = buggyTrace;
		this.feedbackAgent = new AutoFeedbackAgent(buggyTrace, correctTrace, pairList, matcher, finder);
		this.inputs = inputs;
		this.outputs = outputs;
		this.outputNode = outputNode;
	}
	
	public DebugResult startDebug(final RunResult result) {
		SPP spp = new SPP(this.buggyTrace, inputs, outputs, outputNode);
		DebugResult debugResult = new DebugResult(result);
		
		final TraceNode rootCause = result.isOmissionBug ? null : this.buggyTrace.getTraceNode((int)result.rootCauseOrder);

		
		Stack<NodeFeedbacksPair> userFeedbackRecords = new Stack<>();
		
		AutoDebugAgent.printMsg("Start automatic debugging: " + result.projectName + ":" + result.bugID);
		
		TraceNode currentNode = this.outputNode;
		boolean isEnd = false;
		
		// Measurement
		int totalFeedbackCount = 0;
		int correctFeedbackCount = 0;
		List<Double> propTimes = new ArrayList<>();
		List<Double> pathFindingTimes = new ArrayList<>();
		List<Double> totalTimes = new ArrayList<>();
		boolean debugSuccess = false;
		boolean locateRootCause = false;
		while (!isEnd) {
			spp.updateFeedbacks(userFeedbackRecords);
			
			SPP.printMsg("Propagating probability ...");
			long propStartTime = System.currentTimeMillis();
			spp.propagate();
			long propEndTime = System.currentTimeMillis();
			
			SPP.printMsg("Locating root cause ...");
			spp.locateRootCause();
			
			long pathStartTime = System.currentTimeMillis();
			SPP.printMsg("Constructing path to root cause ...");
			spp.constructPath();
			long pathEndTime = System.currentTimeMillis();
			
			double propTime = (propEndTime - propStartTime) / (double) 1000;
			propTimes.add(propTime);
			
			double pathFindingTime = (pathEndTime - pathStartTime) / (double) 1000;
			pathFindingTimes.add(pathFindingTime);
			
			totalTimes.add(propTime + pathFindingTime);
			
			boolean needPropagateAgain = false;
			while (!needPropagateAgain && !isEnd) {
				UserFeedback predictedFeedback = spp.giveFeedback(currentNode);
				SPP.printMsg("--------------------------------------");
				SPP.printMsg("Predicted feedback of node: " + currentNode.getOrder() + ": " + predictedFeedback.toString());
				NodeFeedbacksPair userFeedbacks = this.giveFeedback(currentNode);
				AutoDebugAgent.printMsg("Ground truth feedback: " + userFeedbacks);
				totalFeedbackCount += 1;
				
				// Reach the root cause
				if (currentNode.equals(rootCause)) {
					if (predictedFeedback.getFeedbackType().equals(UserFeedback.ROOTCAUSE)) {
						correctFeedbackCount+=1;
						locateRootCause = true;
					}
					debugSuccess = true;
					isEnd = true;
					break;
				}
				if (userFeedbacks.containsFeedback(predictedFeedback)) {
					// Feedback predicted correctly, save the feedback into record and move to next node
					userFeedbackRecords.add(userFeedbacks);
					currentNode = TraceUtil.findNextNode(currentNode, predictedFeedback, buggyTrace);
					correctFeedbackCount+=1;
				} else if (userFeedbacks.getFeedbackType().equals(UserFeedback.CORRECT)) {
					/*	If the feedback is CORRECT, there are two reasons:
					 *  1. User give wrong feedback
					 *  2. Omission bug occur
					 *  
					 *  We first assume that user give a inaccurate feedback last iteration
					 *  and ask user to correct it. Since user may give multiple inaccurate
					 *  feedbacks, so that we will keep asking until the last accurate feedback
					 *  is located or we end up at the initial step.
					 *  
					 *  If user insist the previous feedback is accurate, then we say there is 
					 *  omission bug
					 */
					SPP.printMsg("You give CORRECT feedback at node: " + currentNode.getOrder());
					NodeFeedbacksPair prevRecord = userFeedbackRecords.peek();
					TraceNode prevNode = prevRecord.getNode();
					SPP.printMsg("Please confirm the feedback at previous node.");
					NodeFeedbacksPair correctingFeedbacks = this.giveFeedback(prevNode);
					if (correctingFeedbacks.equals(prevRecord)) {
						// Omission bug confirmed
						this.reportOmissionBug(currentNode, correctingFeedbacks);
						if (correctingFeedbacks.getFeedbackType().equals(UserFeedback.WRONG_PATH) && this.isControlOmission(result)) {
							correctFeedbackCount+=1;
						} else if (correctingFeedbacks.getFeedbackType().equals(UserFeedback.WRONG_VARIABLE_VALUE) && this.isDataOmission(debugResult)) {
							correctFeedbackCount+=1;
						}
						isEnd = true;
					}  else {
						// Handling wrong feedback
						boolean lastAccurateFeedbackLocated = false;
						userFeedbackRecords.pop();
						while (!lastAccurateFeedbackLocated && !isEnd) {
							prevRecord = userFeedbackRecords.peek();
							prevNode = prevRecord.getNode();
							SPP.printMsg("Please confirm the feedback at previous node.");
							correctingFeedbacks = this.giveFeedback(prevNode);
							if (correctingFeedbacks.equals(prevRecord)) {
								lastAccurateFeedbackLocated = true;
								currentNode = TraceUtil.findNextNode(prevNode, correctingFeedbacks.getFeedbacks().get(0), buggyTrace);
								SPP.printMsg("Last accurate feedback located. Please start giveing feedback from node: " + currentNode.getOrder());
								continue;
							}
							userFeedbackRecords.pop();
							if (userFeedbackRecords.isEmpty()) {
								// Reach initial feedback
								SPP.printMsg("You are going to reach the initialize feedback which assumed to be accurate");
								SPP.printMsg("Pleas start giving from node: "+prevNode.getOrder());
								SPP.printMsg("If the initial feedback is inaccurate, please start the whole process again");
								currentNode = prevNode;
								lastAccurateFeedbackLocated = true;
							}
						}
					}
				} else if (TraceUtil.findNextNode(currentNode, userFeedbacks.getFirstFeedback(), buggyTrace) == null) {
					/* Next node is null. Possible reasons:
					 * 1. Wrong feedback is given
					 * 2. Omission bug occur
					 * 
					 * First assume a wrong feedback is given and ask user
					 * to correct it. After correction, if the feedback
					 * match with predicted feedback, then continue the process
					 * as if nothing happen. If the feedback mismatch, then
					 * handle it the same as wrong prediction.
					 * 
					 * If the user insist the feedback is accurate, then
					 * omission bug confirm
					 */
					SPP.printMsg("Cannot find next node. Please double check you feedback at node: " + currentNode.getOrder());
					NodeFeedbacksPair correctingFeedbacks = this.giveFeedback(currentNode);
					if (correctingFeedbacks.equals(userFeedbacks)) {
						// Omission bug confirmed
						final TraceNode startNode = currentNode.getInvocationParent() == null ? buggyTrace.getTraceNode(1) : currentNode.getInvocationParent();
						this.reportOmissionBug(startNode, correctingFeedbacks);
						if (correctingFeedbacks.getFeedbackType().equals(UserFeedback.WRONG_PATH) && this.isControlOmission(result)) {
							correctFeedbackCount+=1;
						} else if (correctingFeedbacks.getFeedbackType().equals(UserFeedback.WRONG_VARIABLE_VALUE) && this.isDataOmission(debugResult)) {
							correctFeedbackCount+=1;
						}
						isEnd = true;
					} else {
						SPP.printMsg("Wong prediction on feedback, start propagation again");
						needPropagateAgain = true;
//						this.userFeedbackRecords.add(correctingFeedbacks);
						currentNode = TraceUtil.findNextNode(currentNode, correctingFeedbacks.getFirstFeedback(), buggyTrace);
					}
				} else {
					SPP.printMsg("Wong prediction on feedback, start propagation again");
					needPropagateAgain = true;
					userFeedbackRecords.add(userFeedbacks);
					currentNode = TraceUtil.findNextNode(currentNode, userFeedbacks.getFirstFeedback(), buggyTrace);
				
				}
			}
		}
		
		final double avgPropTime = propTimes.stream().collect(Collectors.averagingDouble(Double::doubleValue));
		final double avgPathFindingTime = pathFindingTimes.stream().collect(Collectors.averagingDouble(Double::doubleValue));
		final double avgTime = totalTimes.stream().collect(Collectors.averagingDouble(Double::doubleValue));
		
		debugResult.avgPropTime = avgPropTime;
		debugResult.avgPathFindingTime = avgPathFindingTime;
		debugResult.avgTotalTime = avgTime;
		debugResult.correctFeedbackCount = correctFeedbackCount;
		debugResult.totalFeedbackCount = totalFeedbackCount;
		debugResult.debugSuccess = debugSuccess;
		return debugResult;
	}
	
	private NodeFeedbacksPair giveFeedback(final TraceNode node) {
		UserFeedback feedback = this.feedbackAgent.giveFeedback(node);
		NodeFeedbacksPair feedbackPair = new NodeFeedbacksPair(node, feedback);
		return feedbackPair;
	}
	
	public static String genMsg(final String message) {
		return "[AutoDebugAgent] " + message;
	}
	
	public static void printMsg(final String message) {
		System.out.println(AutoDebugAgent.genMsg(message));
	}
	
	public boolean isControlOmission(final RunResult result) {
		final String solutationName = result.solutionName;
		return solutationName.equals("missing assignment") ||
			   solutationName.equals("incorrect condition") ||
			   solutationName.equals("miss-evaluated condition") ||
			   solutationName.equals("invoke new method");
	}
	
	public boolean isDataOmission(final RunResult result) {
		final String solutationName = result.solutionName;
		return solutationName.equals("extra nested if block") ||
			   solutationName.equals("missing if block") ||
			   solutationName.equals("missing if return") ||
			   solutationName.equals("missing if throw") ||
			   solutationName.equals("invoke different method");
	}
	
	protected void reportOmissionBug(final TraceNode startNode, final NodeFeedbacksPair feedback) {
		if (feedback.getFeedbackType().equals(UserFeedback.WRONG_PATH)) {
			this.reportMissingBranchOmissionBug(startNode, feedback.getNode());
		} else if (feedback.getFeedbackType().equals(UserFeedback.WRONG_VARIABLE_VALUE)) {
			VarValue varValue = feedback.getFeedbacks().get(0).getOption().getReadVar();
			this.reportMissingAssignmentOmissionBug(startNode, feedback.getNode(), varValue);
		}
	}
	protected void reportMissingBranchOmissionBug(final TraceNode startNode, final TraceNode endNode) {
		SPP.printMsg("-------------------------------------------");
		SPP.printMsg("Omission bug detected");
		SPP.printMsg("Scope begin: " + startNode.getOrder());
		SPP.printMsg("Scope end: " + endNode.getOrder());
		SPP.printMsg("Omission Type: Missing Branch");
		SPP.printMsg("-------------------------------------------");
	}
	
	protected void reportMissingAssignmentOmissionBug(final TraceNode startNode, final TraceNode endNode, final VarValue var) {
		SPP.printMsg("-------------------------------------------");
		SPP.printMsg("Omission bug detected");
		SPP.printMsg("Scope begin: " + startNode.getOrder());
		SPP.printMsg("Scope end: " + endNode.getOrder());
		SPP.printMsg("Omission Type: Missing Assignment of " + var.getVarName());
		SPP.printMsg("-------------------------------------------");
	}
}
