package tregression.tracematch;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;

import microbat.algorithm.graphdiff.Matcher;
import microbat.algorithm.graphdiff.MatchingGraphPair;
import microbat.model.BreakPoint;
import microbat.model.value.GraphNode;
import microbat.util.JavaUtil;
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
			
			IndexTreeNode gNodeAfter = findMostSimilarNode(gNodeBefore, childrenAfter, pairList);
			
			MatchingGraphPair pair = new MatchingGraphPair(gNodeBefore, gNodeAfter);
			pairList.add(pair);
			
		}
		
		return pairList;
		
	}

	private IndexTreeNode findMostSimilarNode(GraphNode gNodeBefore, List<? extends GraphNode> childrenAfter, 
			List<MatchingGraphPair> pairList) {
		IndexTreeNode mostSimilarNode = null;
		double sim = -1;
		
		IndexTreeNode itNodeBefore = (IndexTreeNode)gNodeBefore;
		if(itNodeBefore.getTraceNode().getOrder()==253){
			System.currentTimeMillis();
		}
		
		for(GraphNode gNodeAfter: childrenAfter){
			IndexTreeNode itNodeAfter = (IndexTreeNode)gNodeAfter;
			
			if(itNodeAfter.getTraceNode().getOrder()==324){
				System.currentTimeMillis();
			}
			
			if(hasMatched(pairList, itNodeAfter)){
				continue;
			}
			
			if(diffMatcher.isMatch(itNodeBefore.getBreakPoint(), itNodeAfter.getBreakPoint())){
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
		}
		
		if(sim==0){
			mostSimilarNode = null;
		}
		
		return mostSimilarNode;
	}

	private boolean hasMatched(List<MatchingGraphPair> pairList, IndexTreeNode itNodeAfter) {
		for(MatchingGraphPair pair: pairList){
			if(pair.getNodeAfter()==itNodeAfter){
				return true;
			}
		}
		return false;
	}

	private double sim(IndexTreeNode itNodeBefore, IndexTreeNode itNodeAfter) {
		BreakPoint pointBefore = itNodeBefore.getBreakPoint();
		BreakPoint pointAfter = itNodeAfter.getBreakPoint();
		
		try {
			String textBefore = Files.readAllLines(Paths.get(pointBefore.getFullJavaFilePath()), StandardCharsets.ISO_8859_1)
					.get(pointBefore.getLineNumber());
			String textAfter = Files.readAllLines(Paths.get(pointAfter.getFullJavaFilePath()), StandardCharsets.ISO_8859_1)
					.get(pointAfter.getLineNumber());
			
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
