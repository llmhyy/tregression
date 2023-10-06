package tregression.auto;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

import microbat.debugpilot.DebugPilot;
import microbat.debugpilot.NodeFeedbackPair;
import microbat.debugpilot.NodeFeedbacksPair;
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
import tregression.auto.result.DebugResult;
import tregression.auto.result.RunResult;
import tregression.empiricalstudy.EmpiricalTrial;
import tregression.empiricalstudy.RootCauseFinder;
import tregression.model.PairList;
import tregression.separatesnapshots.DiffMatcher;

public class AutoProfInferAgent {
	private final Trace buggyTrace;
	private final CarelessAutoFeedbackAgent feedbackAgent;
	private final List<VarValue> inputs;
	private final List<VarValue> outputs;
	private final TraceNode outputNode;
	
	protected final List<TraceNode> proposeHistory = new ArrayList<>();
	
	public AutoProfInferAgent(final EmpiricalTrial trial, List<VarValue> inputs, List<VarValue> outputs, TraceNode outputNode, final double mistakeProbability) {
		this.buggyTrace = trial.getBuggyTrace();
		this.feedbackAgent = new CarelessAutoFeedbackAgent(trial, mistakeProbability);
		this.inputs = inputs;
		this.outputs = outputs;
		this.outputNode = outputNode;
	}
	
//	public AutoProfInferAgent(final Trace buggyTrace, final Trace correctTrace, 
//			final DiffMatcher matcher, final PairList pairList, final RootCauseFinder finder,
//			List<VarValue> inputs, List<VarValue> outputs, TraceNode outputNode) {
//		this.buggyTrace = buggyTrace;
//		this.feedbackAgent = new AutoFeedbackAgent(buggyTrace, correctTrace, pairList, matcher, finder);
//		this.inputs = inputs;
//		this.outputs = outputs;
//		this.outputNode = outputNode;
//	}
	
	public DebugResult startDebug(final RunResult result) {
		
		DebugPilotSettings settings = new DebugPilotSettings();
		settings.setTrace(this.buggyTrace);
		settings.setCorrectVars(new HashSet<>(this.inputs));
		settings.setWrongVars(new HashSet<>(this.outputs));
		settings.setOutputNode(outputNode);
		
		PropagatorSettings propagatorSettings = settings.getPropagatorSettings();
		propagatorSettings.setPropagatorType(PropagatorType.ProfInfer);
		settings.setPropagatorSettings(propagatorSettings);
		
		PathFinderSettings pathFinderSettings = settings.getPathFinderSettings();
		pathFinderSettings.setPathFinderType(PathFinderType.DijkstraExp);
		settings.setPathFinderSettings(pathFinderSettings);
		
		RootCauseLocatorSettings rootCauseLocatorSettings = settings.getRootCauseLocatorSettings();
		rootCauseLocatorSettings.setRootCauseLocatorType(RootCauseLocatorType.PROBINFER);
		settings.setRootCauseLocatorSettings(rootCauseLocatorSettings);
		
		DebugPilot debugPilot = new DebugPilot(settings);
		DebugResult debugResult = new DebugResult(result);
		
		debugResult.probinfer_effort = 0.0d;
		
		final TraceNode rootCause = this.buggyTrace.getTraceNode((int) result.rootCauseOrder);
//		final TraceNode rootCause = result.isOmissionBug ? null : this.buggyTrace.getTraceNode((int)result.rootCauseOrder);
		if (rootCause  == null) {
			debugResult.errorMessage = Log.genMsg(getClass(), "Root Cause is null");
			return debugResult;
		}
		Stack<NodeFeedbacksPair> userFeedbackRecords = new Stack<>();
		
		Log.printMsg(this.getClass(),  "Start automatic debugging: " + result.projectName + ":" + result.bugID);
		
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
			final TraceNode proposedRootCause = debugPilot.locateRootCause();
			
			debugResult.probinfer_effort += this.measureEffort(proposedRootCause);
			
			Log.printMsg(getClass(), "Proposed root cause: " + proposedRootCause.getOrder());
			if (proposedRootCause.equals(rootCause)) {
				Log.printMsg(getClass(), "Root Cause is found ...");
				debugResult.probinfer_success = true;
				break;
			} else {
				long count = this.proposeHistory.stream().filter(n -> n.equals(proposedRootCause)).count();
				if (count >= 3) {
					debugResult.probinfer_success = false;
					break;
				}
				this.proposeHistory.add(proposedRootCause);
				NodeFeedbacksPair feedback = this.giveFeedback(proposedRootCause);
				Log.printMsg(getClass(), "Wrong root cause, feedback is given: " + feedback);
				userFeedbackRecords.add(feedback);
				totalFeedbackCount++;
			}
		}
		
		final double avgPropTime = propTimes.stream().collect(Collectors.averagingDouble(Double::doubleValue));
		final double avgPathFindingTime = pathFindingTimes.stream().collect(Collectors.averagingDouble(Double::doubleValue));
		final double avgTime = totalTimes.stream().collect(Collectors.averagingDouble(Double::doubleValue));
		
//		debugResult.avgPropTime = avgPropTime;
//		debugResult.avgPathFindingTime = avgPathFindingTime;
//		debugResult.avgTotalTime = avgTime;
		debugResult.correctFeedbackCount = correctFeedbackCount;
		debugResult.totalFeedbackCount = totalFeedbackCount;
		debugResult.debugPilotSuccess = debugSuccess;
//		debugResult.rootCauseCorrect = rootCauseCorrect;
		return debugResult;
	}
	
	private NodeFeedbacksPair giveFeedback(final TraceNode node) {
		UserFeedback feedback = this.feedbackAgent.giveFeedback(node, this.buggyTrace);
		NodeFeedbacksPair feedbackPair = new NodeFeedbacksPair(node, feedback);
		return feedbackPair;
	}
	
	protected int measureEffort(final TraceNode node) {
		return node.getReadVariables().size() + 3;
	}
}
