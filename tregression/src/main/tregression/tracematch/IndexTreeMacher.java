package tregression.tracematch;

import java.util.ArrayList;
import java.util.List;

import microbat.algorithm.graphdiff.Matcher;
import microbat.algorithm.graphdiff.MatchingGraphPair;
import microbat.model.value.GraphNode;
import tregression.separatesnapshots.DiffMatcher;

public class IndexTreeMacher implements Matcher {

	/**
	 * DiffMatcher contains the information of how source code should match with each other.
	 * If this field is null, we assume there is only one-line modification between original
	 * and regression version.
	 */
	private DiffMatcher diffMatcher;
	
	public IndexTreeMacher(DiffMatcher diffMatcher) {
		this.diffMatcher = diffMatcher;
	}
	
	@Override
	public List<MatchingGraphPair> matchList(List<? extends GraphNode> childrenBefore,
			List<? extends GraphNode> childrenAfter) {
		
		List<MatchingGraphPair> pairList = new ArrayList<>();
		for(GraphNode gNodeBefore: childrenBefore){
			for(GraphNode gNodeAfter: childrenAfter){
				IndexTreeNode itNodeBefore = (IndexTreeNode)gNodeBefore;
				IndexTreeNode itNodeAfter = (IndexTreeNode)gNodeAfter;
				
				if(diffMatcher.isMatch(itNodeBefore.getBreakPoint(), itNodeAfter.getBreakPoint())){
					if(isControlPathCompatible(itNodeBefore, itNodeAfter)){
						MatchingGraphPair pair = new MatchingGraphPair(itNodeBefore, itNodeAfter);
						pairList.add(pair);
					}
				}
			}
		}
		
		return pairList;
		
	}

	private boolean isControlPathCompatible(IndexTreeNode itNodeBefore, IndexTreeNode itNodeAfter) {
		List<ControlNode> pathBefore = itNodeBefore.getControlPath();
		List<ControlNode> pathAfter = itNodeAfter.getControlPath();
		
		for(ControlNode nodeBefore: pathBefore){
			if(nodeBefore.getAppearOrder() > 1){
				boolean flag = canFindMatchingNode(nodeBefore, pathAfter);
				if(!flag){
					return false;
				}
			}
		}
		
		for(ControlNode nodeAfter: pathAfter){
			if(nodeAfter.getAppearOrder() > 1){
				boolean flag = canFindMatchingNode(nodeAfter, pathBefore);
				if(!flag){
					return false;
				}
			}
		}
		
		
		return true;
	}

	private boolean canFindMatchingNode(ControlNode node, List<ControlNode> path) {
		for(ControlNode thatNode: path){
			if(node.isMatchableWith(thatNode, diffMatcher)){
				return true;
			}
		}
		return false;
	}

}
