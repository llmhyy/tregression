package tregression;

import java.util.List;

import microbat.model.value.VarValue;

public class StepChangeType {
	public static int IDT = 0;
	public static int SRC = 1;
	public static int DAT = 2;
	public static int CTL = 3;
	
	private int type;
	private List<VarValue> wrongVariableList;
	
	public StepChangeType(int type) {
		super();
		this.type = type;
	}

	public StepChangeType(int type, List<VarValue> wrongVariableList) {
		super();
		this.type = type;
		this.wrongVariableList = wrongVariableList;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public List<VarValue> getWrongVariableList() {
		return wrongVariableList;
	}

	public void setWrongVariableList(List<VarValue> wrongVariableList) {
		this.wrongVariableList = wrongVariableList;
	}

}
