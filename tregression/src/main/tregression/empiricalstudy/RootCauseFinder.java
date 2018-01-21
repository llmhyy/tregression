package tregression.empiricalstudy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import microbat.model.BreakPoint;
import microbat.model.ClassLocation;
import microbat.model.ControlScope;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.trace.TraceNodeOrderComparator;
import microbat.model.value.VarValue;
import tregression.StepChangeType;
import tregression.StepChangeTypeChecker;
import tregression.model.PairList;
import tregression.model.TraceNodePair;
import tregression.separatesnapshots.DiffMatcher;

/**
 * This class implement the alignment slicing and mending algorithm.
 * 
 * @author Yun Lin
 *
 */
public class RootCauseFinder {
	
	class TraceNodeW{
		TraceNode node;
		boolean isOnBefore;
		public TraceNodeW(TraceNode node, boolean isOnBefore) {
			super();
			this.node = node;
			this.isOnBefore = isOnBefore;
		}
	}
	
	private List<TraceNode> regressionNodeList = new ArrayList<>();
	private List<TraceNode> correctNodeList = new ArrayList<>();
	
	private TraceNode rootCause;
	
	private List<MendingRecord> mendingRecordList = new ArrayList<>();
	
	public TraceNode retrieveRootCause(PairList pairList, DiffMatcher matcher, Trace buggyTrace, Trace correctTrace) {
		Collections.sort(regressionNodeList, new TraceNodeOrderComparator());
		Collections.sort(correctNodeList, new TraceNodeOrderComparator());
		
		StepChangeTypeChecker typeChecker = new StepChangeTypeChecker(buggyTrace, correctTrace);
		
		for(TraceNode node: regressionNodeList) {
			StepChangeType type = typeChecker.getType(node, true, pairList, matcher);
			if(type.getType()==StepChangeType.SRC) {
				return node;
			}
		}
		
		for(TraceNode node: correctNodeList) {
			StepChangeType type = typeChecker.getType(node, false, pairList, matcher);
			if(type.getType()==StepChangeType.SRC) {
				int startOrder  = findStartOrderInOtherTrace(node, pairList, false);
				return buggyTrace.getExecutionList().get(startOrder-1);
			}
		}
		
		return null;
	}
	
	private TraceNode findCorrespondingCorrectNode(PairList pairList, TraceNode buggyNode) {
		TraceNodePair pair = pairList.findByBeforeNode(buggyNode);
		if (pair != null) {
			TraceNode correctNode = pair.getAfterNode();
			if(correctNode!=null) {
				return correctNode;
			}
		}
		
		return null;
	}
	
	public RootCauseNode getRootCauseBasedOnDefects4J(PairList pairList, DiffMatcher matcher, Trace buggyTrace, Trace correctTrace) {
		for(int i=buggyTrace.size()-1; i>=0; i--) {
			TraceNode buggyNode = buggyTrace.getExecutionList().get(i);
			if(matcher.checkSourceDiff(buggyNode.getBreakPoint(), true)) {
				return new RootCauseNode(buggyNode, true);
			}
		}
		
		for(int i=correctTrace.size()-1; i>=0; i--) {
			TraceNode correctTraceNode = correctTrace.getExecutionList().get(i);
			if(matcher.checkSourceDiff(correctTraceNode.getBreakPoint(), false)) {
				return new RootCauseNode(correctTraceNode, false);
			}
		}
		
		return null;
	}
	
