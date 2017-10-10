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
import microbat.model.trace.TraceNode;
import microbat.model.value.GraphNode;
import microbat.util.JavaUtil;
import tregression.separatesnapshots.DiffMatcher;

public class IndexTreeMatcher0 implements Matcher {
	/**
	 * DiffMatcher contains the information of how source code should match with each other.
	 * If this field is null, we assume there is only one-line modification between original
	 * and regression version.
	 */
	private DiffMatcher diffMatcher;
	
	private Map<Integer, MatchingGraphPair> pairMap = new HashMap<>();
	private Map<String, List<String>> lineMap = new HashMap<>();
	
	public IndexTreeMatcher0(DiffMatcher diffMatcher) {
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
			if(loopParent==null){
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
		List<MatchingGraphPair> pairs = new ArrayList<>();
		for(IndexTreeNode nodeBefore: treeBefore){
			IndexTreeNode matchedNodeAfter = findMostSimilarNode(nodeBefore, treeAfter, null);
			if(null != matchedNodeAfter){
				MatchingGraphPair pair = new MatchingGraphPair(nodeBefore, matchedNodeAfter);
				pairs.add(pair);
				pairMap.put(matchedNodeAfter.getOrder(), pair);
			}
		}
		
		for(MatchingGraphPair pair: pairs){
			List<IndexTreeNode> childrenBefore = getLoopChildren(pair.getNodeBefore());
			List<IndexTreeNode> childrenAfter = getLoopChildren(pair.getNodeAfter());
			
			matchIndexTree(childrenBefore, childrenAfter, pairList);
		}
		
	}

	private List<IndexTreeNode> getLoopChildren(GraphNode node) {
		List<IndexTreeNode> list = new ArrayList<>();
		IndexTreeNode itNode = (IndexTreeNode)node;
		for(TraceNode traceNode: itNode.getTraceNode().getLoopChildren()){
			IndexTreeNode indexNode = new IndexTreeNode(traceNode);
			list.add(indexNode);
		}
		return list;
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
}
