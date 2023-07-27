package tregression.auto;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

import debuginfo.NodeFeedbackPair;
import debuginfo.NodeFeedbacksPair;
import microbat.debugpilot.DebugPilot;
import microbat.debugpilot.pathfinding.PathFinderType;
import microbat.debugpilot.propagation.PropagatorType;
import microbat.log.Log;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.recommendation.UserFeedback;
import microbat.util.TraceUtil;
import tregression.auto.result.DebugResult;
import tregression.auto.result.RunResult;
import tregression.empiricalstudy.EmpiricalTrial;
import tregression.empiricalstudy.RootCauseFinder;
import tregression.model.PairList;
import tregression.separatesnapshots.DiffMatcher;

public class AutoProfInferAgent {
	private final Trace buggyTrace;
	private final AutoFeedbackAgent feedbackAgent;
	private final List<VarValue> inputs;
	private final List<VarValue> outputs;
	private final TraceNode outputNode;
	
	public AutoProfInferAgent(final EmpiricalTrial trial, List<VarValue> inputs, List<VarValue> outputs, TraceNode outputNode) {
		this.buggyTrace = trial.getBuggyTrace();
		this.feedbackAgent = new AutoFeedbackAgent(trial);
		this.inputs = inputs;
		this.outputs = outputs;
		this.outputNode = outputNode;
	}
	
	public AutoProfInferAgent(final Trace buggyTrace, final Trace correctTrace, 
			final DiffMatcher matcher, final PairList pairList, final RootCauseFinder finder,
			List<VarValue> inputs, List<VarValue> outputs, TraceNode outputNode) {
		this.buggyTrace = buggyTrace;
		this.feedbackAgent = new AutoFeedbackAgent(buggyTrace, correctTrace, pairList, matcher, finder);
		this.inputs = inputs;
		this.outputs = outputs;
		this.outputNode = outputNode;
	}
	
	public DebugResult startDebug(final RunResult result) {
		DebugPilot debugPilot = new DebugPilot(this.buggyTrace, inputs, outputs, outputNode, PropagatorType.ProfInfer, PathFinderType.Dijkstra);
		DebugResult debugResult = new DebugResult(result);
		
		final TraceNode rootCause = result.isOmissionBug ? null : this.buggyTrace.getTraceNode((int)result.rootCauseOrder);
		if (rootCause  == null) {
			debugResult.errorMessage = Log.genMsg(getClass(), "Root Cause is null");
			return debugResult;
		}
		Stack<NodeFeedbacksPair> userFeedbackRecords = new Stack<>();
		
		Log.printMsg(this.getClass(),  "Start automatic debugging: " + result.projectName + ":" + result.bugID);
		
		TraceNode currentNode = this.outputNode;
		boolean isEnd = false;
		
		// Measurement
		int totalFeedbackCount = 0;
		int correctFeedbackCount = 0;
		List<Double> propTimes = new ArrayList<>();
		List<Double> pathFindingTimes = new ArrayList<>();
		List<Double> totalTimes = new ArrayList<>();
		boolean debugSuccess = false;
		boolean rootCauseCorrect = false;
		while (!isEnd) {
			debugPilot.updateFeedbacks(userFeedbackRecords);			
			// Propagation
			Log.printMsg(this.getClass(), "Propagating probability ...");
			long propStartTime = System.currentTimeMillis();
			debugPilot.propagate();
			long propEndTime = System.currentTimeMillis();
			double propTime = (propEndTime - propStartTime) / (double) 1000;
			propTimes.add(propTime);
			Log.printMsg(this.getClass(), "Propagatoin time: " + propTime);
			
			// Locate root cause
			Log.printMsg(this.getClass(), "Locating root cause ...");
			debugPilot.locateRootCause();
			
			final TraceNode proposedRootCause = debugPilot.getRootCause();
			Log.printMsg(getClass(), "Proposed root cause: " + proposedRootCause.getOrder());
			if (proposedRootCause.equals(rootCause)) {
				Log.printMsg(getClass(), "Root Cause is found ...");
				break;
			} else {
				NodeFeedbacksPair feedback = this.giveFeedback(currentNode);
				Log.printMsg(getClass(), "Wrong root cause, feedback is given: " + feedback);
				userFeedbackRecords.add(feedback);
				totalFeedbackCount++;
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
		debugResult.rootCauseCorrect = rootCauseCorrect;
		return debugResult;
	}
	
	private NodeFeedbacksPair giveFeedback(final TraceNode node) {
		UserFeedback feedback = this.feedbackAgent.giveFeedback(node);
		NodeFeedbacksPair feedbackPair = new NodeFeedbacksPair(node, feedback);
		return feedbackPair;
	}
}