	private void checkRootCause(TraceNode observedFaultNode, Trace buggyTrace, Trace correctTrace, PairList pairList, DiffMatcher matcher){
		getRegressionNodeList().add(observedFaultNode);
		
		List<TraceNodeW> workList = new ArrayList<>();
		workList.add(new TraceNodeW(observedFaultNode, true));
		
		StepChangeTypeChecker typeChecker = new StepChangeTypeChecker(buggyTrace, correctTrace);
		
		while(!workList.isEmpty()){
			TraceNodeW stepW = workList.remove(0);
			TraceNode step = stepW.node;
			
			StepChangeType changeType = typeChecker.getType(step, stepW.isOnBefore, pairList, matcher);
			Trace trace = getCorrespondingTrace(stepW.isOnBefore, buggyTrace, correctTrace);
			
			System.currentTimeMillis();
			
//			String isBefore = stepW.isOnBefore?"before":"after";
//			System.out.println("On " + isBefore + " trace," + step);
//			System.out.println("It's a " + changeType.getType() + " type");
			
			if(changeType.getType()==StepChangeType.SRC){
				//TODO
				System.currentTimeMillis();
				break;
			}
			else if(changeType.getType()==StepChangeType.DAT){
				for(VarValue readVar: changeType.getWrongVariableList()){
					trace = getCorrespondingTrace(stepW.isOnBefore, buggyTrace, correctTrace);
					
					TraceNode dataDom = trace.findDataDominator(step, readVar); 
					addWorkNode(workList, dataDom, stepW.isOnBefore);
					
					TraceNode matchedStep = changeType.getMatchingStep();
					addWorkNode(workList, matchedStep, !stepW.isOnBefore);
					
					trace = getCorrespondingTrace(!stepW.isOnBefore, buggyTrace, correctTrace);
					
					VarValue matchedVar = MatchStepFinder.findMatchVariable(readVar, matchedStep);
					
					if(matchedVar != null) {
						TraceNode otherDataDom = trace.findDataDominator(matchedStep, matchedVar);
						addWorkNode(workList, otherDataDom, !stepW.isOnBefore);						
						appendDataMendingRecord(stepW, step, matchedStep, 
								typeChecker, dataDom, pairList, matcher, buggyTrace);
					}
					
				}
				
			}
			else if(changeType.getType()==StepChangeType.CTL){
//				TraceNode controlDom = step.getControlDominator();
				TraceNode controlDom = step.getInvocationMethodOrDominator();
//				TraceNode controlDom = step.getControlDominator();
				if(controlDom==null){
					TraceNode invocationParent = step.getInvocationParent();
					if(!isMatchable(invocationParent, pairList, stepW.isOnBefore)){
						controlDom = invocationParent;
					}
				}
				addWorkNode(workList, controlDom, stepW.isOnBefore);
				
				trace = getCorrespondingTrace(!stepW.isOnBefore, buggyTrace, correctTrace);
				
				ClassLocation correspondingLocation = matcher.findCorrespondingLocation(step.getBreakPoint(), !stepW.isOnBefore);
				
				TraceNode otherControlDom = findControlMendingNodeOnOtherTrace(step, pairList, trace, !stepW.isOnBefore, correspondingLocation);
				addWorkNode(workList, otherControlDom, !stepW.isOnBefore);
				
				appendControlRecord(stepW, otherControlDom, controlDom,
						typeChecker, pairList, matcher, buggyTrace);
				
			}
		}
	}
	
	private void appendControlRecord(TraceNodeW stepW, TraceNode matchingStep, TraceNode dominator, 
			StepChangeTypeChecker typeChecker, PairList pairList, DiffMatcher matcher, Trace buggyTrace) {
		if(!stepW.isOnBefore){
			return;
		}
		
		if(dominator==null){
			return;
		}
		
		StepChangeType domType = typeChecker.getType(dominator, true, pairList, matcher);
		
		if(domType.getType()==StepChangeType.IDT){
			TraceNode step = stepW.node;
			TraceNode prev = step.getStepInPrevious();
			StepChangeType changeType = typeChecker.getType(prev, true, pairList, matcher);
			while(changeType.getType()==StepChangeType.CTL){
				step = prev;
				prev = prev.getStepInPrevious();
				
				if(prev==null){
					break;
				}
				
				changeType = typeChecker.getType(prev, true, pairList, matcher);
			}
			
			List<TraceNode> returningPoints = new ArrayList<>();
			
			int order = step.getOrder();
			returningPoints.add(step);
			while(step.getStepOverPrevious()!=null && 
					step.getStepOverPrevious().getLineNumber()==step.getLineNumber()){
				step = step.getStepOverPrevious();
				returningPoints.add(step);
			}
			
			TraceNode start = buggyTrace.getTraceNode(order);
			TraceNode n = start.getStepOverNext();
			while(n!=null && (n.getLineNumber()==start.getLineNumber())){
				returningPoints.add(n);
				n = n.getStepOverNext();
			}
			
			for(TraceNode returningPoint: returningPoints){
				MendingRecord record = new MendingRecord(MendingRecord.CONTROL, step.getOrder(), 
						matchingStep.getOrder(), returningPoint.getOrder());
				if(!this.mendingRecordList.contains(record)) {
					this.mendingRecordList.add(record);						
				}
			}
		}
	}

