package tregression.tracematch;

import java.util.ArrayList;
import java.util.List;

import microbat.algorithm.graphdiff.Matcher;
import microbat.algorithm.graphdiff.MatchingGraphPair;
import microbat.model.trace.TraceNode;
import microbat.model.value.GraphNode;
import tregression.model.PairList;
import tregression.model.TraceNodePair;
import tregression.model.TraceNodeWrapper;
import tregression.separatesnapshots.DiffMatcher;
import tregression.util.DiffUtil;
import tregression.util.TraceNodeSimilarityComparator;

public class LCSMatcher implements Matcher {

	/**
	 * DiffMatcher contains the information of how source code should match with each other.
	 * If this field is null, we assume there is only one-line modification between original
	 * and regression version.
	 */
	private DiffMatcher matcher;
	private TraceNodeSimilarityComparator comparator;
	
	public LCSMatcher(DiffMatcher matcher, TraceNodeSimilarityComparator comparator) {
		this.matcher = matcher;
		this.comparator = comparator;
	}
	
	@Override
	public List<MatchingGraphPair> matchList(List<? extends GraphNode> childrenBefore,
			List<? extends GraphNode> childrenAfter) {
		
		TraceNode[] beforeList = transferToTraceNodeList(childrenBefore);
		TraceNode[] afterList = transferToTraceNodeList(childrenAfter);
		
		PairList pairList = DiffUtil.generateMatchedTraceNodeList(beforeList, afterList, matcher, comparator);
		
		List<MatchingGraphPair> matchingList = new ArrayList<>();
		for(TraceNodePair pair: pairList.getPairList()){
			TraceNodeWrapper wrapperBefore = new TraceNodeWrapper(pair.getAfterNode());
			TraceNodeWrapper wrapperAfter = new TraceNodeWrapper(pair.getBeforeNode());
			
			MatchingGraphPair match = new MatchingGraphPair(wrapperBefore, wrapperAfter);
			matchingList.add(match);
		}
		
		return matchingList;
	}

	private TraceNode[] transferToTraceNodeList(List<? extends GraphNode> children) {
		List<TraceNode> list = new ArrayList<>();
		
		for(GraphNode child: children){
			if(child instanceof TraceNodeWrapper){
				TraceNodeWrapper node = (TraceNodeWrapper)child;
				list.add(node.getTraceNode());
			}
		}
		
		TraceNode[] array = list.toArray(new TraceNode[0]);
		
		return array;
	}

	public DiffMatcher getMatcher() {
		return matcher;
	}

	public void setMatcher(DiffMatcher matcher) {
		this.matcher = matcher;
	}

}
