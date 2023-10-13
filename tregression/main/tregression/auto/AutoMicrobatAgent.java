package tregression.auto;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import microbat.debugpilot.pathfinding.PathFinderType;
import microbat.debugpilot.propagation.PropagatorType;
import microbat.debugpilot.rootcausefinder.RootCauseLocatorType;
import microbat.debugpilot.settings.DebugPilotSettings;
import microbat.debugpilot.settings.PathFinderSettings;
import microbat.debugpilot.settings.PropagatorSettings;
import microbat.debugpilot.settings.RootCauseLocatorSettings;
import microbat.debugpilot.userfeedback.DPUserFeedback;
import microbat.debugpilot.userfeedback.DPUserFeedbackType;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.util.TraceUtil;
import tregression.auto.result.DebugResult;
import tregression.auto.result.RunResult;
import tregression.empiricalstudy.EmpiricalTrial;
import tregression.empiricalstudy.RootCauseNode;
import tregression.model.TraceNodePair;

public class AutoMicrobatAgent {
	protected final Trace buggyTrace;
	protected final CarelessAutoFeedbackAgent feedbackAgent;
	protected final List<VarValue> inputs;
	protected final List<VarValue> outputs;
	protected final TraceNode outputNode;
	
	protected final Set<TraceNode> gtRootCauses;
	
	// Measurement
	protected int totalFeedbackCount = 0;
	protected int correctFeedbackCount = 0;
	protected List<Double> propTimes = new ArrayList<>();
	protected List<Double> pathFindingTimes = new ArrayList<>();
	protected List<Double> totalTimes = new ArrayList<>();
	protected boolean debugSuccess = false;
	protected boolean rootCauseCorrect = false;
	protected boolean microbatSuccess = true;
	
	protected DebugResult debugResult = null;
	
	protected final EmpiricalTrial trial;
	protected List<TraceNode> rootCausesAtCorrectTrace = new ArrayList<>();
	
	
	public AutoMicrobatAgent(final EmpiricalTrial trial, List<VarValue> inputs, List<VarValue> outputs, TraceNode outputNode, final double mistakeProbability) {
		this.buggyTrace = trial.getBuggyTrace();
		this.feedbackAgent = new CarelessAutoFeedbackAgent(trial, mistakeProbability);
		this.inputs = inputs;
		this.outputs = outputs;
		this.outputNode = outputNode;
		this.trial = trial;
		this.gtRootCauses = this.extractrootCauses(trial);
	}
	
	protected Set<TraceNode> extractrootCauses(final EmpiricalTrial trail) {
		Set<TraceNode> rootCauses = new HashSet<>();
		rootCauses.addAll(this.extractTregressionRootCauses(trail));
		if (rootCauses.isEmpty()) {
			rootCauses.addAll(this.extractFirstDeviationNodes(trail));
		}
		rootCauses.removeIf(r -> r == null);
		
		for (RootCauseNode rootCause : trial.getRootCauseFinder().getRealRootCaseList()) {
			if (!rootCause.isOnBefore()) {
				this.rootCausesAtCorrectTrace.add(rootCause.getRoot());
			}
		}
		
		return rootCauses;
	}
	
	protected Set<TraceNode> extractTregressionRootCauses(final EmpiricalTrial trail) {
		Set<TraceNode> rootCauses = new HashSet<>();
		rootCauses.addAll(trail.getRootCauseFinder().getRealRootCaseList().stream().
				filter(rootCause -> rootCause.isOnBefore()).
				map(rootCause -> rootCause.getRoot()).
				toList());
		rootCauses.add(trail.getRootcauseNode());
		return rootCauses;
	}
	
	protected Set<TraceNode> extractFirstDeviationNodes(final EmpiricalTrial trial) {
		Set<TraceNode> rootCauses = new HashSet<>();
		TraceNode firstStep = null;
		for (TraceNode node : trial.getBuggyTrace().getExecutionList()) {
//			UserFeedback feedback = this.feedbackAgent.giveGTFeedback(node);
			DPUserFeedback feedback = this.feedbackAgent.giveGTFeedback(node);
//			if (!feedback.getFeedbackType().equals(UserFeedback.CORRECT)) {
			if (feedback.getType() == DPUserFeedbackType.CORRECT) {
				firstStep = node;
				break;
			}
		}
		
		final TraceNode firstStep_ = firstStep;
		rootCauses.addAll(
			trial.getBuggyTrace().getExecutionList().stream()
			.filter(node -> 
				node.getClassCanonicalName().equals(firstStep_.getClassCanonicalName()) &&
				node.getLineNumber() == firstStep_.getLineNumber()
			).toList()
		);
		
		return rootCauses;
	}
	