	private void appendDataMendingRecord(TraceNodeW stepW, TraceNode step, 
			TraceNode matchingStep, StepChangeTypeChecker typeChecker, TraceNode dominator, 
			PairList pairList, DiffMatcher matcher, Trace buggyTrace) {
		if(!stepW.isOnBefore){
			return;
		}
		
		if(dominator==null){
			return;
		}
		
		StepChangeType domType = typeChecker.getType(dominator, true, pairList, matcher);
		
		if(domType.getType()==StepChangeType.IDT){
			List<TraceNode> returningPoints = new ArrayList<>();
			if(matchingStep != null){
				TraceNode domOnRef = null;
				StepChangeType matchingStepType = typeChecker.getType(matchingStep, false, pairList, matcher);
				if(matchingStepType.getWrongVariableList()==null) {
					return;
				}
				VarValue wrongVar = matchingStepType.getWrongVariableList().get(0);
				domOnRef = matchingStep.getDataDominator(wrongVar);
				
				while(domOnRef != null){
					StepChangeType changeType = typeChecker.getType(domOnRef, false, pairList, matcher);
					if(changeType.getType()==StepChangeType.SRC){
						returningPoints = findTheNearestCorrespondence(domOnRef, pairList, buggyTrace);
						break;
					}
					else{
						TraceNodePair conPair = pairList.findByAfterNode(domOnRef);
						if(conPair != null && conPair.getBeforeNode() != null){
							TraceNode returningPoint = conPair.getBeforeNode();
							returningPoints.add(returningPoint);
							break;
						}
						else{
							domOnRef = domOnRef.getInvocationMethodOrDominator();
						}
					}
				}
				
				for(TraceNode returningPoint: returningPoints){
					MendingRecord record = new MendingRecord(MendingRecord.DATA, step.getOrder(), 
							matchingStep.getOrder(), returningPoint.getOrder());
					record.setVarValue(wrongVar);
					if(!this.mendingRecordList.contains(record)) {
						this.mendingRecordList.add(record);						
					}
				}
			}
		}
	}

	private List<TraceNode> findTheNearestCorrespondence(TraceNode domOnRef, PairList pairList, Trace buggyTrace) {
		List<TraceNode> list = new ArrayList<>();
		
		TraceNodePair pair = pairList.findByAfterNode(domOnRef);
		if(pair!=null){
			TraceNode beforeNode = pair.getBeforeNode();
			if(beforeNode!=null){
				list.add(beforeNode);
				return list;
			}
		}
		int startOrder = findStartOrderInOtherTrace(domOnRef, pairList, false);
		TraceNode startNode = buggyTrace.getTraceNode(startOrder);
		list.add(startNode);
		while(startNode.getStepOverPrevious()!=null && 
				startNode.getStepOverPrevious().getLineNumber()==startNode.getLineNumber()){
			startNode = startNode.getStepOverPrevious();
			list.add(startNode);
		}
		
		TraceNode start = buggyTrace.getTraceNode(startOrder);
		TraceNode n = start.getStepOverNext();
		while(n!=null && (n.getLineNumber()==start.getLineNumber())){
			list.add(n);
			n = n.getStepOverNext();
		}
		
//		int endOrder = findEndOrderInOtherTrace(domOnRef, pairList, false, buggyTrace);
//		TraceNode endNode = buggyTrace.getTraceNode(endOrder);
//		while(endNode.getStepOverNext()!=null &&
//				endNode.getStepOverNext().getLineNumber()==endNode.getLineNumber()){
//			endNode = endNode.getStepOverNext();
//			list.add(endNode);
//		}
//		
//		for(int i=startOrder; i<=endOrder; i++){
//			TraceNode node = buggyTrace.getTraceNode(i);
//			list.add(node);
//		}
		
		return list;
	}

	private boolean isMatchable(TraceNode invocationParent, PairList pairList, boolean isOnBefore) {
		if(isOnBefore){
			TraceNodePair pair = pairList.findByBeforeNode(invocationParent);
			if(pair!=null){
				if(pair.getAfterNode()!=null){
					return true;
				}
			}
		}
		else{
			TraceNodePair pair = pairList.findByAfterNode(invocationParent);
			if(pair!=null){
				if(pair.getBeforeNode()!=null){
					return true;
				}
			}
		}
		return false;
	}

	public void checkRootCause(List<TraceNode> observedFaults, Trace buggyTrace, Trace correctTrace, PairList pairList, DiffMatcher matcher){
		for(TraceNode observedFaultNode:observedFaults){
			checkRootCause(observedFaultNode, buggyTrace, correctTrace, pairList, matcher);
			TraceNode root = retrieveRootCause(pairList, matcher, buggyTrace, correctTrace);
			System.currentTimeMillis();
			if(root!=null){
				break;
			}
		}
	}

