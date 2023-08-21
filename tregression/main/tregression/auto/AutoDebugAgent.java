package tregression.auto;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import debuginfo.NodeFeedbacksPair;
import microbat.debugpilot.DebugPilot;
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
import tregression.auto.result.DebugResult;
import tregression.auto.result.RunResult;
import tregression.empiricalstudy.EmpiricalTrial;

public class AutoDebugAgent {
	
	protected final Trace buggyTrace;
	protected final AutoFeedbackAgent feedbackAgent;
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
	
	public AutoDebugAgent(final EmpiricalTrial trial, List<VarValue> inputs, List<VarValue> outputs, TraceNode outputNode) {
		this.buggyTrace = trial.getBuggyTrace();
		this.feedbackAgent = new AutoFeedbackAgent(trial);
		this.inputs = inputs;
		this.outputs = outputs;
		this.outputNode = outputNode;
		this.gtRootCauses = this.extractrootCauses(trial);
	}
	
	protected Set<TraceNode> extractrootCauses(final EmpiricalTrial trail) {
		Set<TraceNode> rootCauses = new HashSet<>();
		rootCauses.addAll(this.extractTregressionRootCauses(trail));
		if (rootCauses.isEmpty()) {
			rootCauses.addAll(this.extractFirstDeviationNodes(trail));
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
			UserFeedback feedback = this.feedbackAgent.giveFeedback(node);
			if (!feedback.getFeedbackType().equals(UserFeedback.CORRECT)) {
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
		propagatorSettings.setPropagatorType(PropagatorType.SPP_CF);
		settings.setPropagatorSettings(propagatorSettings);
		
		PathFinderSettings pathFinderSettings = settings.getPathFinderSettings();
		pathFinderSettings.setPathFinderType(PathFinderType.Dijkstra);
		settings.setPathFinderSettings(pathFinderSettings);
		
		RootCauseLocatorSettings rootCauseLocatorSettings = settings.getRootCauseLocatorSettings();
		rootCauseLocatorSettings.setRootCauseLocatorType(RootCauseLocatorType.SPP);
		settings.setRootCauseLocatorSettings(rootCauseLocatorSettings);
		
		return settings;
	}
	
	public DebugResult startDebug(final RunResult result) {
		DebugPilotSettings settings = this.getSettings();
		
		DebugPilot debugPilot = new DebugPilot(settings);
		DebugResult debugResult = new DebugResult(result);
		
		Stack<NodeFeedbacksPair> userFeedbackRecords = new Stack<>();
		
		Log.printMsg(this.getClass(),  "Start automatic debugging: " + result.projectName + ":" + result.bugID);
		
		TraceNode currentNode = this.outputNode;
		boolean isEnd = false;
		
		// Measurement
		while (!isEnd) {
			debugPilot.updateFeedbacks(userFeedbackRecords);
			debugPilot.multiSlicing();
			
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
			TraceNode proposedRootCause =  debugPilot.locateRootCause();
			
			// Path finding
			long pathStartTime = System.currentTimeMillis();
			Log.printMsg(this.getClass(), "Constructing path to root cause ...");
			FeedbackPath feedbackPath =  debugPilot.constructPath(proposedRootCause);
			long pathEndTime = System.currentTimeMillis();
			double pathFindingTime = (pathEndTime - pathStartTime) / (double) 1000;
			pathFindingTimes.add(pathFindingTime);
			Log.printMsg(this.getClass(), "Path finding time: " + pathFindingTime);
			
			totalTimes.add(propTime + pathFindingTime);
			
			boolean needPropagateAgain = false;
			while (!needPropagateAgain && !isEnd) {
				UserFeedback predictedFeedback = feedbackPath.getFeedback(currentNode).getFirstFeedback();
				Log.printMsg(this.getClass(), "--------------------------------------");
				Log.printMsg(this.getClass(), "Predicted feedback of node: " + currentNode.getOrder() + ": " + predictedFeedback.toString());
				NodeFeedbacksPair userFeedbacks = this.giveFeedback(currentNode);
				Log.printMsg(this.getClass(), "Ground truth feedback: " + userFeedbacks);
				totalFeedbackCount += 1;
				
				// Reach the root case
				// UserFeedback type is unclear also tell that this node is root cause
				if (this.gtRootCauses.contains(currentNode)) {
					if (predictedFeedback.getFeedbackType().equals(UserFeedback.ROOTCAUSE)) {
						correctFeedbackCount+=1;
						rootCauseCorrect = true;
					}
					debugSuccess = true;
					isEnd = true;
					break;
				}
				
				if (userFeedbacks.containsFeedback(predictedFeedback)) {
					userFeedbackRecords.add(userFeedbacks);
					currentNode = TraceUtil.findNextNode(currentNode, predictedFeedback, buggyTrace);
					correctFeedbackCount+=1;
				} else if (userFeedbacks.getFeedbackType().equals(UserFeedback.CORRECT) ||
						TraceUtil.findNextNode(currentNode, userFeedbacks.getFirstFeedback(), buggyTrace) == null) {
					final TraceNode startNode = currentNode;
					final NodeFeedbacksPair prevsFeedbacksPair = userFeedbackRecords.peek();
					final TraceNode endNode = prevsFeedbacksPair.getNode();
					final UserFeedback feedback = prevsFeedbacksPair.getFirstFeedback();
					this.handleOmissionBug(startNode, endNode, feedback);
					isEnd = true;
				} else {
					Log.printMsg(this.getClass(), "Wong prediction on feedback, start propagation again");
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
		debugResult.rootCauseCorrect = rootCauseCorrect;
		return debugResult;
	}
	
	private NodeFeedbacksPair giveFeedback(final TraceNode node) {
		UserFeedback feedback = this.feedbackAgent.giveFeedback(node);
		NodeFeedbacksPair feedbackPair = new NodeFeedbacksPair(node, feedback);
		return feedbackPair;
	}
	
	public void handleOmissionBug(final TraceNode startNode, final TraceNode endNode, final UserFeedback feedback) {
		List<TraceNode> candidatesNodes = this.buggyTrace.getExecutionList().stream()
				.filter(node -> node.getOrder() > startNode.getOrder() && node.getOrder() < endNode.getOrder())
				.toList();
		if (!candidatesNodes.isEmpty()) {
			List<TraceNode> sortedList = this.sortNodeList(candidatesNodes);
			if (feedback.getFeedbackType().equals(UserFeedback.WRONG_PATH)) {
				this.handleControlOmissionBug(sortedList);
			} else if (feedback.getFeedbackType().equals(UserFeedback.WRONG_VARIABLE_VALUE)) {
				this.handleDataOmissionBug(sortedList, feedback.getOption().getReadVar());
			} else {
				throw new IllegalArgumentException(Log.genMsg(getClass(), "Unhandled feedback during omission bug"));
			}
		}
	}
	
	public void handleControlOmissionBug(List<TraceNode> candidatesList) {
		int left = 0;
		int right = candidatesList.size()-1;
		while (left <= right) {
			int mid = left+(right-left)/2;
			final TraceNode node = candidatesList.get(mid);
			this.totalFeedbackCount+=1;
			if (this.gtRootCauses.contains(node)) {
				this.correctFeedbackCount+=1;
				return;
			}
			UserFeedback feedback = this.feedbackAgent.giveFeedback(node);
			if (feedback.getFeedbackType().equals(UserFeedback.CORRECT)) {
				left = mid+1;
			} else {
				right = mid-1;
			}	
		}
	}
	
	public void handleDataOmissionBug(List<TraceNode> candidatesList, final VarValue wrongVar) {
		List<TraceNode> relatedCandidates = candidatesList.stream().filter(node -> node.isReadVariablesContains(wrongVar.getVarID())).toList();
		
		int startOrder = candidatesList.get(0).getOrder();
		int endOrder = candidatesList.get(candidatesList.size()-1).getOrder();
		for (TraceNode node : relatedCandidates) {
			this.totalFeedbackCount+=1;
			if (this.gtRootCauses.contains(node)) {
				this.correctFeedbackCount+=1;
				return;
			}
			UserFeedback userFeedback = this.feedbackAgent.giveFeedback(node);
			if (userFeedback.getFeedbackType().equals(UserFeedback.CORRECT)) {
				startOrder = node.getOrder();
			} else {
				endOrder = node.getOrder();
				break;
			}
		}
		
		final int startOrder_ = startOrder;
		final int endOrder_ = endOrder;
		List<TraceNode> filteredCandiates = candidatesList.stream().filter(node -> node.getOrder() >= startOrder_ && node.getOrder() <= endOrder_).toList();
		for (TraceNode node : filteredCandiates) {
			this.totalFeedbackCount+=1;
			if (this.gtRootCauses.contains(node)) {
				this.correctFeedbackCount+=1;
				return;
			}
		}
	}
	
	protected List<TraceNode> sortNodeList(final List<TraceNode> list) {
		return list.stream().sorted(
				new Comparator<TraceNode>() {
					@Override
					public int compare(TraceNode node1, TraceNode node2) {
						return node1.getOrder() - node2.getOrder();
					}
				}
			).toList();
	}
}
