package tregression;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import tregression.model.PairList;
import tregression.model.TraceNodePair;

public abstract class Simulator {
	
	protected PairList pairList;
	protected TraceNode observedFaultNode;
	
	public abstract void prepare(Trace mutatedTrace, Trace correctTrace, PairList pairList, Object sourceDiffInfo);
	
	
	public PairList getPairList() {
		return pairList;
	}

	public void setPairList(PairList pairList) {
		this.pairList = pairList;
	}
	
	protected Map<Integer, TraceNode> findAllWrongNodes(PairList pairList, Trace mutatedTrace){
		Map<Integer, TraceNode> actualWrongNodes = new HashMap<>();
		for(TraceNode mutatedTraceNode: mutatedTrace.getExectionList()){
			TraceNodePair foundPair = pairList.findByAfterNode(mutatedTraceNode);
			if(foundPair != null){
				if(!foundPair.isExactSame()){
					TraceNode mutatedNode = foundPair.getAfterNode();
					actualWrongNodes.put(mutatedNode.getOrder(), mutatedNode);
				}
			}
			else{
				actualWrongNodes.put(mutatedTraceNode.getOrder(), mutatedTraceNode);
			}
		}
		return actualWrongNodes;
	}
	
	protected TraceNode findObservedFault(List<TraceNode> wrongNodeList, PairList pairList){
		TraceNode observedFaultNode = wrongNodeList.get(0);
		
		/**
		 * If the last portion of steps in trace are all wrong-path nodes, then we choose
		 * the one at the beginning of this portion as the observable step. 
		 */
		if(isObservedFaultWrongPath(observedFaultNode, pairList)){
			int index = 1;
			observedFaultNode = wrongNodeList.get(index);
			while(isObservedFaultWrongPath(observedFaultNode, pairList)){
				index++;
				if(index < wrongNodeList.size()){
					observedFaultNode = wrongNodeList.get(index);					
				}
				else{
					break;
				}
			}
			
			observedFaultNode = wrongNodeList.get(index-1);
			
			if(observedFaultNode.getControlDominator() == null){
				if(index < wrongNodeList.size()){
					observedFaultNode = wrongNodeList.get(index);					
				}
			}
		}
		
		return observedFaultNode;
	}
	
	private boolean isObservedFaultWrongPath(TraceNode observableNode, PairList pairList){
		TraceNodePair pair = pairList.findByAfterNode(observableNode);
		if(pair == null){
			return true;
		}
		
		if(pair.getBeforeNode() == null){
			return true;
		}
		
		return false;
	}
}
