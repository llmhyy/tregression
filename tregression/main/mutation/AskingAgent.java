package mutation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import microbat.model.trace.TraceNode;

public class AskingAgent {
	private final List<TraceNode> executionList;
	private Set<Integer> visitedNodeOrder;
	
	private int startPointer;
	
	private static final int NAN = -1;
	
	public AskingAgent(List<TraceNode> executionList) {
		this.executionList = executionList;
		this.visitedNodeOrder = new HashSet<>();
		this.startPointer = 0;
	}
	
	public void addVisistedNodeOrder(final int order) {
		this.visitedNodeOrder.add(order);
	}
	
	public void addVisistedNode(TraceNode node) {
		this.addVisistedNodeOrder(node.getOrder());
	}
	
	/**
	 * When the baseline recommend a repeated node, this
	 * function return the new node to be asked for feedback
	 * 
	 * Strategy: If the node is not visited, return that node. If the
	 * node is visited, return the control dominator if exist. If the
	 * control dominator does not exist, return the next execution
	 * node. If there are no more execution node, then return -1
	 * 
	 * @param node Error Node
	 * @return Order of next asking node
	 */
	public int getNodeOrderToBeAsked(TraceNode node) {
		
		// Recommend this node if the node is not visited before
		if (!this.isVisitedNode(node)) {
			this.addVisistedNode(node);
			return node.getOrder();
		}

		// Check control dominator
		TraceNode controlDominator = node.getControlDominator();
		if (controlDominator != null) {
			if (!this.isVisitedNode(node)) {
				this.addVisistedNode(controlDominator);
				return controlDominator.getOrder();
			}
		}
		
		if (this.isAllNodeVisisted()) {
			return AskingAgent.NAN;
		}
		
		// Get the order of next node to be asked
		int nodeOrder = this.executionList.get(startPointer).getOrder();
		while(this.isVisitedOrder(nodeOrder)) {
			
			if (this.isAllNodeVisisted()) {
				return AskingAgent.NAN;
			}
			
			nodeOrder = this.executionList.get(this.startPointer).getOrder();
			this.startPointer++;
		}
		
		this.addVisistedNodeOrder(nodeOrder);
		return nodeOrder;
	}
	
	private boolean isAllNodeVisisted() {
		return this.startPointer >= this.executionList.size();
	}
	
	public boolean isVisitedNode(TraceNode node) {
		return this.isVisitedOrder(node.getOrder());
	}
	
	public boolean isVisitedOrder(final int order) {
		return this.visitedNodeOrder.contains(order);
	}
}
