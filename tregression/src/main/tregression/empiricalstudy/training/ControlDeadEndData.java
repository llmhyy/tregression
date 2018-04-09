package tregression.empiricalstudy.training;

public class ControlDeadEndData extends DeadEndData{
	public int moveUps;
	public int moveDowns;
	public int moveRights;
	
	public int dataDependency;
	public int controlDependency;
	
	public String getPlainText(String project, String bugID){
		StringBuffer buffer = new StringBuffer();
		
		buffer.append(project+",");
		buffer.append(bugID+",");
		buffer.append(String.valueOf(this.traceOrder)+",");
		buffer.append(String.valueOf(this.isBreakStep)+",");
		buffer.append(String.valueOf(this.moveUps)+",");
		buffer.append(String.valueOf(this.moveDowns)+",");
		buffer.append(String.valueOf(this.moveRights)+",");
		buffer.append(String.valueOf(this.dataDependency)+",");
		buffer.append(String.valueOf(this.controlDependency)+",");
		buffer.append(String.valueOf(this.deadEndLength)+",");
		
		String commonText = super.getPlainText(project, bugID);
		buffer.append(commonText);
		
		return buffer.toString();
	}
}
