package tregression.tracematch;

import microbat.model.BreakPoint;
import tregression.separatesnapshots.DiffMatcher;

public class ControlNode {
	private IndexTreeNode itNode;
	private int appearOrder;

	public ControlNode(IndexTreeNode itNode, int appearOrder) {
		super();
		this.itNode = itNode;
		this.appearOrder = appearOrder;
	}
	
	@Override
	public String toString() {
		int order = itNode.getTraceNode().getOrder();
		String file = itNode.getBreakPoint().getClassCanonicalName();
		file = file.substring(file.lastIndexOf("."), file.length());
		int lineNumber = itNode.getBreakPoint().getLineNumber();
		
		StringBuffer buffer = new StringBuffer();
		buffer.append("[");
		buffer.append("file: " + file + ", ");
		buffer.append("line: " + lineNumber + ", ");
		buffer.append("time: " + appearOrder + ", ");
		buffer.append("node order: " + order + "]");
		
		return buffer.toString();
	}
	
	public boolean isMatchableWith(ControlNode thatNode, DiffMatcher diffMatcher){
		BreakPoint thisPoint = itNode.getBreakPoint();
		BreakPoint thatPoint = thatNode.getItNode().getBreakPoint();
		
		if(diffMatcher.isMatch(thisPoint, thatPoint) && 
				diffMatcher.isMatch(thatPoint, thisPoint)){
			if(this.appearOrder==thatNode.getAppearOrder()){
				return true;
			}
		}
		
		return false;
	}

	public IndexTreeNode getItNode() {
		return itNode;
	}

	public void setItNode(IndexTreeNode itNode) {
		this.itNode = itNode;
	}

	public int getAppearOrder() {
		return appearOrder;
	}

	public void setAppearOrder(int appearOrder) {
		this.appearOrder = appearOrder;
	}
	
	
	
}
