package tregression.tracematch;

import java.util.ArrayList;
import java.util.List;

import microbat.algorithm.graphdiff.MatchingGraphPair;
import microbat.model.BreakPoint;
import microbat.model.value.GraphNode;
import tregression.separatesnapshots.DiffMatcher;
import tregression.separatesnapshots.diff.FilePairWithDiff;

public class BucketBasedIndexTreeMatcher extends IndexTreeMatcher {

	public BucketBasedIndexTreeMatcher(DiffMatcher diffMatcher) {
		this.diffMatcher = diffMatcher;
	}
	
	class Bucket{
		public Bucket(List<Integer> sourceLines2, List<Integer> targetLines2) {
			this.sourceLines = sourceLines2;
			this.targetLines = targetLines2;
		}
		List<Integer> sourceLines = new ArrayList<>();
		List<Integer> targetLines = new ArrayList<>();
		
		List<GraphNode> childrenBefore = new ArrayList<>();
		List<GraphNode> childrenAfter = new ArrayList<>();
		
		public void mergeSourceLines(List<Integer> sourceLines2) {
			for(Integer line: sourceLines2){
				if(!sourceLines.contains(line)){
					sourceLines.add(line);
				}
			}
			
		}
		
		public void mergeTargetLines(List<Integer> targetLines2) {
			for(Integer line: targetLines2){
				if(!targetLines.contains(line)){
					targetLines.add(line);
				}
			}
			
		}
		
		public boolean sourceLinesInvovles(List<Integer> sourceLines2) {
			if(this.sourceLines==null || sourceLines2==null){
				return false;
			}
			
			for(Integer sourceLine: this.sourceLines){
				for(Integer l: sourceLines2){
					if(sourceLine.equals(l)){
						return true;
					}
				}
			}
			return false;
		}
		
		public boolean targetLinesInvolves(List<Integer> targetLines2) {
			if(this.targetLines==null || targetLines2==null){
				return false;
			}
			
			for(Integer targetLine: this.targetLines){
				for(Integer l: targetLines2){
					if(targetLine.equals(l)){
						return true;
					}
				}
			}
			return false;
		}
		
		
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
		
		List<Bucket> buckets = divideBuckets(childrenBefore, childrenAfter);
		
		for(Bucket bucket: buckets){
			
			if(bucket.childrenBefore.isEmpty() && !bucket.childrenAfter.isEmpty()){
				for(GraphNode node: bucket.childrenAfter){
					MatchingGraphPair pair = new MatchingGraphPair(null, node);
					pairList.add(pair);
				}
				continue;
			}
			
			if(!bucket.childrenBefore.isEmpty() && bucket.childrenAfter.isEmpty()){
				for(GraphNode node: bucket.childrenBefore){
					MatchingGraphPair pair = new MatchingGraphPair(node, null);
					pairList.add(pair);
				}
				continue;
			}
			
			for(GraphNode gNodeBefore: bucket.childrenBefore){
				IndexTreeNode gNodeAfter = findMostSimilarNode(gNodeBefore, bucket.childrenAfter, pairList);
				
				MatchingGraphPair pair = new MatchingGraphPair(gNodeBefore, gNodeAfter);
				pairList.add(pair);
				if(gNodeAfter!=null){
					pairMap.put(gNodeAfter.getTraceNode().getOrder(), pair);					
				}
			}
		}
		return pairList;
		
	}

	private Bucket isBucketListContains(List<Integer> sourceLines, List<Integer> targetLines, List<Bucket> buckets){
		if(sourceLines==null || targetLines==null){
			return null;
		}
		
		for(Bucket bucket: buckets){
			if(bucket.sourceLinesInvovles(sourceLines) || bucket.targetLinesInvolves(targetLines)){
				return bucket;
			}
		}
		
		return null;
	}
	
	private List<Bucket> divideBuckets(List<? extends GraphNode> childrenBefore,
			List<? extends GraphNode> childrenAfter) {
		List<Bucket> buckets = new ArrayList<>();
		
		for(GraphNode nodeBefore: childrenBefore){
			BreakPoint sourcePoint = ((IndexTreeNode)nodeBefore).getBreakPoint();
			FilePairWithDiff diff = diffMatcher.findDiffBySourceFile(sourcePoint);
			IndexTreeNode tNodeBefore = (IndexTreeNode)nodeBefore;
			List<Integer> targetLines = null;
			if(diff!=null){
				targetLines = diff.getSourceToTargetMap().get(tNodeBefore.getLineNumber());
			}
			else{
				targetLines = new ArrayList<>();
				targetLines.add(tNodeBefore.getLineNumber());
			}
			List<Integer> sourceLines = new ArrayList<>();
			sourceLines.add(tNodeBefore.getLineNumber());
			
			Bucket bucket = retrieveOrCreateABucket(buckets, targetLines, sourceLines);
			
			bucket.childrenBefore.add(tNodeBefore);
		}
		
		for(GraphNode nodeAfter: childrenAfter){
			BreakPoint targetPoint = ((IndexTreeNode)nodeAfter).getBreakPoint();
			FilePairWithDiff diff = diffMatcher.findDiffByTargetFile(targetPoint);
			IndexTreeNode tNodeAfter = (IndexTreeNode)nodeAfter;
			List<Integer> sourceLines = null;
			if(diff!=null){
				sourceLines = diff.getTargetToSourceMap().get(tNodeAfter.getLineNumber());
			}
			else{
				sourceLines = new ArrayList<>();
				sourceLines.add(tNodeAfter.getLineNumber());
			}
			List<Integer> targetLines = new ArrayList<>();
			targetLines.add(tNodeAfter.getLineNumber());
			
			Bucket bucket = retrieveOrCreateABucket(buckets, targetLines, sourceLines);
			bucket.childrenAfter.add(tNodeAfter);
		}
		
		return buckets;
	}

	private Bucket retrieveOrCreateABucket(List<Bucket> buckets, List<Integer> targetLines, List<Integer> sourceLines) {
		Bucket bucket = isBucketListContains(sourceLines, targetLines, buckets);
		if(bucket!=null){
			bucket.mergeSourceLines(sourceLines);
			bucket.mergeTargetLines(targetLines);
		}
		else{
			bucket = new Bucket(sourceLines, targetLines);
			buckets.add(bucket);
		}
		return bucket;
	}

	

}
