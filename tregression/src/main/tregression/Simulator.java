package tregression;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import tregression.model.PairList;
import tregression.model.TraceNodePair;

public abstract class Simulator {
	
	protected PairList pairList;
	protected List<TraceNode> observedFaults;
	
	public abstract void prepare(Trace mutatedTrace, Trace correctTrace, PairList pairList, Object sourceDiffInfo);
	
	
	public PairList getPairList() {
		return pairList;
	}

	public void setPairList(PairList pairList) {
		this.pairList = pairList;
	}
	
	protected Map<Integer, TraceNode> findAllWrongNodes(PairList pairList, Trace buggyTrace){
		Map<Integer, TraceNode> actualWrongNodes = new HashMap<>();
		for(TraceNode buggyTraceNode: buggyTrace.getExectionList()){
			TraceNodePair foundPair = pairList.findByBeforeNode(buggyTraceNode);
			if(foundPair != null){
				if(!foundPair.isExactSame()){
					TraceNode mutatedNode = foundPair.getBeforeNode();
					actualWrongNodes.put(mutatedNode.getOrder(), mutatedNode);
				}
			}
			else{
				actualWrongNodes.put(buggyTraceNode.getOrder(), buggyTraceNode);
			}
		}
		return actualWrongNodes;
	}
	
	protected List<TraceNode> findObservedFault(List<TraceNode> wrongNodeList, PairList pairList){
		List<TraceNode> observedFaults = new ArrayList<>();
		for(TraceNode node: wrongNodeList) {
			if (isInvokedByTearDownMethod(node)) {
				continue;
			}
			else if(isObservedFaultWrongPath(node, pairList) && node.getControlDominator()==null) {
				observedFaults.add(node);
				if(node.isException()) {
					return observedFaults;
				}
				
				continue;
			}
			else {
				observedFaults.add(node);
				return observedFaults;
			}
		}
		
		return observedFaults;
	}
	
	private boolean isInvokedByTearDownMethod(TraceNode node) {
		TraceNode n = node;
		while(n!=null) {
			if(n.getMethodSign()!=null && n.getMethodSign().contains(".tearDown()V")) {
				return true;
			}
			else {
				n = n.getInvocationParent();
			}
		}
		
		return false;
	}


	private TraceNode findExceptionNode(List<TraceNode> wrongNodeList) {
		for(TraceNode node: wrongNodeList) {
			if(node.isException()) {
				return node;
			}
		}
		return null;
	}


	protected boolean isObservedFaultWrongPath(TraceNode observableNode, PairList pairList){
		TraceNodePair pair = pairList.findByBeforeNode(observableNode);
		if(pair == null){
			return true;
		}
		
		if(pair.getBeforeNode() == null){
			return true;
		}
		
		return false;
	}
}
