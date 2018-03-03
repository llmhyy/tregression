package tregression.tracematch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.CompilationUnit;

import microbat.codeanalysis.ast.LoopHeadParser;
import microbat.model.BreakPoint;
import microbat.model.trace.TraceNode;
import microbat.model.value.GraphNode;
import microbat.util.JavaUtil;

public class IndexTreeNode implements GraphNode {

	
	public IndexTreeNode(TraceNode node){
		this.node = node;
	}
	
	private TraceNode node;
	
	public String toString(){
		return node.toString();
	}
	
	public int getLineNumber(){
		return node.getLineNumber();
	}
	
	@Override
	public List<? extends GraphNode> getChildren() {
		List<TraceNode> invocationChildren = node.getInvocationChildren();
		List<IndexTreeNode> children = new ArrayList<>();
		for(TraceNode invocationChild: invocationChildren){
			IndexTreeNode child = new IndexTreeNode(invocationChild);
			children.add(child);
		}
		return children;
	}

	@Override
	public List<? extends GraphNode> getParents() {
		TraceNode invocationParent = node.getInvocationParent();
		
		List<IndexTreeNode> parents = new ArrayList<>();
		parents.add(new IndexTreeNode(invocationParent));
		
		return parents;
	}
	
	private IndexTreeNode getIndexParent(){
		TraceNode invocationParent = node.getInvocationParent();
		TraceNode controlDominator = node.getControlDominator();
		
		if(invocationParent==null && controlDominator==null){
			return null;
		}
		else if(invocationParent==null && controlDominator!=null){
			return new IndexTreeNode(controlDominator);
		}
		else if(invocationParent!=null && controlDominator==null){
			return new IndexTreeNode(invocationParent);
		}
		else{
			TraceNode largerNode = (invocationParent.getOrder() > controlDominator.getOrder())?
					invocationParent : controlDominator;
			return new IndexTreeNode(largerNode);
		}
	}
	
	private int order=-1;
	public int getOrder() {
		if(order==-1){
			this.order = this.node.getOrder();
		}
		return this.order;
	}
	
	private List<ControlNode> controlPath;
	public List<ControlNode> getControlPath(){
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
		
		List<ControlNode> controlNodeList = new ArrayList<>();
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
			
			ControlNode controlNode = new ControlNode(node, appearingTime);
			controlNodeList.add(controlNode);
		}
		
		this.controlPath = controlNodeList;
		return controlNodeList;
	}

	private boolean isLoopControlBy(IndexTreeNode parent) {
		BreakPoint p = parent.getBreakPoint();
		if(p.getLoopScope()==null){
			CompilationUnit cu = JavaUtil.findCompiltionUnitBySourcePath(p.getFullJavaFilePath(), p.getDeclaringCompilationUnitName());
			
			LoopHeadParser lhParser = new LoopHeadParser(cu, p);
			cu.accept(lhParser);
			
			p.setLoopScope(lhParser.extractScope());
		}
		
		
		return p.getLoopScope().containLocation(point);
	}

	private int calculateAppearingTime(List<ControlNode> controlNodeList, IndexTreeNode node) {
		
		int count = 0;
		for(ControlNode cNode: controlNodeList){
			if(cNode.getItNode().getBreakPoint().equals(node.getBreakPoint())){
				count++;
			}
		}
		
		return count+1;
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

}
