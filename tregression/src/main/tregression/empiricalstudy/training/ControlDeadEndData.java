package tregression.empiricalstudy.training;

public class ControlDeadEndData extends DeadEndData{
	public int dataDependency;
	public int controlDependency;
	
	public String getPlainText(String project, String bugID){
		StringBuffer buffer = new StringBuffer();
		
		buffer.append(project+",");
		buffer.append(bugID+",");
		buffer.append(String.valueOf(this.traceOrder)+",");
		buffer.append(String.valueOf(this.isBreakStep)+",");
		
		String traverseText = super.fillTraverseInfomation();
		buffer.append(traverseText);
		
		buffer.append(String.valueOf(this.dataDependency)+",");
		buffer.append(String.valueOf(this.controlDependency)+",");
		buffer.append(String.valueOf(this.deadEndLength)+",");
		
		String commonText = super.fillCommonRowInfomation();
		buffer.append(commonText);
		
		return buffer.toString();
	}
}
