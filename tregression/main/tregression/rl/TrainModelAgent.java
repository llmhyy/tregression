package tregression.rl;

import java.util.HashSet;
import java.util.List;
import java.util.Stack;

import debuginfo.NodeFeedbacksPair;
import microbat.debugpilot.DebugPilot;
import microbat.debugpilot.DebugPilotTrainer;
import microbat.debugpilot.pathfinding.FeedbackPath;
import microbat.debugpilot.pathfinding.PathFinderType;
import microbat.debugpilot.propagation.PropagatorType;
import microbat.debugpilot.rootcausefinder.RootCauseLocatorType;
import microbat.debugpilot.settings.DebugPilotSettings;
import microbat.debugpilot.settings.PathFinderSettings;
import microbat.debugpilot.settings.PropagatorSettings;
import microbat.debugpilot.settings.RootCauseLocatorSettings;
import microbat.log.Log;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
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
		
		DebugPilotSettings settings = new DebugPilotSettings();
		settings.setTrace(this.buggyTrace);
		settings.setCorrectVars(new HashSet<>(this.inputs));
		settings.setWrongVars(new HashSet<>(this.outputs));
		settings.setOutputNode(outputNode);
		
		PropagatorSettings propagatorSettings = new PropagatorSettings();
		propagatorSettings.setPropagatorType(PropagatorType.SPP_RL_TRAIN);
		settings.setPropagatorSettings(propagatorSettings);
		
		PathFinderSettings pathFinderSettings = new PathFinderSettings();
		pathFinderSettings.setPathFinderType(PathFinderType.Dijkstra);
		settings.setPathFinderSettings(pathFinderSettings);
		
		RootCauseLocatorSettings rootCauseLocatorSettings = new RootCauseLocatorSettings();
		rootCauseLocatorSettings.setRootCauseLocatorType(RootCauseLocatorType.SPP);
		settings.setRootCauseLocatorSettings(rootCauseLocatorSettings);
		
		DebugPilotTrainer debugPilotTrainer = new DebugPilotTrainer(settings);
		Stack<NodeFeedbacksPair> userFeedbackRecords = new Stack<>();
		final TraceNode rootCause = result.isOmissionBug ? null : this.buggyTrace.getTraceNode((int)result.rootCauseOrder);
		Log.printMsg(this.getClass(), "Start automatic debugging: " + result.projectName + ":" + result.bugID);
		
		TraceNode currentNode = this.outputNode;
		boolean isEnd = false;
		
		while (!isEnd) {
			debugPilotTrainer.updateFeedbacks(userFeedbackRecords);
			debugPilotTrainer.multiSlicing();
			debugPilotTrainer.propagate();
			TraceNode proposedRootCause = debugPilotTrainer.locateRootCause();
			FeedbackPath feedbackPath = debugPilotTrainer.constructPath(proposedRootCause);
			
			RewardCalculator rewardCalculator = new RewardCalculator(this.buggyTrace, this.feedbackAgent, rootCause, this.outputNode);
			debugPilotTrainer.sendRewards(rewardCalculator.getReward(proposedRootCause, feedbackPath, currentNode));
			
			boolean needPropagateAgain = false;
			while (!needPropagateAgain && !isEnd) {
				UserFeedback predictedFeedback = feedbackPath.getFeedback(currentNode).getFirstFeedback();
				Log.printMsg(this.getClass(), "--------------------------------------");
				Log.printMsg(this.getClass(), "Predicted feedback of node: " + currentNode.getOrder() + ": " + predictedFeedback.toString());
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
					Log.printMsg(this.getClass(), "You give CORRECT feedback at node: " + currentNode.getOrder());
					NodeFeedbacksPair prevRecord = userFeedbackRecords.peek();
					TraceNode prevNode = prevRecord.getNode();
					Log.printMsg(this.getClass(), "Please confirm the feedback at previous node.");
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
							Log.printMsg(this.getClass(), "Please confirm the feedback at previous node.");
							correctingFeedbacks = this.giveFeedback(prevNode);
							if (correctingFeedbacks.equals(prevRecord)) {
								lastAccurateFeedbackLocated = true;
								currentNode = TraceUtil.findNextNode(prevNode, correctingFeedbacks.getFeedbacks().get(0), buggyTrace);
								Log.printMsg(this.getClass(), "Last accurate feedback located. Please start giveing feedback from node: " + currentNode.getOrder());
								continue;
							}
							userFeedbackRecords.pop();
							if (userFeedbackRecords.isEmpty()) {
								// Reach initial feedback
								Log.printMsg(this.getClass(), "You are going to reach the initialize feedback which assumed to be accurate");
								Log.printMsg(this.getClass(), "Pleas start giving from node: "+prevNode.getOrder());
								Log.printMsg(this.getClass(), "If the initial feedback is inaccurate, please start the whole process again");
								currentNode = prevNode;
								lastAccurateFeedbackLocated = true;
							}
						}
					}
				} else if (TraceUtil.findNextNode(currentNode, userFeedbacks.getFirstFeedback(), buggyTrace) == null) {
					Log.printMsg(this.getClass(), "Cannot find next node. Please double check you feedback at node: " + currentNode.getOrder());
					NodeFeedbacksPair correctingFeedbacks = this.giveFeedback(currentNode);
					if (correctingFeedbacks.equals(userFeedbacks)) {
						// Omission bug confirmed
						isEnd = true;
					} else {
						Log.printMsg(this.getClass(), "Wong prediction on feedback, start propagation again");
						needPropagateAgain = true;
//						this.userFeedbackRecords.add(correctingFeedbacks);
						currentNode = TraceUtil.findNextNode(currentNode, correctingFeedbacks.getFirstFeedback(), buggyTrace);
					}
				} else {
					Log.printMsg(this.getClass(), "Wong prediction on feedback, start propagation again");
					needPropagateAgain = true;
					userFeedbackRecords.add(userFeedbacks);
					currentNode = TraceUtil.findNextNode(currentNode, userFeedbacks.getFirstFeedback(), buggyTrace);
				}
			}
		}
		return result;
	}
	
	private NodeFeedbacksPair giveFeedback(final TraceNode node) {
		UserFeedback feedback = this.feedbackAgent.giveFeedback(node);
		NodeFeedbacksPair feedbackPair = new NodeFeedbacksPair(node, feedback);
		return feedbackPair;
	}
	
}
