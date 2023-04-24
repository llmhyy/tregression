package tregression.empiricalstudy;

import microbat.model.trace.Trace;
import microbat.model.value.VarValue;
import tregression.empiricalstudy.solutionpattern.SolutionPattern;
import tregression.empiricalstudy.training.DED;
import tregression.empiricalstudy.training.TrainingDataTransfer;

public class DeadEndRecord {
	public static int CONTROL = 0;
	public static int DATA = 1;

	private int type;
	private int occurOrder;
	private int deadEndOrder;

	private int correspondingStepOnReference;
	private int breakStepOrder;
	
	private VarValue varValue;
	
	private SolutionPattern solutionPattern;

	public DeadEndRecord(int type, int occurOrder, int deadEndOrder, 
			int correspondingStepOnReference, int breakStepOrder) {
		super();
		this.type = type;
		this.occurOrder = occurOrder;
		this.deadEndOrder = deadEndOrder;
		this.correspondingStepOnReference = correspondingStepOnReference;
		this.breakStepOrder = breakStepOrder;
	}
	
	@Override
	public String toString(){
		StringBuffer buffer = new StringBuffer();
		buffer.append("dead end type: ");
		String mendingType = (getType()==DeadEndRecord.CONTROL)? "control" : "data";
		buffer.append(mendingType + "\n");
		
		buffer.append("occur: ");
		buffer.append(getOccurOrder() + "\n");
		if(getType()==DeadEndRecord.DATA) {
			buffer.append("occur var:");
			buffer.append(getVarValue().getVarName());
			buffer.append("\n");
		}
		
		buffer.append("dead end: ");
		buffer.append(getDeadEndOrder() + "\n");
		
		buffer.append("break step: ");
		buffer.append(getBreakStepOrder() + "\n");
		
		buffer.append("solution type: ");
		buffer.append(solutionPattern + "\n");
		
		return buffer.toString();
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + correspondingStepOnReference;
		result = prime * result + breakStepOrder;
		result = prime * result + occurOrder;
		result = prime * result + type;
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
		DeadEndRecord other = (DeadEndRecord) obj;
		if (correspondingStepOnReference != other.correspondingStepOnReference)
			return false;
		if (breakStepOrder != other.breakStepOrder)
			return false;
		if (occurOrder != other.occurOrder)
			return false;
		if (type != other.type)
			return false;
		return true;
	}
	
	public String getTypeString(){
		if(getType()==DeadEndRecord.DATA){
			return "data";
		}
		else if(getType()==DeadEndRecord.CONTROL){
			return "control";
		}
		return "null";
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getOccurOrder() {
		return occurOrder;
	}

	public void setOccurOrder(int startOrder) {
		this.occurOrder = startOrder;
	}

	public int getCorrespondingStepOnReference() {
		return correspondingStepOnReference;
	}

	public void setCorrespondingStepOnReference(int correspondingStepOnReference) {
		this.correspondingStepOnReference = correspondingStepOnReference;
	}

	public int getBreakStepOrder() {
		return breakStepOrder;
	}

	public void setBreakStepOrder(int breakStepOrder) {
		this.breakStepOrder = breakStepOrder;
	}

	public VarValue getVarValue() {
		return varValue;
	}

	public void setVarValue(VarValue varValue) {
		this.varValue = varValue;
	}

	public int getDeadEndOrder() {
		return deadEndOrder;
	}

	public void setDeadEndOrder(int deadEndOrder) {
		this.deadEndOrder = deadEndOrder;
	}

	public SolutionPattern getSolutionPattern() {
		return solutionPattern;
	}

	public void setSolutionPattern(SolutionPattern solutionPattern) {
		this.solutionPattern = solutionPattern;
	}

	public DED getTransformedData(Trace trace) {
		if(transformedData!=null){
			return transformedData;			
		}
		else{
			transformedData = new TrainingDataTransfer().transfer(this, trace);
			return transformedData;
		}
	}

	public void setTransformedData(DED transformedData) {
		this.transformedData = transformedData;
	}

	private DED transformedData;
	
}