	protected DebugPilotSettings getSettings() {
		DebugPilotSettings settings = new DebugPilotSettings();
		settings.setTrace(this.buggyTrace);
		settings.setCorrectVars(new HashSet<>(this.inputs));
		settings.setWrongVars(new HashSet<>(this.outputs));
		settings.setOutputNode(outputNode);
		
		PropagatorSettings propagatorSettings = settings.getPropagatorSettings();
		propagatorSettings.setPropagatorType(PropagatorType.SPPS_CS);
		settings.setPropagatorSettings(propagatorSettings);
		
		PathFinderSettings pathFinderSettings = settings.getPathFinderSettings();
		pathFinderSettings.setPathFinderType(PathFinderType.SuspiciousDijkstraExp);
		settings.setPathFinderSettings(pathFinderSettings);
		
		RootCauseLocatorSettings rootCauseLocatorSettings = settings.getRootCauseLocatorSettings();
		rootCauseLocatorSettings.setRootCauseLocatorType(RootCauseLocatorType.SUSPICIOUS);
		settings.setRootCauseLocatorSettings(rootCauseLocatorSettings);
		
		return settings;
	}
	
	public DebugResult startDebug(final RunResult result) {
		DebugPilotSettings settings = this.getSettings();
		
		this.debugResult = new DebugResult(result);
		this.debugResult.microbat_effort = 0.0d;
		this.debugResult.debugpilot_effort = 0.0d;
		this.debugResult.microbatSuccess = false;
		
		TraceNode currentNode = this.outputNode;
		boolean isEnd = false;

		Stack<DPUserFeedback> userFeedbackRecords = new Stack<>();
		final TraceNode outputNode = settings.getOutputNode();
		DPUserFeedback feedback = null;
		for (VarValue wrongVar : settings.getPropagatorSettings().getWrongVars()) {
			if (wrongVar.isConditionResult()) {
				feedback = new DPUserFeedback(DPUserFeedbackType.WRONG_PATH, outputNode);
				userFeedbackRecords.add(feedback);
				break;
			} else {
				feedback = new DPUserFeedback(DPUserFeedbackType.WRONG_VARIABLE, outputNode);
				feedback.addWrongVar(wrongVar);
			}
		}
		userFeedbackRecords.add(feedback);
		
//		Stack<NodeFeedbacksPair> userFeedbackRecords = new Stack<>();
//		for (VarValue wrongVar : settings.getPropagatorSettings().getWrongVars()) {
//			if (wrongVar.isConditionResult()) {
//				UserFeedback feedback = new UserFeedback(UserFeedback.WRONG_PATH);
//				NodeFeedbacksPair pair = new NodeFeedbacksPair(settings.getOutputNode(), feedback);
//				userFeedbackRecords.add(pair);
//			} else {
//				UserFeedback feedback = new UserFeedback(UserFeedback.WRONG_VARIABLE_VALUE);
//				feedback.setOption(new ChosenVariableOption(wrongVar, null));
// 				NodeFeedbacksPair pair = new NodeFeedbacksPair(settings.getOutputNode(), feedback);
// 				userFeedbackRecords.add(pair);
//			}
//			break;
//		}
		
		long startDebugTime = System.currentTimeMillis();
		
		while (!isEnd) {
			this.debugResult.totalFeedbackCount += 1;
			this.debugResult.microbat_effort += this.measureMicrobatEffort(currentNode);
			
			// Reach root cause
			if (this.gtRootCauses.contains(currentNode)) {
				this.debugResult.microbatSuccess = true;
				isEnd = true;
				break;
			}
			
//			DPUserFeedback userFeedbacksPair = this.giveFeedback(currentNode);
//			UserFeedback userFeedback = userFeedbacksPair.getFirstFeedback();
			
			DPUserFeedback userFeedback = this.giveFeedback(outputNode);
			DPUserFeedback gtFeedbacksPair = this.giveGTFeedback(currentNode);
			
//			if (!gtFeedbacksPair.equals(userFeedbacksPair)) {
//				this.debugResult.microbatSuccess = false;
//				isEnd = true;
//				break;
//			}
			
//			if (userFeedbacksPair.getFeedbackType().equals(UserFeedback.CORRECT)) {
			if (userFeedback.getType() == DPUserFeedbackType.CORRECT) {
				// Correct feedback is given, omission bug detected
				final TraceNode startNode = currentNode;
				final DPUserFeedback prevsFeedbacksPair = userFeedbackRecords.peek();
				final TraceNode endNode= prevsFeedbacksPair.getNode();
				
				this.debugResult.microbat_effort += this.measureMicrobatEffort(endNode);
				
				// Check is first deviation point lies in range
				for (TraceNode rootCause : gtRootCauses) {
					if (rootCause.getOrder() >= startNode.getOrder() && rootCause.getOrder() <= endNode.getOrder()) {
						this.debugResult.microbatSuccess = true;
						break;
					}
				}
				
				// Check is first deviation point lies in range in correct trace
				final TraceNodePair startNodePair = this.trial.getPairList().findByBeforeNode(startNode);
				final TraceNodePair endNodePair = this.trial.getPairList().findByBeforeNode(endNode);
				if (startNodePair != null && endNodePair != null) {
					final TraceNode startNodeAtCorrectTrace = startNodePair.getAfterNode();
					final TraceNode endNodeAtCorrectTrace = endNodePair.getAfterNode();
					
					for (TraceNode rootCause : this.rootCausesAtCorrectTrace) {
						if (rootCause.getOrder() >= startNodeAtCorrectTrace.getOrder() && rootCause.getOrder() <= endNodeAtCorrectTrace.getOrder()) {
							this.debugResult.microbatSuccess = true;
							break;
						}
					}
				}
				
				isEnd = true;
//			} else if (TraceUtil.findNextNode(currentNode, userFeedback, buggyTrace) == null) {
			} else if (TraceUtil.findAllNextNodes(userFeedback).isEmpty()) {
				TraceNode startNode = currentNode.getInvocationMethodOrDominator();
				final TraceNode endNode = currentNode;
				this.debugResult.microbat_effort += this.measureMicrobatEffort(endNode);
				// Check is first deviation point lies in range
				if (startNode != null) {					
					for (TraceNode rootCause : gtRootCauses) {
						if (rootCause.getOrder() >= startNode.getOrder() && rootCause.getOrder() <= endNode.getOrder()) {
							this.debugResult.microbatSuccess = true;
							break;
						}
					}
				}
				
				// Check is first deviation point lies in range in correct trace
				final TraceNodePair startNodePair = this.trial.getPairList().findByBeforeNode(startNode);
				final TraceNodePair endNodePair = this.trial.getPairList().findByBeforeNode(endNode);
				if (startNodePair != null && endNodePair != null) {
					final TraceNode startNodeAtCorrectTrace = startNodePair.getAfterNode();
					final TraceNode endNodeAtCorrectTrace = endNodePair.getAfterNode();
					
					for (TraceNode rootCause : this.rootCausesAtCorrectTrace) {
						if (rootCause.getOrder() >= startNodeAtCorrectTrace.getOrder() && rootCause.getOrder() <= endNodeAtCorrectTrace.getOrder()) {
							this.debugResult.microbatSuccess = true;
							break;
						}
					}
				}
				
				isEnd = true;
			} else {
				// Give feedback normally
//				currentNode = TraceUtil.findNextNode(currentNode, userFeedback, buggyTrace);
				currentNode = TraceUtil.findAllNextNodes(userFeedback).stream().findFirst().orElse(null);
				userFeedbackRecords.add(userFeedback);
			}
		}
		
		long endDebugTime = System.currentTimeMillis();
		this.debugResult.debug_time = (endDebugTime - startDebugTime) / 1000.0d;
		
		
		return this.debugResult;
		
	}
	
	protected DPUserFeedback giveFeedback(final TraceNode node) {
		return this.feedbackAgent.giveFeedback(node, this.buggyTrace);
//		NodeFeedbacksPair feedbacksPair = new NodeFeedbacksPair(node, feedback);
//		return feedbacksPair;
	}
	
	protected DPUserFeedback giveGTFeedback(final TraceNode node) {
		return this.feedbackAgent.giveGTFeedback(node);
//		NodeFeedbacksPair feedbacksPair = new NodeFeedbacksPair(node, feedback);
//		return feedbacksPair;
	}
	
	protected double measureMicrobatEffort(final TraceNode node) {
		int totalNoChoice = this.countAvaliableFeedback(node) + 1;
		return (double) totalNoChoice / 2.0d;
	}
	
	protected int countAvaliableFeedback(final TraceNode node) {
		return node.getReadVariables().size() + 3; // Add 3 for control slicing, root cause, and correct
	}
}
