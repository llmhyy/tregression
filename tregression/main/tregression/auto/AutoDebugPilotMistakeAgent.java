package tregression.auto;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import microbat.debugpilot.DebugPilot;
import microbat.debugpilot.NodeFeedbacksPair;
import microbat.debugpilot.fsc.AbstractDebugPilotState;
import microbat.debugpilot.fsc.DebugPilotFiniteStateMachine;
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
import microbat.recommendation.ChosenVariableOption;
import microbat.recommendation.UserFeedback;
import microbat.util.TraceUtil;
import tregression.auto.result.DebugResult;
import tregression.auto.result.RunResult;
import tregression.empiricalstudy.EmpiricalTrial;

public class AutoDebugPilotMistakeAgent {

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
	
	public AutoDebugPilotMistakeAgent(final EmpiricalTrial trial, List<VarValue> inputs, List<VarValue> outputs, TraceNode outputNode) {
		this.buggyTrace = trial.getBuggyTrace();
		this.feedbackAgent = new CarelessAutoFeedbackAgent(trial, 0.05d);
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
		rootCauses.removeIf(r -> r == null);
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
			UserFeedback feedback = this.feedbackAgent.giveGTFeedback(node);
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
		
		DebugPilot debugPilot = new DebugPilot(settings);
		this.debugResult = new DebugResult(result);
		debugResult.microbat_effort = 0.0d;
		debugResult.debugpilot_effort = 0.0d;
		
		
		Stack<NodeFeedbacksPair> userFeedbackRecords = new Stack<>();
		for (VarValue wrongVar : settings.getPropagatorSettings().getWrongVars()) {
			if (wrongVar.isConditionResult()) {
				UserFeedback feedback = new UserFeedback(UserFeedback.WRONG_PATH);
				NodeFeedbacksPair pair = new NodeFeedbacksPair(settings.getOutputNode(), feedback);
				userFeedbackRecords.add(pair);
			} else {
				UserFeedback feedback = new UserFeedback(UserFeedback.WRONG_VARIABLE_VALUE);
				feedback.setOption(new ChosenVariableOption(wrongVar, null));
 				NodeFeedbacksPair pair = new NodeFeedbacksPair(settings.getOutputNode(), feedback);
 				userFeedbackRecords.add(pair);
			}
			break;
		}
		TraceNode currentNode = this.outputNode;
		
		Log.printMsg(this.getClass(),  "Start automatic debugging: " + result.projectName + ":" + result.bugID);
		final DebugPilotFiniteStateMachine fsm = new DebugPilotFiniteStateMachine(debugPilot);
		fsm.setState(new PropagationState(fsm, userFeedbackRecords, currentNode, true));
		while (!fsm.isEnd()) {
			fsm.handleFeedback();
		}
		
		
		final double avgPropTime = propTimes.stream().collect(Collectors.averagingDouble(Double::doubleValue));
		final double avgPathFindingTime = pathFindingTimes.stream().collect(Collectors.averagingDouble(Double::doubleValue));
		final double avgTime = totalTimes.stream().collect(Collectors.averagingDouble(Double::doubleValue));
		
		debugResult.avgPropTime = avgPropTime;
		debugResult.avgPathFindingTime = avgPathFindingTime;
		debugResult.avgTotalTime = avgTime;
		debugResult.correctFeedbackCount = correctFeedbackCount;
		debugResult.totalFeedbackCount = totalFeedbackCount;
		debugResult.rootCauseCorrect = rootCauseCorrect;
		return debugResult;
	}
	
	private NodeFeedbacksPair giveFeedback(final TraceNode node) {
		UserFeedback feedback = this.feedbackAgent.giveFeedback(node, this.buggyTrace);
		NodeFeedbacksPair feedbackPair = new NodeFeedbacksPair(node, feedback);
		return feedbackPair;
	}
	
