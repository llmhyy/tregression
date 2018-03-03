package tregression.empiricalstudy.training;

public class DataDeadEndData extends DeadEndData{
	public static final int LOCAL_VAR = 1;
	public static final int FIELD = 2;
	public static final int ARRAY_ELEMENT = 3;
	
	public int criticalConditionalStep;
	
	public int sameWLocalVarType;
	public int sameWLocalVarName;
	public int sameWFieldParent;
	public int sameWFieldParentType;
	public int sameWFieldType;
	public int sameWFieldName;
	public int sameWArrayParent;
	public int sameWArrayType;
	public int sameWArrayIndex;
	
	public int sameRLocalVarType;
	public int sameRLocalVarName;
	public int sameRFieldParent;
	public int sameRFieldParentType;
	public int sameRFieldType;
	public int sameRFieldName;
	public int sameRArrayParent;
	public int sameRArrayType;
	public int sameRArrayIndex;
	
	public int type;
}
