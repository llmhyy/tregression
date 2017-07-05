package tregression.model;

import java.util.ArrayList;
import java.util.List;

import microbat.model.trace.TraceNode;

public class PairList {
	private List<TraceNodePair> pairList = new ArrayList<>();

	public PairList(List<TraceNodePair> pairList) {
		super();
		this.pairList = pairList;
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

	public TraceNodePair findByBuggyNode(TraceNode node) {
		for(TraceNodePair pair: pairList){
			if(pair.getAfterNode().equals(node)){
				return pair;
			}
		}
		return null;
	}
	
	public TraceNodePair findByCorrectNode(TraceNode node) {
		for(TraceNodePair pair: pairList){
			if(pair.getBeforeNode().equals(node)){
				return pair;
			}
		}
		return null;
	}
	
	public int size(){
		return pairList.size();
	}
}