	private NodeFeedbacksPair giveGTFeedback(final TraceNode node) {
		UserFeedback feedback = this.feedbackAgent.giveGTFeedback(node);
		NodeFeedbacksPair feedbacksPair = new NodeFeedbacksPair(node, feedback);
		return feedbacksPair;
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
			UserFeedback feedback = this.feedbackAgent.giveGTFeedback(node);
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
			UserFeedback userFeedback = this.feedbackAgent.giveGTFeedback(node);
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
	
	protected int countAvaliableFeedback(final TraceNode node) {
		return node.getReadVariables().size() + 3; // Add 3 for control slicing, root cause, and correct
	}
	
	protected double measureMicorbatEffort(final TraceNode node) {
		int totalNoChoice = this.countAvaliableFeedback(node) + 1;
		return totalNoChoice / 2.0d;
	}
	
	protected double measureDebugPilotEffort(final TraceNode node, final UserFeedback feedback, final UserFeedback gtFeedback) {
		
		if (feedback.equals(gtFeedback)) {
			return 1.0d;
		}
		
		double maxSlicingSuspicious = -1.0d;
		
		// Find all possible feedback and corresponding suspicious
		Map<UserFeedback, Double> possibleFeedbackMap = new HashMap<>();
		for (VarValue readVar : node.getReadVariables()) {
			UserFeedback possibleFeedback = new UserFeedback(UserFeedback.WRONG_VARIABLE_VALUE);
			possibleFeedback.setOption(new ChosenVariableOption(readVar, null));
			if (!possibleFeedback.equals(feedback)) {
				possibleFeedbackMap.put(possibleFeedback, readVar.computationalCost);
				maxSlicingSuspicious = Math.max(maxSlicingSuspicious, readVar.computationalCost);
			}
		}
		final TraceNode controlDom = node.getControlDominator();
		UserFeedback possibleControlFeedback = new UserFeedback(UserFeedback.WRONG_PATH);
		possibleFeedbackMap.put(possibleControlFeedback, controlDom == null ? 0.0d : controlDom.getConditionResult().computationalCost);
		if (controlDom != null) {			
			maxSlicingSuspicious = Math.max(maxSlicingSuspicious, controlDom.getConditionResult().computationalCost);
		}
		
		if (gtFeedback.getFeedbackType().equals(UserFeedback.CORRECT) && maxSlicingSuspicious <= 0.2d) {
			return 3.0d;
		}
		
		// Sort the feedback in descending order
        List<Map.Entry<UserFeedback, Double>> slicingFeedbacks = new ArrayList<>(possibleFeedbackMap.entrySet());
        Comparator<Map.Entry<UserFeedback, Double>> valueComparator = (entry1, entry2) -> {
            return Double.compare(entry2.getValue(), entry1.getValue());
        };
        slicingFeedbacks.sort(valueComparator);
        List<UserFeedback> sortedFeedbackList = new ArrayList<>();
        sortedFeedbackList.add(new UserFeedback(UserFeedback.ROOTCAUSE));
        for (Map.Entry<UserFeedback, Double> entry : slicingFeedbacks) {
        	sortedFeedbackList.add(entry.getKey());
        }
        sortedFeedbackList.add(new UserFeedback(UserFeedback.CORRECT));
        
        // Start measuring effort
        double effort = 1.0d;
        for (UserFeedback sortedFeedback : sortedFeedbackList) {
        	effort += 1.0d;
        	if (sortedFeedback.equals(gtFeedback)) {
        		return effort;
        	}
        }
        throw new RuntimeException("GT Feedback is not in the sorted list");

	}
	
	protected boolean isWithing(final TraceNode rootCause, final TraceNode startNode, final TraceNode endNode) {
		return rootCause.getOrder() >= startNode.getOrder() && rootCause.getOrder() <= endNode.getOrder(); 
	}
	
	
	protected class PropagationState extends AbstractDebugPilotState {

		protected Stack<NodeFeedbacksPair> userFeedbackRecords;
		protected TraceNode currentNode;
		protected boolean microbatSuccess;
		
		public PropagationState(DebugPilotFiniteStateMachine stateMachine, final NodeFeedbacksPair initFeedbacksPair, TraceNode currentNode, boolean microbatSuccess) {
			super(stateMachine);
			this.currentNode = currentNode;
			this.userFeedbackRecords.add(initFeedbacksPair);
			this.microbatSuccess = microbatSuccess;
		}
		
		public PropagationState(DebugPilotFiniteStateMachine stateMachine, Stack<NodeFeedbacksPair> userFeedbackRecords, TraceNode currentNode, boolean microbatSuccess) {
			super(stateMachine);
			this.userFeedbackRecords = userFeedbackRecords;
			this.currentNode = currentNode;
			this.microbatSuccess = microbatSuccess;
		}
		
		@Override
		public void handleFeedback() {
			final DebugPilot debugPilot = this.stateMachine.getDebugPilot();
			debugPilot.updateFeedbacks(this.userFeedbackRecords);  
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
			
			long pathStartTime = System.currentTimeMillis();
			Log.printMsg(this.getClass(), "Constructing path to root cause ...");
			FeedbackPath feedbackPath =  debugPilot.constructPath(proposedRootCause);
			long pathEndTime = System.currentTimeMillis();
			double pathFindingTime = (pathEndTime - pathStartTime) / (double) 1000;
			pathFindingTimes.add(pathFindingTime);

			totalTimes.add(propTime + pathFindingTime);
			
			for (NodeFeedbacksPair nodeFeedbacksPair : feedbackPath) {
				if (!nodeFeedbacksPair.getNode().equals(this.currentNode)) {
					continue;
				}
				
				final UserFeedback predictedFeedback = nodeFeedbacksPair.getFirstFeedback();
				totalFeedbackCount += 1;
				
				NodeFeedbacksPair userFeedbacks = giveFeedback(currentNode);
				NodeFeedbacksPair gtUserFeedbacks = giveGTFeedback(currentNode);
				
				if (gtRootCauses.contains(this.currentNode)) {
					if (predictedFeedback.getFeedbackType().equals(UserFeedback.ROOTCAUSE)) {
						correctFeedbackCount+=1;
						rootCauseCorrect = true;
					} 
					debugResult.debugpilot_effort += measureDebugPilotEffort(currentNode, predictedFeedback, new UserFeedback(UserFeedback.ROOTCAUSE));
					debugResult.microbat_effort += measureMicorbatEffort(currentNode);
					debugSuccess = true;
					this.stateMachine.setState(new EndState(this.stateMachine, true, this.microbatSuccess));
					return;
				} else if (userFeedbacks.containsFeedback(predictedFeedback)) {
					this.userFeedbackRecords.add(userFeedbacks);
					debugResult.microbat_effort += measureMicorbatEffort(currentNode);
					debugResult.debugpilot_effort += measureDebugPilotEffort(currentNode, predictedFeedback, userFeedbacks.getFirstFeedback());
					this.currentNode = TraceUtil.findNextNode(currentNode, predictedFeedback, buggyTrace);					
					correctFeedbackCount+=1;
				} else if (gtUserFeedbacks.getFeedbackType().equals(UserFeedback.CORRECT)) {
					// Confirm with user the last node
//					this.userFeedbackRecords.pop();
					debugResult.microbat_effort += measureMicorbatEffort(currentNode);
					debugResult.debugpilot_effort += measureDebugPilotEffort(currentNode, predictedFeedback, userFeedbacks.getFirstFeedback());
					this.stateMachine.setState(new ConfirmState(this.stateMachine, this.userFeedbackRecords, this.microbatSuccess));
					return;
				} else if (TraceUtil.findNextNode(currentNode, userFeedbacks.getFirstFeedback(), buggyTrace) == null) {
					debugResult.microbat_effort += measureMicorbatEffort(currentNode);
					debugResult.debugpilot_effort += measureDebugPilotEffort(currentNode, predictedFeedback, userFeedbacks.getFirstFeedback());
					this.stateMachine.setState(new ConfirmState(this.stateMachine, this.userFeedbackRecords, this.microbatSuccess));
					return;
				} else {
					Log.printMsg(this.getClass(), "Wong prediction on feedback, start propagation again");

					this.userFeedbackRecords.add(userFeedbacks);
					
					debugResult.microbat_effort += measureMicorbatEffort(this.currentNode);
					debugResult.debugpilot_effort += measureDebugPilotEffort(this.currentNode, predictedFeedback, userFeedbacks.getFirstFeedback());
					
					this.currentNode = TraceUtil.findNextNode(currentNode, userFeedbacks.getFirstFeedback(), buggyTrace);
					this.stateMachine.setState(new PropagationState(this.stateMachine, this.userFeedbackRecords, this.currentNode, this.microbatSuccess));
					return;
				}	
			}
			throw new RuntimeException("Node does not in the path");
		}
	}
	
	protected class EndState extends AbstractDebugPilotState {

		protected boolean debugPilotSuccess;
		protected boolean microbatSuccess;
		public EndState(DebugPilotFiniteStateMachine stateMachine, boolean debugPilotSucess, boolean microbatSuccess) {
			super(stateMachine);
			this.debugPilotSuccess = debugPilotSucess;
			this.microbatSuccess = microbatSuccess;
		}

		@Override
		public void handleFeedback() {
			debugResult.debugPilotSuccess = debugPilotSuccess;
			debugResult.microbatSuccess = this.microbatSuccess;
			this.stateMachine.setEnd(true);
		}
	}
	
	protected class ConfirmState extends AbstractDebugPilotState {
		
		protected Stack<NodeFeedbacksPair> userFeedbackRecords;
		protected boolean microbatSuccess;
		
		public ConfirmState(DebugPilotFiniteStateMachine stateMachine, Stack<NodeFeedbacksPair> userFeedbackRecords, boolean microbatSuccess) {
			super(stateMachine);
			this.userFeedbackRecords = userFeedbackRecords;
			this.microbatSuccess = microbatSuccess;
		}
		
		@Override
		public void handleFeedback() {
			if (this.userFeedbackRecords.isEmpty()) {
				debugResult.debugPilotSuccess = false;
				this.stateMachine.setState(new EndState(stateMachine, false, false));
				return;
			}
			
			final NodeFeedbacksPair nodeFeedbacksPair = this.userFeedbackRecords.pop();
			final TraceNode node = nodeFeedbacksPair.getNode();
			final UserFeedback predictedFeedback = nodeFeedbacksPair.getFirstFeedback();
			
			NodeFeedbacksPair userFeedbacksPair = giveGTFeedback(node);
			
			debugResult.microbat_effort += measureMicorbatEffort(node);
			debugResult.debugpilot_effort += measureDebugPilotEffort(node, predictedFeedback, userFeedbacksPair.getFirstFeedback());
			
			if (userFeedbacksPair.containsFeedback(predictedFeedback)) {
				// Confirm that it is not mistake
				TraceNode nextNode = TraceUtil.findNextNode(node, predictedFeedback, buggyTrace);
				this.stateMachine.setState(new OmissionState(stateMachine, nextNode, node,this.microbatSuccess));
			} else if (userFeedbacksPair.getFeedbackType().equals(UserFeedback.ROOTCAUSE)) {
				// Give a wrong feedback to microbat
				this.stateMachine.setState(new EndState(stateMachine, true, false));
			} else if (userFeedbacksPair.getFeedbackType().equals(UserFeedback.CORRECT)) {
				this.stateMachine.setState(new ConfirmState(stateMachine, userFeedbackRecords, false));
			} else {
				TraceNode nextNode = TraceUtil.findNextNode(node, userFeedbacksPair.getFirstFeedback(), buggyTrace);
				this.userFeedbackRecords.add(userFeedbacksPair);
				this.stateMachine.setState(new PropagationState(stateMachine, this.userFeedbackRecords, nextNode, false));
			}
		}
	}
	
	protected class OmissionState extends AbstractDebugPilotState {
		
		protected TraceNode startNode;
		protected TraceNode endNode;
		
		protected boolean microbatSuccess;
		
		public OmissionState(DebugPilotFiniteStateMachine stateMachine, TraceNode startNode, TraceNode endNode, boolean microbatSuccess) {
			super(stateMachine);
			this.startNode = startNode;
			this.endNode = endNode;
			this.microbatSuccess = microbatSuccess;
		}
		
		@Override
		public void handleFeedback() {
			for (TraceNode rootCause : gtRootCauses) {
				if (this.withinRange(rootCause, this.startNode, this.endNode)) {
					this.stateMachine.setState(new EndState(this.stateMachine, true, this.microbatSuccess));
					return;
				}
			}
			this.stateMachine.setState(new EndState(this.stateMachine, false, false));
			return;
		}
		
		protected boolean withinRange(final TraceNode rootCause, final TraceNode startNode, final TraceNode endNode) {
			return rootCause.getOrder() >= startNode.getOrder() && rootCause.getOrder() <= endNode.getOrder();
		
			
		}
	}
}
