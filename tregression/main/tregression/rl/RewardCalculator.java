package tregression.rl;

import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.recommendation.UserFeedback;
import microbat.util.TraceUtil;
import tregression.auto.AutoFeedbackAgent;
import debuginfo.NodeFeedbacksPair;
import java.util.List;
import java.util.ArrayList;

import microbat.debugpilot.pathfinding.ActionPath;
import microbat.log.Log;

public class RewardCalculator {
	
	protected final Trace trace;
	protected final AutoFeedbackAgent agent;
	protected final TraceNode gtRootCause;
	protected final TraceNode startNode;
	protected final ActionPath gtPath;
	
	public RewardCalculator(final Trace trace, final AutoFeedbackAgent agent, final TraceNode gtRootCause, final TraceNode startNode) {
		this.trace = trace;
		this.agent = agent;
		this.gtRootCause = gtRootCause;
		this.startNode = startNode;
		this.gtPath = this.constructGTPath();
	}
	
	public float getReward(final TraceNode proposedRootCause, final ActionPath proposedPath, final TraceNode currentNode) {
		float reward = 0;
		
		if (this.gtRootCause != null) {
			if (proposedRootCause.equals(this.gtRootCause)) {
				reward += 1.0;
			} else {
				reward += 1 / TraceUtil.relationDistance(proposedRootCause, this.gtRootCause, this.trace, 5);
			}
		}
		
		final float feedback_reward = this.countCorrectFeedback(proposedPath, this.gtPath, currentNode);
		reward += feedback_reward;
		return reward;
	}

	protected ActionPath constructGTPath() {
		List<NodeFeedbacksPair> paths = new ArrayList<>();
		TraceNode currentNode = this.startNode;
		while (!currentNode.equals(this.gtRootCause)) {
			final UserFeedback feedback = this.agent.giveFeedback(currentNode);
			final NodeFeedbacksPair pair = new NodeFeedbacksPair(currentNode, feedback);
			paths.add(pair);
			if (feedback.getFeedbackType().equals(UserFeedback.CORRECT)) {
				break;
			}
			currentNode = TraceUtil.findNextNode(currentNode, feedback, this.trace);
		}
		if (currentNode.equals(this.gtRootCause)) {
			final UserFeedback feedback = new UserFeedback(UserFeedback.ROOTCAUSE);
			final NodeFeedbacksPair pair = new NodeFeedbacksPair(this.gtRootCause, feedback);
			paths.add(pair);
		}
		return new ActionPath(paths);
	}
	
	protected int distance(final TraceNode proposedRootCause, final TraceNode gtRootCause) {
		return TraceUtil.relationDistance(proposedRootCause, gtRootCause, this.trace, 5);
	}
	
	protected float countCorrectFeedback(final ActionPath proposedPath, final ActionPath gtPath, final TraceNode currentNode) {
		int totalCount = 0;
		boolean startCounting = false;
		for (NodeFeedbacksPair pair : gtPath) {
			final TraceNode node = pair.getNode();
			if (node.equals(currentNode)) {
				startCounting = true;
			}
			if (startCounting) {
				totalCount += 1;
			}
		}
		
		int countCorrectFeedback = 0;
		startCounting = false;
		for (int idx=0; idx<proposedPath.getLength(); idx++) {
			final TraceNode node = proposedPath.get(idx).getNode();
			if (node.equals(currentNode)) {
				startCounting = true;
			}
			if (startCounting) {
				final UserFeedback proposedFeedback = proposedPath.get(idx).getFirstFeedback();
				final UserFeedback gtFeedback = gtPath.get(idx).getFirstFeedback();
				if (proposedFeedback.equals(gtFeedback)) {
					countCorrectFeedback += 1;
				} else {
					break;
				}
			}
		}
		
		Log.printMsg(this.getClass(), "Correct Feedback: " + countCorrectFeedback);
		Log.printMsg(this.getClass(), "Total Feedback: " + totalCount);
		return countCorrectFeedback / (float) totalCount;
	}
}