	public TraceNode findControlMendingNodeOnOtherTrace(TraceNode problematicStep, PairList pairList,
			Trace otherTrace, boolean isOtherTraceTheBeforeTrace, ClassLocation correspondingLocation) {
		
		int startOrder = findStartOrderInOtherTrace(problematicStep, pairList, !isOtherTraceTheBeforeTrace);
		int endOrder = findEndOrderInOtherTrace(problematicStep, pairList, !isOtherTraceTheBeforeTrace, otherTrace);
		System.currentTimeMillis();
		TraceNode bestNode = null;
		int value = -1;
		
		//TODO this implementation is problematic, I need to use soot to analyze the static control dependence relation.
		TraceNode temp = null;
		for(int i=endOrder; i>=startOrder; i--){
			if(i<=otherTrace.size()) {
				TraceNode node = otherTrace.getExecutionList().get(i-1);
				if(node.isConditional()){
					temp = node;
					if(node.getControlScope().containLocation(correspondingLocation)) {
						if(bestNode==null) {
							TraceNode programaticInvocationParent = problematicStep.getInvocationParent();
							TraceNode invocationParent = node.getInvocationParent();
							
							if(programaticInvocationParent==null && invocationParent==null) {
								bestNode = node;								
							}
							else if(programaticInvocationParent!=null && invocationParent!=null){
								if(pairList.isPair(programaticInvocationParent, 
										invocationParent, !isOtherTraceTheBeforeTrace)) {
									bestNode = node;
								}
							}
						}
					}
					else{
//						List<TraceNode> allControlDominatees = new ArrayList<>();
//						retrieveAllControlDominatees(node, allControlDominatees);
						List<TraceNode> allControlDominatees = node.findAllControlDominatees();
						for(TraceNode controlDominatee: allControlDominatees){
							if(controlDominatee.isException()){
								if(value==-1) {
									bestNode = node;
									value++;
								}
								else {
									List<TraceNode> allDominatees = bestNode.findAllControlDominatees();
									if(allDominatees.contains(node)) {
										bestNode = node;
									}
								}
							}
						}
					}
					
				}				
			}
		}
		
		if(bestNode==null){
			bestNode = temp;
		}
		
		return bestNode;
	}

	
	private void retrieveAllControlDominatees(TraceNode node, List<TraceNode> allControlDominatees) {
		for(TraceNode controlDominatee: node.getControlDominatees()){
			if(!allControlDominatees.contains(controlDominatee)){
				allControlDominatees.add(controlDominatee);
				retrieveAllControlDominatees(controlDominatee, allControlDominatees);
			}
		}
	}

	public int findStartOrderInOtherTrace(TraceNode problematicStep, PairList pairList, boolean isOnBeforeTrace) {
		TraceNode node = problematicStep.getStepInPrevious();
		while(node != null) {
			TraceNode matchedNode = null;
			if(isOnBeforeTrace) {
				TraceNodePair pair = pairList.findByBeforeNode(node);
				if(pair != null) {
					matchedNode = pair.getAfterNode();
				}
			}
			else {
				TraceNodePair pair = pairList.findByAfterNode(node);
				if(pair != null) {
					matchedNode = pair.getBeforeNode();
				}
			}
			
			
			if(matchedNode != null) {
				return matchedNode.getOrder();
			}
			
			node = node.getStepInPrevious();
			
		}
		
		return 1;
	}
	
	public int findEndOrderInOtherTrace(TraceNode problematicStep, PairList pairList, boolean isOnBeforeTrace, Trace otherTrace) {
		TraceNode node = problematicStep.getStepInNext();
		while(node != null) {
			TraceNode matchedNode = null;
			if(isOnBeforeTrace) {
				TraceNodePair pair = pairList.findByBeforeNode(node);
				if(pair != null) {
					matchedNode = pair.getAfterNode();
				}
			}
			else {
				TraceNodePair pair = pairList.findByAfterNode(node);
				if(pair != null) {
					matchedNode = pair.getBeforeNode();
				}
			}
			
			
			if(matchedNode != null) {
				return matchedNode.getOrder();
			}
			
			node = node.getStepInNext();
		}
		
		/**
		 * Then, all the steps after problemStep cannot be matched in the other trace. 
		 */
		int order0 = findStartOrderInOtherTrace(problematicStep, pairList, isOnBeforeTrace);
		if(order0+1<=otherTrace.size()){
			TraceNode n = otherTrace.getExecutionList().get(order0);
			while(n!=null){
				if(n.isConditional()){
					if(n.getStepOverNext()!=null){
						return n.getStepOverNext().getOrder();
					}
					else{
						return n.getOrder();						
					}
				}
				else{
					if(n.getStepOverNext()!=null){
						n=n.getStepOverNext();						
					}
					else{
						n=n.getStepInNext();
					}
				}
			}
		}
		return otherTrace.size();
		
		/**
		 * The the length of the other trace.
		 */
//		TraceNode n = null;
//		int size = pairList.getPairList().size();
//		if(isOnBeforeTrace) {
//			n = pairList.getPairList().get(size-1).getAfterNode();
//		}
//		else {
//			n = pairList.getPairList().get(size-1).getBeforeNode();
//		}
//		int order = n.getOrder();
//		while(n!=null) {
//			n = n.getStepInNext();
//			if(n!=null) {
//				order = n.getOrder();
//			}
//		}
//		return order;
	}

