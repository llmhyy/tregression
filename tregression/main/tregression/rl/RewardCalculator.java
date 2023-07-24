package tregression.rl;

import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.recommendation.UserFeedback;
import microbat.util.TraceUtil;
import sav.common.core.Pair;
import tregression.auto.AutoFeedbackAgent;
import debuginfo.NodeFeedbacksPair;
import java.util.List;
import java.util.ArrayList;

import microbat.debugpilot.pathfinding.FeedbackPath;
import microbat.debugpilot.pathfinding.FeedbackPathUtil;
import microbat.log.Log;

public class RewardCalculator {
	
	protected final Trace trace;
	protected final AutoFeedbackAgent agent;
	protected final TraceNode gtRootCause;
	protected final TraceNode startNode;
	protected final FeedbackPath gtPath;
	
	public RewardCalculator(final Trace trace, final AutoFeedbackAgent agent, final TraceNode gtRootCause, final TraceNode startNode) {
		this.trace = trace;
		this.agent = agent;
		this.gtRootCause = gtRootCause;
		this.startNode = startNode;
		this.gtPath = this.constructGTPath();
	}
	
	public List<Pair<TraceNode, Double>> getReward(final TraceNode proposedRootCause, final FeedbackPath proposedPath, final TraceNode currentNode) {
		
		if (!FeedbackPathUtil.samePathBeforeNode(proposedPath, this.gtPath, currentNode)) {
			throw new RuntimeException(Log.genMsg(getClass(), "Path does not match before the currentNode: " + currentNode.getOrder()));
		}
		
		double globalReward = 0;
//		globalReward += this.calDistanceReward(proposedRootCause);
		globalReward += this.calPathReward(proposedPath, this.gtPath, currentNode);
		
		List<Pair<TraceNode, Double>> rewardList = new ArrayList<>();
		final int minPathLength = Math.min(proposedPath.getLength(), this.gtPath.getLength());
		boolean startCalReward = false;
		for (int i=0; i<minPathLength; i++) {
			final NodeFeedbacksPair proposedPair = proposedPath.get(i);
			final NodeFeedbacksPair gtPair = this.gtPath.get(i);
			if (proposedPair.getNode().equals(currentNode) && gtPair.getNode().equals(currentNode)) {
				startCalReward = true;
			} else if (!proposedPair.getNode().equals(currentNode) && !gtPair.getNode().equals(currentNode)) {
				
			} else {
				throw new RuntimeException(Log.genMsg(this.getClass(), "Path does not match"));
			}
			
			if (startCalReward) {
				/*
				 * We handle three case
				 * 1. Feedback is given correctly: reward = global reward + 1.0
				 * 2. Feedback is given wrongly: reward = global reward - 1.0
				 * 3. Don't have reference: reward = global reward
				 */
				double reward = globalReward;
				if (proposedPair.equals(gtPair)) {
					reward += 1.0d;
				} else if (proposedPair.getNode().equals(gtPair.getNode()) && !proposedPair.equals(gtPair)) {
					reward -= 1.0d;
				}
				Pair<TraceNode, Double> pair = Pair.of(proposedPair.getNode(), reward);
				rewardList.add(pair);
			}
		}
		return rewardList;
	}
	
	protected double calDistanceReward(final TraceNode proposedRootCause) {
		if (this.gtRootCause== null) return 0.0f;
		final double distance = TraceUtil.relationDistance(proposedRootCause, this.gtRootCause, this.trace, 5);
		if (distance == -1.0f) return 0.0f;
		return 1 / (distance+1.0f);
	}

	protected FeedbackPath constructGTPath() {
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
		return new FeedbackPath(paths);
	}
	
	protected int distance(final TraceNode proposedRootCause, final TraceNode gtRootCause) {
		return TraceUtil.relationDistance(proposedRootCause, gtRootCause, this.trace, 5);
	}
	
	protected double calPathReward(final FeedbackPath proposedPath, final FeedbackPath gtPath, final TraceNode currentNode) {
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
		return countCorrectFeedback / (double) totalCount;
	}
}
