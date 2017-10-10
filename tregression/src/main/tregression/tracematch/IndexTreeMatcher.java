package tregression.tracematch;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;

import microbat.algorithm.graphdiff.Matcher;
import microbat.algorithm.graphdiff.MatchingGraphPair;
import microbat.model.BreakPoint;
import microbat.model.value.GraphNode;
import microbat.util.JavaUtil;
import tregression.separatesnapshots.DiffMatcher;
import tregression.separatesnapshots.diff.FilePairWithDiff;

public class IndexTreeMatcher implements Matcher {

	/**
	 * DiffMatcher contains the information of how source code should match with each other.
	 * If this field is null, we assume there is only one-line modification between original
	 * and regression version.
	 */
	private DiffMatcher diffMatcher;
	
	public IndexTreeMatcher(DiffMatcher diffMatcher) {
		this.diffMatcher = diffMatcher;
	}
	
	private Map<Integer, MatchingGraphPair> pairMap = new HashMap<>();
	
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

	private IndexTreeNode findMostSimilarNode(GraphNode gNodeBefore, List<? extends GraphNode> childrenAfter, 
			List<MatchingGraphPair> pairList) {
		IndexTreeNode mostSimilarNode = null;
		double sim = -1;
		
		IndexTreeNode itNodeBefore = (IndexTreeNode)gNodeBefore;
		for(GraphNode gNodeAfter: childrenAfter){
			IndexTreeNode itNodeAfter = (IndexTreeNode)gNodeAfter;
			if(hasMatched(pairList, itNodeAfter)){
				continue;
			}
			
			if(isControlPathCompatible(itNodeBefore, itNodeAfter)){
				if(mostSimilarNode==null){
					mostSimilarNode = itNodeAfter;
					sim = sim(itNodeBefore, itNodeAfter);
				}
				else{
					double sim1 = sim(itNodeBefore, itNodeAfter);
					if(sim1 > sim){
						mostSimilarNode = itNodeAfter;
						sim = sim1;
					}
				}
			}
		}
		
		if(sim==0){
			mostSimilarNode = null;
		}
		
		return mostSimilarNode;
	}
	
	private boolean hasMatched(List<MatchingGraphPair> pairList, IndexTreeNode itNodeAfter) {
		MatchingGraphPair p = pairMap.get(itNodeAfter.getTraceNode().getOrder());
		return p!=null;
//		for(MatchingGraphPair pair: pairList){
//			if(pair.getNodeAfter()==itNodeAfter){
//				return true;
//			}
//		}
//		return false;
	}
	
	private Map<String, List<String>> lineMap = new HashMap<>();

	private double sim(IndexTreeNode itNodeBefore, IndexTreeNode itNodeAfter) {
		BreakPoint pointBefore = itNodeBefore.getBreakPoint();
		BreakPoint pointAfter = itNodeAfter.getBreakPoint();
		
		try {
			String pathBefore = pointBefore.getFullJavaFilePath();
			List<String> stringLinesBefore = lineMap.get(pathBefore);
			if(stringLinesBefore==null){
				stringLinesBefore = Files.readAllLines(Paths.get(pointBefore.getFullJavaFilePath()), StandardCharsets.ISO_8859_1);
				lineMap.put(pathBefore, stringLinesBefore);
			}
			String textBefore = stringLinesBefore.get(pointBefore.getLineNumber()-1);
			
			String pathAfter = pointAfter.getFullJavaFilePath();
			List<String> stringLinesAfter = lineMap.get(pathAfter); 
			if(stringLinesAfter==null){
				stringLinesAfter = Files.readAllLines(Paths.get(pointAfter.getFullJavaFilePath()), StandardCharsets.ISO_8859_1);
				lineMap.put(pathAfter, stringLinesAfter);
			}
			String textAfter = stringLinesAfter.get(pointAfter.getLineNumber()-1);
			
			if(textBefore.equals(textAfter)) {
				return 1;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		ASTNode nodeBefore = parseASTNodeInBreakpoint(itNodeBefore, pointBefore);
		ASTNode nodeAfter = parseASTNodeInBreakpoint(itNodeAfter, pointAfter);
		
		if(nodeBefore.getNodeType()==nodeAfter.getNodeType()){
			return 1;
		}
		else{
			return 0;			
		}
		
	}

	private ASTNode parseASTNodeInBreakpoint(IndexTreeNode itNodeBefore, BreakPoint pointBefore) {
		String compilationUnitName = itNodeBefore.getTraceNode().getDeclaringCompilationUnitName();
		
		CompilationUnit cu = JavaUtil.findCompiltionUnitBySourcePath(pointBefore.getFullJavaFilePath(), 
				compilationUnitName);
		ASTNode node = findSpecificNode(cu, pointBefore);
		
		return node;
	}

	class MinimumASTNodeFinder extends ASTVisitor{
		private int line;
		private CompilationUnit cu;
		
		private int startLine = -1;
		private int endLine = -1;
		
		ASTNode minimumNode = null;

		public MinimumASTNodeFinder(int line, CompilationUnit cu) {
			super();
			this.line = line;
			this.cu = cu;
		}

		@Override
		public void preVisit(ASTNode node) {
			
			int start = cu.getLineNumber(node.getStartPosition());
			int end = cu.getLineNumber(node.getStartPosition()+node.getLength());
			
			if(start<=line && line<=end){
				if(minimumNode == null){
					startLine = start;
					endLine = end;
					minimumNode = node;
				}
				else{
					boolean flag = false;
					
					if(startLine<start){
						startLine = start;
						flag = true;
					}
					
					if(endLine>end){
						endLine = end;
						flag = true;
					}
					
					if(flag){
						minimumNode = node;						
					}
				}
			}
		}
	}
	
	private ASTNode findSpecificNode(CompilationUnit cu, BreakPoint point) {
		MinimumASTNodeFinder finder = new MinimumASTNodeFinder(point.getLineNumber(), cu);
		cu.accept(finder);
		return finder.minimumNode;
	}

	private boolean isControlPathCompatible(IndexTreeNode itNodeBefore, IndexTreeNode itNodeAfter) {
		List<ControlNode> pathBefore = itNodeBefore.getControlPath();
		List<ControlNode> pathAfter = itNodeAfter.getControlPath();
		
		/**
		 * Here is an optimization:
		 * If parent control node has been recorded in pair list, we do not need to compare their control
		 * path any more. 
		 */
		MatchingGraphPair pair = null;
		int size = pathAfter.size();
		for(int i=size-1; i>=0; i--) {
			ControlNode cNode = pathAfter.get(i);
			MatchingGraphPair p = pairMap.get(cNode.getOrder());
			if(p!=null) {
				pair = p;
				break;
			}
		}
		
		int cursorBefore = 0;
		int cursorAfter = 0;
		if(pair!=null) {
			cursorBefore = ((IndexTreeNode)pair.getNodeBefore()).getOrder();
			cursorAfter = ((IndexTreeNode)pair.getNodeAfter()).getOrder();
		}
		
		
		for(ControlNode nodeBefore: pathBefore){
			if(nodeBefore.getOrder()<=cursorBefore) {
				continue;
			}
			
			if(nodeBefore.getAppearOrder() > 1){
				boolean flag = canFindMatchingNode(nodeBefore, pathAfter);
				if(!flag){
					return false;
				}
			}
		}
		
		for(ControlNode nodeAfter: pathAfter){
			if(nodeAfter.getOrder()<=cursorAfter) {
				continue;
			}
			
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
