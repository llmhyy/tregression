package baseline;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import microbat.model.trace.TraceNode;

public class AskingAgent {
	private final List<TraceNode> executionList;
	private Set<Integer> visitedNodeOrder;
	
	private int startPointer;
	
	public AskingAgent(List<TraceNode> executionList) {
		this.executionList = executionList;
		this.visitedNodeOrder = new HashSet<>();
		this.startPointer = 0;
	}
	
	public void addVisistedNodeOrder(final int order) {
		this.visitedNodeOrder.add(order);
	}
	
	public int getNodeOrderToBeAsked() {
		int nodeOrder = this.executionList.get(startPointer).getOrder();
		while(this.visitedNodeOrder.contains(nodeOrder)) {
			nodeOrder = this.executionList.get(++this.startPointer).getOrder();
		}
		return nodeOrder;
	}
	
	public boolean isVisitedNode(TraceNode node) {
		return this.visitedNodeOrder.contains(node.getOrder());
	}
}
