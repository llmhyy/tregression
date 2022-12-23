package tregression.tracematch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import microbat.algorithm.graphdiff.MatchingGraphPair;
import microbat.model.BreakPoint;
import microbat.model.trace.TraceNode;
import microbat.model.value.GraphNode;
import tregression.separatesnapshots.DiffMatcher;

public class HierarchicalIndexTreeMatcher extends IndexTreeMatcher {
	
	public HierarchicalIndexTreeMatcher(DiffMatcher diffMatcher) {
		this.diffMatcher = diffMatcher;
	}
	
	@Override
	public List<MatchingGraphPair> matchList(List<? extends GraphNode> childrenBefore,
			List<? extends GraphNode> childrenAfter) {
		List<MatchingGraphPair> pairList = new ArrayList<>();
		if(childrenBefore.isEmpty() && childrenAfter.isEmpty()){
			return pairList;
		}
		else if(childrenBefore.isEmpty() && !childrenAfter.isEmpty()){
			for(GraphNode node: childrenAfter){
				MatchingGraphPair pair = new MatchingGraphPair(null, node);
				pairList.add(pair);
			}
			return pairList;
		}
		else if(!childrenBefore.isEmpty() && childrenAfter.isEmpty()){
			for(GraphNode node: childrenBefore){
				MatchingGraphPair pair = new MatchingGraphPair(node, null);
				pairList.add(pair);
			}
			return pairList;
		}
		
		List<IndexTreeNode> treeBefore = parseTopNodesIndexTree(childrenBefore);
		List<IndexTreeNode> treeAfter = parseTopNodesIndexTree(childrenAfter);
		
		pairList = new ArrayList<>();
		matchIndexTree(treeBefore, treeAfter, pairList);
		
		return pairList;
	}

	private List<IndexTreeNode> parseTopNodesIndexTree(List<? extends GraphNode> nodes) {
		List<IndexTreeNode> topNodes = new ArrayList<>();
		for(GraphNode n: nodes){
			IndexTreeNode itNode = (IndexTreeNode)n;
			TraceNode invocationParent = itNode.getTraceNode().getInvocationParent();
			TraceNode loopParent = itNode.getTraceNode().getLoopParent();
			if(loopParent==null && invocationParent!=null){
				topNodes.add(itNode);
			}
			else if(invocationParent==null){
				topNodes.add(itNode);
			}
			else if(loopParent.getOrder()<invocationParent.getOrder()){
				topNodes.add(itNode);
			}
			
		}
		return topNodes;
	}

	private void matchIndexTree(List<IndexTreeNode> treeBefore, List<IndexTreeNode> treeAfter,
			List<MatchingGraphPair> pairList) {
		
		Map<Integer, MatchingGraphPair> pairMap = new HashMap<>();
		
		Map<BreakPoint, List<IndexTreeNode>> buckets = new HashMap<>();
		for(IndexTreeNode afterNode: treeAfter){
			List<IndexTreeNode> list = buckets.get(afterNode.getBreakPoint());
			if(list == null){
				list = new ArrayList<>();
			}
			list.add(afterNode);
			buckets.put(afterNode.getBreakPoint(), list);
		}
		
		List<MatchingGraphPair> pairs = new ArrayList<>();
		for(IndexTreeNode nodeBefore: treeBefore){
			List<IndexTreeNode> nodeAfterList = filterByMatchedLocation(nodeBefore, buckets);
			IndexTreeNode matchedNodeAfter = findMostSimilarNode(nodeBefore, nodeAfterList, pairMap);
			if(null != matchedNodeAfter){
				MatchingGraphPair pair = new MatchingGraphPair(nodeBefore, matchedNodeAfter);
				pairs.add(pair);
				pairMap.put(matchedNodeAfter.getOrder(), pair);
			}
		}
		pairList.addAll(pairs);
		
		for(MatchingGraphPair pair: pairs){
			List<IndexTreeNode> childrenBefore = getLoopChildren(pair.getNodeBefore());
			List<IndexTreeNode> childrenAfter = getLoopChildren(pair.getNodeAfter());
			
			matchIndexTree(childrenBefore, childrenAfter, pairList);
		}
	}

	private List<IndexTreeNode> filterByMatchedLocation(IndexTreeNode nodeBefore, Map<BreakPoint, List<IndexTreeNode>> buckets) {
		List<IndexTreeNode> list = new ArrayList<>();
		BreakPoint beforePoint = nodeBefore.getBreakPoint();
		
		for(BreakPoint afterPoint: buckets.keySet()){
			boolean isMatch = diffMatcher.isMatch(beforePoint, afterPoint);
			if(isMatch){
				list.addAll(buckets.get(afterPoint));
			}
		}
		
		Collections.sort(list, new Comparator<IndexTreeNode>(){
			@Override
			public int compare(IndexTreeNode o1, IndexTreeNode o2) {
				return o1.getOrder() - o2.getOrder();
			}
		});
		return list;
	}

	private List<IndexTreeNode> getLoopChildren(GraphNode node) {
		List<IndexTreeNode> list = new ArrayList<>();
		IndexTreeNode itNode = (IndexTreeNode)node;
		for(TraceNode traceNode: itNode.getTraceNode().getLoopChildren()){
			IndexTreeNode indexNode = itNode.fetchIndexTreeNode(traceNode);
			list.add(indexNode);
		}
		return list;
	}

}
