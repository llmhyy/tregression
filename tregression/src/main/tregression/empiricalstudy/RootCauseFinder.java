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
				return buggyTrace.getExectionList().get(startOrder-1);
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
			TraceNode buggyNode = buggyTrace.getExectionList().get(i);
			if(matcher.checkSourceDiff(buggyNode.getBreakPoint(), true)) {
				return new RootCauseNode(buggyNode, true);
			}
		}
		
		for(int i=correctTrace.size()-1; i>=0; i--) {
			TraceNode correctTraceNode = correctTrace.getExectionList().get(i);
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
			
//			String isBefore = stepW.isOnBefore?"before":"after";
//			System.out.println("On " + isBefore + " trace," + step);
//			System.out.println("It's a " + changeType.getType() + " type");
			
			if(changeType.getType()==StepChangeType.SRC){
				//TODO
				System.currentTimeMillis();
				System.currentTimeMillis();
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
					}
				}
			}
			else if(changeType.getType()==StepChangeType.CTL){
//				TraceNode controlDom = step.getControlDominator();
//				TraceNode controlDom = getInvocationMethodOrDominator(step);
				TraceNode controlDom = step.getControlDominator();
				if(controlDom==null){
					TraceNode invocationParent = step.getInvocationParent();
					if(!isMatchable(invocationParent, pairList, stepW.isOnBefore)){
						controlDom = invocationParent;
					}
				}
				addWorkNode(workList, controlDom, stepW.isOnBefore);
				
				trace = getCorrespondingTrace(!stepW.isOnBefore, buggyTrace, correctTrace);
				
				if(controlDom!=null && controlDom.getOrder()==322) {
					System.currentTimeMillis();
					System.currentTimeMillis();
				}
				
				ClassLocation correspondingLocation = matcher.findCorrespondingLocation(step.getBreakPoint(), !stepW.isOnBefore);
				
				TraceNode otherControlDom = findResponsibleControlDomOnOtherTrace(step, pairList, trace, !stepW.isOnBefore, correspondingLocation);
				addWorkNode(workList, otherControlDom, !stepW.isOnBefore);
				
			}
		}
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

	private TraceNode getInvocationMethodOrDominator(TraceNode step) {
		TraceNode controlDom = step.getControlDominator();
		TraceNode invocationParent = step.getInvocationParent();
		
		if(controlDom!=null && invocationParent!=null) {
			if(controlDom.getOrder()<invocationParent.getOrder()) {
				return invocationParent;
			}
			else {
				return controlDom;
			}
		}
		else if(controlDom!=null && invocationParent==null) {
			return controlDom;
		}
		else if(controlDom==null && invocationParent!=null) {
			return invocationParent;
		}
		
		return null;
	}

	public TraceNode findResponsibleControlDomOnOtherTrace(TraceNode problematicStep, PairList pairList,
			Trace otherTrace, boolean isOtherTraceTheBeforeTrace, ClassLocation correspondingLocation) {
		
		int startOrder = findStartOrderInOtherTrace(problematicStep, pairList, !isOtherTraceTheBeforeTrace);
		int endOrder = findEndOrderInOtherTrace(problematicStep, pairList, !isOtherTraceTheBeforeTrace, otherTrace);
		
		TraceNode bestNode = null;
		int value = -1;
		
		//TODO this implementation is problematic, I need to use soot to analyze the static control dependence relation.
		for(int i=endOrder; i>=startOrder; i--){
			if(i<=otherTrace.size()) {
				TraceNode node = otherTrace.getExectionList().get(i-1);
				if(node.isConditional()){
					if(node.getControlScope().containLocation(correspondingLocation)) {
						if(bestNode==null) {
							bestNode = node;
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
			TraceNode n = otherTrace.getExectionList().get(order0);
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
		for(TraceNode node: trace.getExectionList()){
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
	
	
}
