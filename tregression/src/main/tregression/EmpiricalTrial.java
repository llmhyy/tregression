package tregression;

import java.util.ArrayList;
import java.util.List;

import microbat.model.trace.TraceNode;

public class EmpiricalTrial {
	public static final int FIND_BUG = 0;
	public static final int OVER_SKIP = 1;

	private int bugType;
	private int overskipLength = 0;
	
	private List<TraceNode> checkList = new ArrayList<>();

	public EmpiricalTrial(int bugType, int overskipLength, List<TraceNode> checkList) {
		super();
		this.bugType = bugType;
		this.overskipLength = overskipLength;
		this.setCheckList(checkList);
	}
	
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		String type = (this.bugType==FIND_BUG) ? "bug_found" : "over_skip";
		buffer.append("trial type: " + type + "\n");
		buffer.append("over skip length: " + this.overskipLength + "\n");
		buffer.append("debugging trace: \n");
		for(TraceNode node: checkList) {
			buffer.append(node.toString() + "\n");
		}
		return buffer.toString();
	}

	public int getBugType() {
		return bugType;
	}

	public void setBugType(int bugType) {
		this.bugType = bugType;
	}

	public int getOverskipLength() {
		return overskipLength;
	}

	public void setOverskipLength(int overskipLength) {
		this.overskipLength = overskipLength;
	}

	public List<TraceNode> getCheckList() {
		return checkList;
	}

	public void setCheckList(List<TraceNode> checkList) {
		this.checkList = checkList;
	}

}
