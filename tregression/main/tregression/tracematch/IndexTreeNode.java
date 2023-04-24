package tregression.tracematch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.CompilationUnit;

import microbat.codeanalysis.ast.LoopHeadParser;
import microbat.model.BreakPoint;
import microbat.model.trace.TraceNode;
import microbat.model.value.GraphNode;
import microbat.util.JavaUtil;
import tregression.separatesnapshots.DiffMatcher;

public class IndexTreeNode implements GraphNode {

	/**
	 * IndexTreeNode is a wrapper for TraceNode. However, it cannot wrap relations of a TraceNode,
	 * e.g., invoke parent, loop parent, ..., etc. Therefore, if we need to get the invoke/loop
	 * parent of an IndexTreeNode, we may need to link back to its TraceNode. 
	 * 
	 * The problem is when we get the invoke parent of a TraceNode, how do we know its IndexTreeNode?
	 * A naive way is to create a new IndexTreeNode object, which wastes a great amout of memory. 
	 * 
	 * To this end, we keep a global linkMap to track a TraceNode to its IndexTreeNode to save the memory.
	 */
	private Map<TraceNode, IndexTreeNode> linkMap;
	
	private int appearOrder = -1;
	
	public IndexTreeNode(TraceNode node, Map<TraceNode, IndexTreeNode> map){
		this.node = node;
		this.linkMap = map;
	}
	
	@Override
	public String toString() {
		int order = node.getOrder();
		String file = node.getBreakPoint().getClassCanonicalName();
		file = file.substring(file.lastIndexOf(".")+1, file.length());
		int lineNumber = node.getBreakPoint().getLineNumber();
		
		StringBuffer buffer = new StringBuffer();
		buffer.append("[");
		buffer.append("file: " + file + ", ");
		buffer.append("line: " + lineNumber + ", ");
		buffer.append("time: " + appearOrder + ", ");
		buffer.append("node order: " + order + "]");
		
		return buffer.toString();
	}
	
	public boolean isMatchableWith(IndexTreeNode thatNode, DiffMatcher diffMatcher){
		BreakPoint thisPoint = node.getBreakPoint();
		BreakPoint thatPoint = thatNode.getBreakPoint();
		
		if(this.appearOrder==thatNode.getAppearOrder()){
			if(diffMatcher.isMatch(thisPoint, thatPoint)){
				return true;				
			}
		}
		
		return false;
	}
	
	public IndexTreeNode fetchIndexTreeNode(TraceNode traceNode){
		IndexTreeNode iNode = linkMap.get(traceNode);
		if(iNode == null){
			iNode = new IndexTreeNode(traceNode, linkMap);
			linkMap.put(traceNode, iNode);
		}
		
		return iNode;
	}
	
	private TraceNode node;
	
	
	public int getLineNumber(){
		return node.getLineNumber();
	}
	
	@Override
	public List<? extends GraphNode> getChildren() {
//		List<TraceNode> invocationChildren = node.getInvocationChildren();
		List<TraceNode> invocationChildren = node.getAbstractChildren();
		List<IndexTreeNode> children = new ArrayList<>();
		for(TraceNode invocationChild: invocationChildren){
			IndexTreeNode child = fetchIndexTreeNode(invocationChild);
			children.add(child);
		}
		return children;
	}

	@Override
	public List<? extends GraphNode> getParents() {
//		TraceNode invocationParent = node.getInvocationParent();
		TraceNode invocationParent = node.getAbstractionParent();
		
		List<IndexTreeNode> parents = new ArrayList<>();
		parents.add(fetchIndexTreeNode(invocationParent));
		
		return parents;
	}
	
	private IndexTreeNode getIndexParent(){
		
		TraceNode invocationParent = node.getInvocationParent();
		TraceNode controlDominator = node.getControlDominator();
		
		IndexTreeNode parent = null;
		if(invocationParent==null && controlDominator==null){
			return null;
		}
		else if(invocationParent==null && controlDominator!=null){
			parent = fetchIndexTreeNode(controlDominator);
		}
		else if(invocationParent!=null && controlDominator==null){
			parent = fetchIndexTreeNode(invocationParent);
		}
		else{
			TraceNode largerNode = (invocationParent.getOrder() > controlDominator.getOrder())?
					invocationParent : controlDominator;
			parent = fetchIndexTreeNode(largerNode);
		}
		
		return parent;
	}
	
