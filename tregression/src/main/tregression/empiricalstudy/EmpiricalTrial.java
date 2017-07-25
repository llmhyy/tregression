package tregression.empiricalstudy;

import java.util.ArrayList;
import java.util.List;

import microbat.model.trace.TraceNode;
import tregression.model.StepOperationTuple;

public class EmpiricalTrial {
	public static final int FIND_BUG = 0;
	public static final int OVER_SKIP = 1;

	private int bugType;
	private int overskipLength = 0;
	
	private TraceNode rootcauseNode;
	
	private List<StepOperationTuple> checkList = new ArrayList<>();

	public EmpiricalTrial(int bugType, int overskipLength, TraceNode rootcauseNode, List<StepOperationTuple> checkList) {
		super();
		this.bugType = bugType;
		this.overskipLength = overskipLength;
		this.rootcauseNode = rootcauseNode;
		this.checkList = checkList;
	}
	
	public static String getTypeStringName(int t) {
		if(t==FIND_BUG) {
			return "find_bug";
		}
		else if(t==OVER_SKIP) {
			return "over_skip";
		}
		
		return null;
	}
	
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		String type = (this.bugType==FIND_BUG) ? "bug_found" : "over_skip";
		buffer.append("trial type: " + type + "\n");
		int rootcauseOrder = (this.rootcauseNode==null)? -1 : this.rootcauseNode.getOrder();
		buffer.append("root cause: " + rootcauseOrder + "\n");
		buffer.append("over skip length: " + this.overskipLength + "\n");
		buffer.append("debugging trace: \n");
		for(StepOperationTuple tuple: checkList) {
			buffer.append(tuple.toString() + "\n");
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

	public List<StepOperationTuple> getCheckList() {
		return checkList;
	}

	public void setCheckList(List<StepOperationTuple> checkList) {
		this.checkList = checkList;
	}
	
	public TraceNode getRootcauseNode() {
		return this.rootcauseNode;
	}

}
