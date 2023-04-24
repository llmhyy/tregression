package tregression.empiricalstudy;

import java.util.HashMap;
import java.util.Map;

import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;

public class CausalityNode {
	private TraceNode node;
	private boolean isOnBefore;

	/**
	 * if var value is null, it means the causality is control
	 */
	private Map<CausalityNode, VarValue> results = new HashMap<>();
	private Map<CausalityNode, VarValue> reasons = new HashMap<>();

	public CausalityNode(TraceNode node, boolean isOnBefore) {
		super();
		this.node = node;
		this.isOnBefore = isOnBefore;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (isOnBefore ? 1231 : 1237);
		result = prime * result + ((node == null) ? 0 : node.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CausalityNode other = (CausalityNode) obj;
		if (isOnBefore != other.isOnBefore)
			return false;
		if (node == null) {
			if (other.node != null)
				return false;
		} else if (!node.equals(other.node))
			return false;
		return true;
	}
	
	@Override
	public String toString(){
		StringBuffer buffer = new StringBuffer();
		buffer.append(this.node.getOrder());
		buffer.append(", ");
		buffer.append(this.isOnBefore);
		
		return buffer.toString();
	}

	public Map<CausalityNode, VarValue> getResults() {
		return results;
	}

	public void setResults(Map<CausalityNode, VarValue> results) {
		this.results = results;
	}

	public Map<CausalityNode, VarValue> getReasons() {
		return reasons;
	}

	public void setReasons(Map<CausalityNode, VarValue> reasons) {
		this.reasons = reasons;
	}

	public TraceNode getNode() {
		return node;
	}

	public void setNode(TraceNode node) {
		this.node = node;
	}

	public boolean isOnBefore() {
		return isOnBefore;
	}

	public void setOnBefore(boolean isOnBefore) {
		this.isOnBefore = isOnBefore;
	}

}
