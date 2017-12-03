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
	
	private String testcase;
	
	private TraceNode rootcauseNode;
	private RootCauseNode realcauseNode;
	
	private int traceCollectionTime;
	private int traceMatchTime;
	private int simulationTime;
	
	private int buggyTraceLength;
	private int correctTranceLength;
	
	private List<TraceNode> visitedRegressionNodes;
	private List<TraceNode> visitedCorrectNodes;
	
	private int totalVisitedNodesNum;
	
	private List<StepOperationTuple> checkList = new ArrayList<>();
	
	private String exceptionExplanation;
	
	private RootCauseFinder rootCauseFinder;

	public EmpiricalTrial(int bugType, int overskipLength, TraceNode rootcauseNode, 
			RootCauseNode realcauseNode, List<StepOperationTuple> checkList, int traceCollectionTime,
			int traceMatchTime, int simulationTime, int buggyTraceLength, int correctTraceLength,
			RootCauseFinder rootCauseFinder) {
		super();
		this.bugType = bugType;
		this.overskipLength = overskipLength;
		this.rootcauseNode = rootcauseNode;
		this.checkList = checkList;
		this.realcauseNode = realcauseNode;
		this.setTraceCollectionTime(traceCollectionTime);
		this.setTraceMatchTime(traceMatchTime);
		this.setSimulationTime(simulationTime);
		this.buggyTraceLength = buggyTraceLength;
		this.correctTranceLength = correctTraceLength;
		this.setRootCauseFinder(rootCauseFinder);
		if(rootCauseFinder != null){
			this.visitedRegressionNodes = rootCauseFinder.getRegressionNodeList();
			this.visitedCorrectNodes = rootCauseFinder.getCorrectNodeList();
			this.totalVisitedNodesNum = rootCauseFinder.getRegressionNodeList().size()+rootCauseFinder.getCorrectNodeList().size();			
		}
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
		buffer.append("found root cause: " + rootcauseOrder + "\n");
		String realcauseOrder = (this.realcauseNode==null)? "-1" : this.realcauseNode.toString();
		buffer.append("real root cause: " + realcauseOrder + "\n");
		buffer.append("over skip length: " + this.overskipLength + "\n");
		buffer.append("explanation size: " + this.totalVisitedNodesNum + "\n");
		buffer.append("debugging trace: \n");
		if(checkList!=null) {
			for(StepOperationTuple tuple: checkList) {
				buffer.append(tuple.toString() + "\n");
			}
		}
		if(this.rootCauseFinder!=null){
			for(MendingRecord record: this.rootCauseFinder.getMendingRecordList()){
				buffer.append("mending type: ");
				String mendingType = (record.getType()==MendingRecord.CONTROL)? "control" : "data";
				buffer.append(mendingType + "\n");
				
				buffer.append("mending start at: ");
				buffer.append(record.getStartOrder() + "\n");
				
				buffer.append("mending corresponding point at: ");
				buffer.append(record.getCorrespondingStepOnReference() + "\n");
				
				buffer.append("mending returning point at: ");
				buffer.append(record.getReturningPoint() + "\n");
			}
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

	public RootCauseNode getRealcauseNode() {
		return realcauseNode;
	}

	public void setRealcauseNode(RootCauseNode realcauseNode) {
		this.realcauseNode = realcauseNode;
	}

	public int getTraceCollectionTime() {
		return traceCollectionTime;
	}

	public void setTraceCollectionTime(int traceCollectionTime) {
		this.traceCollectionTime = traceCollectionTime;
	}

	public int getTraceMatchTime() {
		return traceMatchTime;
	}

	public void setTraceMatchTime(int traceMatchTime) {
		this.traceMatchTime = traceMatchTime;
	}

	public int getSimulationTime() {
		return simulationTime;
	}

	public void setSimulationTime(int simulationTime) {
		this.simulationTime = simulationTime;
	}

	public int getBuggyTraceLength() {
		return buggyTraceLength;
	}

	public void setBuggyTraceLength(int buggyTraceLength) {
		this.buggyTraceLength = buggyTraceLength;
	}

	public int getCorrectTranceLength() {
		return correctTranceLength;
	}

	public void setCorrectTranceLength(int correctTranceLength) {
		this.correctTranceLength = correctTranceLength;
	}

	public String getTestcase() {
		return testcase;
	}

	public void setTestcase(String testcase) {
		this.testcase = testcase;
	}

	public List<TraceNode> getVisitedRegressionNodes() {
		return visitedRegressionNodes;
	}

	public void setVisitedRegressionNodes(List<TraceNode> visitedRegressionNodes) {
		this.visitedRegressionNodes = visitedRegressionNodes;
	}

	public List<TraceNode> getVisitedCorrectNodes() {
		return visitedCorrectNodes;
	}

	public void setVisitedCorrectNodes(List<TraceNode> visitedCorrectNodes) {
		this.visitedCorrectNodes = visitedCorrectNodes;
	}

	public int getTotalVisitedNodesNum() {
		return totalVisitedNodesNum;
	}

	public void setTotalVisitedNodesNum(int totalVisitedNodesNum) {
		this.totalVisitedNodesNum = totalVisitedNodesNum;
	}

	public String getExceptionExplanation() {
		return exceptionExplanation;
	}

	public void setExceptionExplanation(String exceptionExplanation) {
		this.exceptionExplanation = exceptionExplanation;
	}

	public RootCauseFinder getRootCauseFinder() {
		return rootCauseFinder;
	}

	public void setRootCauseFinder(RootCauseFinder rootCauseFinder) {
		this.rootCauseFinder = rootCauseFinder;
	}

}
