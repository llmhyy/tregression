package tregression.empiricalstudy;

import microbat.model.trace.TraceNode;

public class RootCauseNode {

	private TraceNode root;
	private boolean isOnBefore;

	public RootCauseNode(TraceNode root, boolean isOnBefore) {
		super();
		this.root = root;
		this.isOnBefore = isOnBefore;
	}

	public TraceNode getRoot() {
		return root;
	}

	public void setRoot(TraceNode root) {
		this.root = root;
	}

	public boolean isOnBefore() {
		return isOnBefore;
	}

	public void setOnBefore(boolean isOnBefore) {
		this.isOnBefore = isOnBefore;
	}
	
	@Override
	public String toString(){
		StringBuffer buffer = new StringBuffer();
		String trace = isOnBefore?"buggy":"correct";
		buffer.append("On " + trace + " trace, order: ");
		buffer.append(root.getOrder());
		return buffer.toString();
	}

}