	private List<BreakPoint> findAllExecutedStatement(Trace trace) {
		List<BreakPoint> pointList = new ArrayList<>();
		for(TraceNode node: trace.getExecutionList()){
			BreakPoint p = node.getBreakPoint();
			if(!pointList.contains(p)){
				pointList.add(p);
			}
		}
		
		return pointList;
	}

	private List<BreakPoint> collectAllInfluenceScope(BreakPoint p, HashSet<BreakPoint> allControlScope, 
			List<BreakPoint> executedStatements) {
		ControlScope scope = (ControlScope) p.getControlScope();
		if(scope != null) {
			for(ClassLocation location: scope.getRangeList()){
				BreakPoint point = findCorrespondingPoint(location, executedStatements);
				if(point != null && !allControlScope.contains(point)){
					allControlScope.add(point);
					
					collectAllInfluenceScope(point, allControlScope, executedStatements);
				}
			}			
		}
		return null;
	}

	private BreakPoint findCorrespondingPoint(ClassLocation location, List<BreakPoint> executedStatements) {
		for(BreakPoint p: executedStatements){
			if(location.getClassCanonicalName().equals(p.getDeclaringCompilationUnitName()) 
					&& location.getLineNumber()==p.getLineNumber()){
				return p;
			}
		}
		return null;
	}

	private void addWorkNode(List<TraceNodeW> workList, TraceNode node, boolean isOnBeforeTrace) {
		if(node != null){
			boolean isVisited = false;
			
			if(isOnBeforeTrace){
				if(!getRegressionNodeList().contains(node)){
					getRegressionNodeList().add(node);
				}
				else {
					isVisited = true;
				}
			}
			else{
				if(!getCorrectNodeList().contains(node)){
					getCorrectNodeList().add(node);
				}
				else {
					isVisited = true;
				}
			}
			
			if(!isVisited) {
				workList.add(new TraceNodeW(node, isOnBeforeTrace));
//				String str = isOnBeforeTrace?"before:":"after:";
//				System.out.println(str+node);
				/**
				 * method invocation will cause a return step with the same line number
				 */
//				TraceNode previous = node.getStepOverPrevious();
//				if(previous!=null && previous.getLineNumber()==node.getLineNumber()) {
//					addWorkNode(workList, previous, isOnBeforeTrace);
//				}
//				TraceNode next = node.getStepOverNext();
//				if(next!=null && next.getLineNumber()==node.getLineNumber()) {
//					addWorkNode(workList, next, isOnBeforeTrace);
//				}
			}
		}
		
	}

	private Trace getCorrespondingTrace(boolean isOnBeforeTrace, Trace buggyTrace, Trace correctTrace) {
		return isOnBeforeTrace ? buggyTrace : correctTrace;
	}

	public List<TraceNode> getRegressionNodeList() {
		return regressionNodeList;
	}

	public void setRegressionNodeList(List<TraceNode> regressionNodeList) {
		this.regressionNodeList = regressionNodeList;
	}

	public List<TraceNode> getCorrectNodeList() {
		return correctNodeList;
	}

	public void setCorrectNodeList(List<TraceNode> correctNodeList) {
		this.correctNodeList = correctNodeList;
	}

	public TraceNode getRootCause() {
		return rootCause;
	}

	public void setRootCause(TraceNode rootCause) {
		this.rootCause = rootCause;
	}

	public List<MendingRecord> getMendingRecordList() {
		return mendingRecordList;
	}

	public void setMendingRecordList(List<MendingRecord> mendingRecordList) {
		this.mendingRecordList = mendingRecordList;
	}
	
	
}
