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
	
	public String getPlainText(String project, String bugID){
		StringBuffer buffer = new StringBuffer();
		
		buffer.append(project+",");
		buffer.append(bugID+",");
		buffer.append(String.valueOf(this.traceOrder)+",");
		buffer.append(String.valueOf(this.isBreakStep)+",");
		buffer.append(String.valueOf(this.criticalConditionalStep)+",");
		buffer.append(String.valueOf(this.sameWArrayParent)+",");
		buffer.append(String.valueOf(this.sameWArrayType)+",");
		buffer.append(String.valueOf(this.sameWArrayIndex)+",");
		buffer.append(String.valueOf(this.sameRArrayParent)+",");
		buffer.append(String.valueOf(this.sameRArrayType)+",");
		buffer.append(String.valueOf(this.sameRArrayIndex)+",");
		buffer.append(String.valueOf(this.deadEndLength)+",");
		
		String commonText = super.getPlainText(project, bugID);
		buffer.append(commonText);
		
		return buffer.toString();
	}
}
