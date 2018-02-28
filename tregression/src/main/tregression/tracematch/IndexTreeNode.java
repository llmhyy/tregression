package tregression.tracematch;

import java.util.ArrayList;
import java.util.List;

import microbat.model.BreakPoint;
import microbat.model.trace.TraceNode;
import microbat.model.value.GraphNode;

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
	
	public int getOrder() {
		return this.node.getOrder();
	}
	
	public List<ControlNode> getControlPath(){
		List<IndexTreeNode> path = new ArrayList<>();
		IndexTreeNode parent = this.getIndexParent();
		
		while(parent != null && hasSameInvocationParent(this, parent)){
			path.add(parent);
			parent = parent.getIndexParent();
		}
		
		List<ControlNode> controlNodeList = new ArrayList<>();
		for(int i=path.size()-1; i>=0; i--){
			IndexTreeNode node = path.get(i);
			int appearingTime = calculateAppearingTime(controlNodeList, node);
			
			ControlNode controlNode = new ControlNode(node, appearingTime);
			controlNodeList.add(controlNode);
		}
		return controlNodeList;
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
	
	public BreakPoint getBreakPoint(){
		return node.getBreakPoint();
	}

	public TraceNode getTraceNode() {
		return node;
	}

	public void setTraceNode(TraceNode node) {
		this.node = node;
	}

}
