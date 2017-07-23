package tregression;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import microbat.model.BreakPoint;
import microbat.model.ClassLocation;
import microbat.model.ControlScope;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.model.variable.Variable;
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
	
	public void checkRootCause(TraceNode observedFaultNode, Trace buggyTrace, Trace correctTrace, PairList pairList, DiffMatcher matcher){
		getRegressionNodeList().add(observedFaultNode);
		
		List<TraceNodeW> workList = new ArrayList<>();
		workList.add(new TraceNodeW(observedFaultNode, true));
		
		StepChangeTypeChecker typeChecker = new StepChangeTypeChecker();
		
		
		while(!workList.isEmpty()){
			TraceNodeW stepW = workList.remove(0);
			TraceNode step = stepW.node;
//			boolean isOnBeforeTrace = stepW.isOnBefore;
			
			StepChangeType changeType = typeChecker.getType(step, stepW.isOnBefore, pairList, matcher);
			Trace trace = getCorrespondingTrace(stepW.isOnBefore, buggyTrace, correctTrace);
			
			if(changeType.getType()==StepChangeType.SRC){
				//TODO
			}
			else if(changeType.getType()==StepChangeType.DAT){
				for(VarValue readVar: changeType.getWrongVariableList()){
					trace = getCorrespondingTrace(stepW.isOnBefore, buggyTrace, correctTrace);
					
					TraceNode dataDom = trace.getStepVariableTable().get(readVar.getVarID()).getProducers().get(0); 
					addWorkNode(workList, dataDom, stepW.isOnBefore);
					
					TraceNode matchedStep = MatchStepFinder.findMatchedStep(stepW.isOnBefore, step, pairList);
					addWorkNode(workList, matchedStep, !stepW.isOnBefore);
					
					trace = getCorrespondingTrace(!stepW.isOnBefore, buggyTrace, correctTrace);
					
					VarValue matchedVar = MatchStepFinder.findMatchVariable(readVar, matchedStep);
					
					TraceNode otherDataDom = trace.getStepVariableTable().get(matchedVar.getVarID()).getProducers().get(0);
					addWorkNode(workList, otherDataDom, !stepW.isOnBefore);
				}
			}
			else if(changeType.getType()==StepChangeType.CTL){
//				TraceNode controlDom = step.getControlDominator();
				TraceNode controlDom = getInvocationMethodOrDominator(step);
				addWorkNode(workList, controlDom, stepW.isOnBefore);
				
				trace = getCorrespondingTrace(!stepW.isOnBefore, buggyTrace, correctTrace);
				
				if(controlDom.getOrder()==322) {
					System.currentTimeMillis();
					System.currentTimeMillis();
				}
				
				ClassLocation correspondingLocation = matcher.findCorrespondingLocation(controlDom.getBreakPoint(), !stepW.isOnBefore);
				
				TraceNode otherControlDom = findResponsibleControlDomOnOtherTrace(step, pairList, trace, !stepW.isOnBefore, correspondingLocation);
				addWorkNode(workList, otherControlDom, !stepW.isOnBefore);
				
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

	private TraceNode findResponsibleControlDomOnOtherTrace(TraceNode problematicStep, PairList pairList,
			Trace otherTrace, boolean isOtherTraceTheBeforeTrace, ClassLocation correspondingLocation) {
		
		int startOrder = findStartOrderInOtherTrace(problematicStep, pairList, !isOtherTraceTheBeforeTrace);
		int endOrder = findEndOrderInOtherTrace(problematicStep, pairList, !isOtherTraceTheBeforeTrace);
		
		
		List<BreakPoint> executedStatement = findAllExecutedStatement(otherTrace);
		
		//TODO this implementation is problematic, I need to use soot to analyze the static control dependence relation.
		for(int i=endOrder; i>=startOrder; i--){
			TraceNode node = otherTrace.getExectionList().get(i-1);
			if(node.isConditional()){
				HashSet<BreakPoint> allInfluenceScope = new HashSet<>(); 
				
				//TODO this method is problematic, I need Soot to parse the static dependence relation.
				collectAllInfluenceScope(node.getBreakPoint(), allInfluenceScope, executedStatement);
				
				for(BreakPoint location: allInfluenceScope){
					if(location.getDeclaringCompilationUnitName().equals(correspondingLocation.getClassCanonicalName()) &&
							location.getLineNumber()==correspondingLocation.getLineNumber()){
						return node;
					}
				}
			}
		}
		
		return null;
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
	
	public int findEndOrderInOtherTrace(TraceNode problematicStep, PairList pairList, boolean isOnBeforeTrace) {
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
		 * The the length of the other trace.
		 */
		TraceNode n = null;
		int size = pairList.getPairList().size();
		if(isOnBeforeTrace) {
			n = pairList.getPairList().get(size-1).getAfterNode();
		}
		else {
			n = pairList.getPairList().get(size-1).getBeforeNode();
		}
		int order = n.getOrder();
		while(n!=null) {
			n = n.getStepInNext();
			if(n!=null) {
				order = n.getOrder();
			}
		}
		return order;
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

	private TraceNode findLatestNode(List<TraceNode> visitedNodeList) {
		TraceNode node = null;
		for(TraceNode n: visitedNodeList){
			if(node == null){
				node = n;
			}
			else{
				if(n.getOrder()<node.getOrder()){
					node = n;
				}
			}
		}
		
		return node;
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
				
				/**
				 * method invocation will cause a return step with the same line number
				 */
				TraceNode previous = node.getStepOverPrevious();
				if(previous!=null && previous.getLineNumber()==node.getLineNumber()) {
					addWorkNode(workList, previous, isOnBeforeTrace);
				}
				TraceNode next = node.getStepOverNext();
				if(next!=null && next.getLineNumber()==node.getLineNumber()) {
					addWorkNode(workList, next, isOnBeforeTrace);
				}
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
	
	
}
