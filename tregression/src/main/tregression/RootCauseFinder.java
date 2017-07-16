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
import tregression.model.PairList;
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
		regressionNodeList.add(observedFaultNode);
		
		List<TraceNodeW> workList = new ArrayList<>();
		workList.add(new TraceNodeW(observedFaultNode, true));
		
		StepChangeTypeChecker typeChecker = new StepChangeTypeChecker();
		
		
		while(!workList.isEmpty()){
			TraceNodeW stepW = workList.remove(0);
			TraceNode step = stepW.node;
			boolean isOnBeforeTrace = stepW.isOnBefore;
			
			StepChangeType changeType = typeChecker.getType(step, isOnBeforeTrace, pairList, matcher);
			Trace trace = getCorrespondingTrace(isOnBeforeTrace, buggyTrace, correctTrace);
			
			if(changeType.getType()==StepChangeType.SRC){
				//TODO
			}
			else if(changeType.getType()==StepChangeType.DAT){
				for(VarValue readVar: changeType.getWrongVariableList()){
					TraceNode dataDom = trace.getLatestProducer(step.getOrder(), readVar.getVarID());
					addWorkNode(workList, dataDom, isOnBeforeTrace);
					
					TraceNode matchedStep = MatchStepFinder.findMatchedStep(isOnBeforeTrace, step, pairList);
					addWorkNode(workList, matchedStep, isOnBeforeTrace);
					
					isOnBeforeTrace = !isOnBeforeTrace;
					trace = getCorrespondingTrace(isOnBeforeTrace, buggyTrace, correctTrace);
					
					VarValue matchedVar = MatchStepFinder.findMatchVariable(readVar, matchedStep);
					TraceNode otherDataDom = trace.getLatestProducer(matchedStep.getOrder(), matchedVar.getVarID());
					addWorkNode(workList, otherDataDom, isOnBeforeTrace);
				}
			}
			else if(changeType.getType()==StepChangeType.CTL){
				TraceNode controlDom = step.getControlDominator();
				addWorkNode(workList, controlDom, isOnBeforeTrace);
				
				isOnBeforeTrace = !isOnBeforeTrace;
				
				trace = getCorrespondingTrace(isOnBeforeTrace, buggyTrace, correctTrace);
				
				ClassLocation correspondingLocation = matcher.findCorrespondingLocation(controlDom.getBreakPoint(), isOnBeforeTrace);
				
				TraceNode otherControlDom = findLatestControlDom(trace, isOnBeforeTrace, correspondingLocation);
				addWorkNode(workList, otherControlDom, isOnBeforeTrace);
				
			}
		}
		
	}

	private TraceNode findLatestControlDom(Trace trace, boolean isOnBeforeTrace, ClassLocation correspondingLocation) {
		List<TraceNode> visitedNodeList = isOnBeforeTrace ? regressionNodeList : correctNodeList;
		TraceNode latestNode = findLatestNode(visitedNodeList);
		if(null == latestNode){
			latestNode = trace.getLastestNode();
		}
		
		TraceNode invocationParent = latestNode.getInvocationParent();
		List<BreakPoint> executedStatement = findAllExecutedStatement(trace);
		
		for(int i=latestNode.getOrder(); i>=invocationParent.getOrder(); i--){
			TraceNode node = trace.getExectionList().get(i-1);
			if(node.isConditional()){
				HashSet<BreakPoint> allControlScope = new HashSet<>(); 
				collectAllControlScope(node.getBreakPoint(), allControlScope, executedStatement);
				
				for(BreakPoint location: allControlScope){
					if(location.getDeclaringCompilationUnitName().equals(correspondingLocation.getClassCanonicalName()) &&
							location.getLineNumber()==correspondingLocation.getLineNumber()){
						return node;
					}
				}
			}
		}
		
		return invocationParent;
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

	private List<BreakPoint> collectAllControlScope(BreakPoint p, HashSet<BreakPoint> allControlScope, 
			List<BreakPoint> executedStatements) {
		ControlScope scope = (ControlScope) p.getControlScope();
		if(scope != null) {
			for(ClassLocation location: scope.getRangeList()){
				BreakPoint point = findCorrespondingPoint(location, executedStatements);
				if(point != null && !allControlScope.contains(point)){
					allControlScope.add(point);
					
					collectAllControlScope(point, allControlScope, executedStatements);
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
				if(!regressionNodeList.contains(node)){
					regressionNodeList.add(node);
				}
				else {
					isVisited = true;
				}
			}
			else{
				if(!correctNodeList.contains(node)){
					correctNodeList.add(node);
				}
				else {
					isVisited = true;
				}
			}
			
			if(!isVisited) {
				workList.add(new TraceNodeW(node, isOnBeforeTrace));				
			}
		}
		
	}

	private Trace getCorrespondingTrace(boolean isOnBeforeTrace, Trace buggyTrace, Trace correctTrace) {
		return isOnBeforeTrace ? buggyTrace : correctTrace;
	}
	
	
}