	private int order=-1;
	public int getOrder() {
		if(order==-1){
			this.order = this.node.getOrder();
		}
		return this.order;
	}
	
	private List<IndexTreeNode> controlPath;
	public List<IndexTreeNode> getControlPath(){
		if(controlPath!=null){
			return controlPath;
		}
		
		List<IndexTreeNode> path = new ArrayList<>();
		IndexTreeNode parent = this.getIndexParent();
		while(parent != null && hasSameInvocationParent(this, parent)){
			if(isLoopControlBy(parent)){
				path.add(parent);				
			}
			parent = parent.getIndexParent();
		}
		
		List<IndexTreeNode> controlNodeList = new ArrayList<>();
		Map<BreakPoint, Integer> map = new HashMap<>();
		for(int i=path.size()-1; i>=0; i--){
			IndexTreeNode node = path.get(i);
			BreakPoint point = node.getBreakPoint();
			
			Integer appearingTime = map.get(point);
			if(appearingTime==null){
				appearingTime = 0;
			}
			appearingTime++;
			map.put(point, appearingTime);
			//int appearingTime = calculateAppearingTime(controlNodeList, node);
			
			node.setAppearOrder(appearingTime);
			if(appearingTime>1){
				controlNodeList.add(node);				
			}
		}
		
		this.controlPath = controlNodeList;
		return controlNodeList;
	}

	private boolean isLoopControlBy(IndexTreeNode parent) {
		BreakPoint p = parent.getBreakPoint();
//		p.setLoopScope(null);
		if(p.getLoopScope()==null){
			CompilationUnit cu = JavaUtil.findCompiltionUnitBySourcePath(p.getFullJavaFilePath(), p.getDeclaringCompilationUnitName());
			
			LoopHeadParser lhParser = new LoopHeadParser(cu, p);
			cu.accept(lhParser);
			
			p.setLoopScope(lhParser.extractScope());
			
		}
		
		
		return p.getLoopScope().isLoop() && p.getLoopScope().containLocation(point);
	}

	private boolean hasSameInvocationParent(IndexTreeNode indexTreeNode, IndexTreeNode parent) {
		TraceNode invocationP1 = indexTreeNode.getTraceNode().getInvocationParent();
		TraceNode invocationP2 = parent.getTraceNode().getInvocationParent();
		
		if(invocationP1==null && invocationP2==null){
			return true;
		}
		else if(invocationP1==null && invocationP2!=null){
			return false;
		}
		else if(invocationP1!=null && invocationP2==null){
			return false;
		}
		else{
			return invocationP1.getOrder()==invocationP2.getOrder();
		}
		
	}

	@Override
	public boolean match(GraphNode node) {
		return false;
	}

	@Override
	public boolean isTheSameWith(GraphNode node) {
//		if(node instanceof IndexTreeNode){
//			IndexTreeNode thatNode = (IndexTreeNode)node;
//			TraceNodeVariableSimilarityComparator comparator = new TraceNodeVariableSimilarityComparator();
//			
//			double sim = comparator.compute(this.node, thatNode.getTraceNode());
//			
//			return sim==1;
//		}
//		return false;
		return true;
	}
	
	private BreakPoint point;
	public BreakPoint getBreakPoint(){
		if(point==null){
			point = node.getBreakPoint();
		}
		return point;
	}

	public TraceNode getTraceNode() {
		return node;
	}

	public void setTraceNode(TraceNode node) {
		this.node = node;
	}

	public Map<TraceNode, IndexTreeNode> getLinkMap() {
		return linkMap;
	}

	public void setLinkMap(Map<TraceNode, IndexTreeNode> linkMap) {
		this.linkMap = linkMap;
	}

	public int getAppearOrder() {
		return appearOrder;
	}

	public void setAppearOrder(int appearOrder) {
		this.appearOrder = appearOrder;
	}

}
