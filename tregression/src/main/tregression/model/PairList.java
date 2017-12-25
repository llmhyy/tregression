package tregression.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import microbat.model.trace.TraceNode;

public class PairList {
	private List<TraceNodePair> pairList = new ArrayList<>();
	private Map<Integer, TraceNodePair> beforeNodeToPairMap = new HashMap<>();
	private Map<Integer, TraceNodePair> afterNodeToPairMap = new HashMap<>();

	public PairList(List<TraceNodePair> pairList) {
		super();
		this.pairList = pairList;
		for(TraceNodePair pair: pairList){
			TraceNode beforeNode = pair.getBeforeNode();
			TraceNode afterNode = pair.getAfterNode();
			
			if(beforeNode!=null){
				beforeNodeToPairMap.put(beforeNode.getOrder(), pair);
			}
			
			if(afterNode!=null){
				afterNodeToPairMap.put(afterNode.getOrder(), pair);
			}
		}
	}

	public List<TraceNodePair> getPairList() {
		return pairList;
	}

	public void setPairList(List<TraceNodePair> pairList) {
		this.pairList = pairList;
	}
	
	public void add(TraceNodePair pair){
		this.pairList.add(pair);
	}

	public TraceNodePair findByAfterNode(TraceNode node) {
//		for(TraceNodePair pair: pairList){
//			if(pair.getAfterNode().equals(node)){
//				return pair;
//			}
//		}
		if(node==null){
			return null;
		}
		TraceNodePair pair = afterNodeToPairMap.get(node.getOrder());
		return pair;
	}
	
	public TraceNodePair findByBeforeNode(TraceNode node) {
//		for(TraceNodePair pair: pairList){
//			if(pair.getBeforeNode().equals(node)){
//				return pair;
//			}
//		}
		if(node==null){
			return null;
		}
		TraceNodePair pair = beforeNodeToPairMap.get(node.getOrder());
		return pair;
	}
	
	public int size(){
		return pairList.size();
	}
	
	public boolean isPair(TraceNode node1, TraceNode node2, boolean isNode1Before) {
		if(isNode1Before) {
			return isPair(node1, node2);
		}
		else {
			return isPair(node2, node1);
		}
	}
	
	public boolean isPair(TraceNode beforeNode, TraceNode afterNode) {
		if(beforeNode==null || afterNode==null) {
			return false;
		}
		
		TraceNodePair pair = findByBeforeNode(beforeNode);
		if(pair!=null) {
			TraceNode n = pair.getBeforeNode();
			if(n != null) {
				return n.getOrder()==afterNode.getOrder();
			}
		}
		
		return false;
	}
}
