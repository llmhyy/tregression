package tregression.empiricalstudy;

import microbat.model.value.VarValue;

public class MendingRecord {
	public static int CONTROL = 0;
	public static int DATA = 1;

	private int type;
	private int startOrder;

	private int correspondingStepOnReference;
	private int returningPoint;
	
	private VarValue varValue;

	public MendingRecord(int type, int startOrder, int correspondingStepOnReference, int returningPoint) {
		super();
		this.type = type;
		this.startOrder = startOrder;
		this.correspondingStepOnReference = correspondingStepOnReference;
		this.returningPoint = returningPoint;
	}
	
	public String getTypeString(){
		if(getType()==MendingRecord.DATA){
			return "data";
		}
		else if(getType()==MendingRecord.CONTROL){
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

	public int getStartOrder() {
		return startOrder;
	}

	public void setStartOrder(int startOrder) {
		this.startOrder = startOrder;
	}

	public int getCorrespondingStepOnReference() {
		return correspondingStepOnReference;
	}

	public void setCorrespondingStepOnReference(int correspondingStepOnReference) {
		this.correspondingStepOnReference = correspondingStepOnReference;
	}

	public int getReturningPoint() {
		return returningPoint;
	}

	public void setReturningPoint(int returningPoint) {
		this.returningPoint = returningPoint;
	}

	public VarValue getVarValue() {
		return varValue;
	}

	public void setVarValue(VarValue varValue) {
		this.varValue = varValue;
	}

}
