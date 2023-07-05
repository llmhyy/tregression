package tregression.rl;

import java.util.List;
import java.util.Stack;

import debuginfo.NodeFeedbacksPair;
import microbat.log.Log;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.probability.SPP.DebugPilot;
import microbat.probability.SPP.propagation.PropagatorType;
import microbat.recommendation.UserFeedback;
import microbat.util.TraceUtil;
import tregression.auto.AutoDebugAgent;
import tregression.auto.AutoFeedbackAgent;
import tregression.auto.result.RunResult;
import tregression.empiricalstudy.EmpiricalTrial;

public class TrainModelAgent {
	
	private final Trace buggyTrace;
	private final AutoFeedbackAgent feedbackAgent;
	private final List<VarValue> inputs;
	private final List<VarValue> outputs;
	private final TraceNode outputNode;
	
	public TrainModelAgent(final EmpiricalTrial trial, List<VarValue> inputs, List<VarValue> outputs, TraceNode outputNode) {
		this.buggyTrace = trial.getBuggyTrace();
		this.feedbackAgent = new AutoFeedbackAgent(trial);
		this.inputs = inputs;
		this.outputs = outputs;
		this.outputNode = outputNode;
	}
	
	public RunResult startTraining(final RunResult result) {
		DebugPilot debugPilot = new DebugPilot(this.buggyTrace, inputs, outputs, outputNode, PropagatorType.RL);
		Stack<NodeFeedbacksPair> userFeedbackRecords = new Stack<>();
		final TraceNode rootCause = result.isOmissionBug ? null : this.buggyTrace.getTraceNode((int)result.rootCauseOrder);
		TrainModelAgent.printMsg("Start automatic debugging: " + result.projectName + ":" + result.bugID);
		
		TraceNode currentNode = this.outputNode;
		boolean isEnd = false;
		
		while (!isEnd) {
			debugPilot.updateFeedbacks(userFeedbackRecords);
			debugPilot.propagate();
			debugPilot.locateRootCause(currentNode);
			debugPilot.constructPath();
			
			RewardCalculator rewardCalculator = new RewardCalculator(this.buggyTrace, this.feedbackAgent, rootCause, this.outputNode);
			float reward = rewardCalculator.getReward(debugPilot.getRootCause(), debugPilot.getPath(), currentNode);
			debugPilot.sendReward(reward);
			
			boolean needPropagateAgain = false;
			while (!needPropagateAgain && !isEnd) {
				UserFeedback predictedFeedback = debugPilot.giveFeedback(currentNode);
				DebugPilot.printMsg("--------------------------------------");
				DebugPilot.printMsg("Predicted feedback of node: " + currentNode.getOrder() + ": " + predictedFeedback.toString());
				NodeFeedbacksPair userFeedbacks = this.giveFeedback(currentNode);
				Log.printMsg(this.getClass(), "Ground truth feedback: " + userFeedbacks);

				// Reach the root case
				// UserFeedback type is unclear also tell that this node is root cause
				if (currentNode.equals(rootCause) || userFeedbacks.getFeedbackType().equals(UserFeedback.UNCLEAR)) {
					isEnd = true;
					break;
				}
				
				if (userFeedbacks.containsFeedback(predictedFeedback)) {
					// Feedback predicted correctly, save the feedback into record and move to next node
					userFeedbackRecords.add(userFeedbacks);
					currentNode = TraceUtil.findNextNode(currentNode, predictedFeedback, buggyTrace);
				} else if (userFeedbacks.getFeedbackType().equals(UserFeedback.CORRECT)) {
					DebugPilot.printMsg("You give CORRECT feedback at node: " + currentNode.getOrder());
					NodeFeedbacksPair prevRecord = userFeedbackRecords.peek();
					TraceNode prevNode = prevRecord.getNode();
					DebugPilot.printMsg("Please confirm the feedback at previous node.");
					NodeFeedbacksPair correctingFeedbacks = this.giveFeedback(prevNode);
					if (correctingFeedbacks.equals(prevRecord)) {
						isEnd = true;
					}  else {
						// Handling wrong feedback
						boolean lastAccurateFeedbackLocated = false;
						userFeedbackRecords.pop();
						while (!lastAccurateFeedbackLocated && !isEnd) {
							prevRecord = userFeedbackRecords.peek();
							prevNode = prevRecord.getNode();
							DebugPilot.printMsg("Please confirm the feedback at previous node.");
							correctingFeedbacks = this.giveFeedback(prevNode);
							if (correctingFeedbacks.equals(prevRecord)) {
								lastAccurateFeedbackLocated = true;
								currentNode = TraceUtil.findNextNode(prevNode, correctingFeedbacks.getFeedbacks().get(0), buggyTrace);
								DebugPilot.printMsg("Last accurate feedback located. Please start giveing feedback from node: " + currentNode.getOrder());
								continue;
							}
							userFeedbackRecords.pop();
							if (userFeedbackRecords.isEmpty()) {
								// Reach initial feedback
								DebugPilot.printMsg("You are going to reach the initialize feedback which assumed to be accurate");
								DebugPilot.printMsg("Pleas start giving from node: "+prevNode.getOrder());
								DebugPilot.printMsg("If the initial feedback is inaccurate, please start the whole process again");
								currentNode = prevNode;
								lastAccurateFeedbackLocated = true;
							}
						}
					}
				} else if (TraceUtil.findNextNode(currentNode, userFeedbacks.getFirstFeedback(), buggyTrace) == null) {
					DebugPilot.printMsg("Cannot find next node. Please double check you feedback at node: " + currentNode.getOrder());
					NodeFeedbacksPair correctingFeedbacks = this.giveFeedback(currentNode);
					if (correctingFeedbacks.equals(userFeedbacks)) {
						// Omission bug confirmed
						isEnd = true;
					} else {
						DebugPilot.printMsg("Wong prediction on feedback, start propagation again");
						needPropagateAgain = true;
//						this.userFeedbackRecords.add(correctingFeedbacks);
						currentNode = TraceUtil.findNextNode(currentNode, correctingFeedbacks.getFirstFeedback(), buggyTrace);
					}
				} else {
					DebugPilot.printMsg("Wong prediction on feedback, start propagation again");
					needPropagateAgain = true;
					userFeedbackRecords.add(userFeedbacks);
					currentNode = TraceUtil.findNextNode(currentNode, userFeedbacks.getFirstFeedback(), buggyTrace);
				}
			}
		}
		return result;
	}
	
	public static String genMsg(final String message) {
		return "[TrainModelAgent] " + message;
	}
	
	public static void printMsg(final String message) {
		System.out.println(TrainModelAgent.genMsg(message));
	}
	
	private NodeFeedbacksPair giveFeedback(final TraceNode node) {
		UserFeedback feedback = this.feedbackAgent.giveFeedback(node);
		NodeFeedbacksPair feedbackPair = new NodeFeedbacksPair(node, feedback);
		return feedbackPair;
	}
	
}
