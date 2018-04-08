package tregression.empiricalstudy;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import tregression.empiricalstudy.training.ControlDeadEndData;
import tregression.empiricalstudy.training.DataDeadEndData;
import tregression.empiricalstudy.training.DeadEndData;

public class DeadEndCSVWriter {
	public static String LOCAL_DATA = "local_var";
	public static String FIELD = "field";
	public static String ARRAY = "array";
	public static String CONTROL = "control";
	
	public void export(List<DeadEndData> dataList, String project, String bugID) throws IOException{
		String fileName = null;
		
		DeadEndData data = dataList.get(0);
		
		if(data instanceof DataDeadEndData){
			DataDeadEndData ddd = (DataDeadEndData)data;
			if(ddd.type==DataDeadEndData.LOCAL_VAR){
				fileName = LOCAL_DATA + ".csv";
				appendLocalVarData(fileName, dataList, project, bugID);
			}
			else if(ddd.type==DataDeadEndData.FIELD){
				fileName = FIELD + ".csv";
				appendFieldData(fileName, dataList, project, bugID);
			}
			else if(ddd.type==DataDeadEndData.ARRAY_ELEMENT){
				fileName = ARRAY + ".csv";
				appendArrayElementData(fileName, dataList, project, bugID);
			}
		}
		else if(data instanceof ControlDeadEndData){
			fileName = CONTROL + ".csv";
			appendControlData(fileName, dataList, project, bugID);
		}
	}

	private void appendControlData(String fileName, List<DeadEndData> dataList, String project, String bugID) throws IOException{
		File file = new File(fileName);
		if(!file.exists()){
			file.createNewFile();
		}
		
		FileWriter writer = new FileWriter(fileName, true);
		for(DeadEndData data0: dataList){
			ControlDeadEndData dData = (ControlDeadEndData)data0;
			writer.append(project+",");
			writer.append(bugID+",");
			writer.append(String.valueOf(dData.traceOrder)+",");
			writer.append(String.valueOf(dData.isBreakStep)+",");
			writer.append(String.valueOf(dData.moveUps)+",");
			writer.append(String.valueOf(dData.moveDowns)+",");
			writer.append(String.valueOf(dData.moveRights)+",");
			writer.append(String.valueOf(dData.dataDependency)+",");
			writer.append(String.valueOf(dData.controlDependency)+",");
			writer.append(String.valueOf(dData.deadEndLength)+",");
			fillCommonRowInfomation(writer, dData);
		}
		
		writer.flush();
		writer.close();	
	}

	private void appendArrayElementData(String fileName, List<DeadEndData> dataList, String project, String bugID) throws IOException{
		File file = new File(fileName);
		if(!file.exists()){
			file.createNewFile();
		}
		
		FileWriter writer = new FileWriter(fileName, true);
		for(DeadEndData data0: dataList){
			DataDeadEndData dData = (DataDeadEndData)data0;
			writer.append(project+",");
			writer.append(bugID+",");
			writer.append(String.valueOf(dData.traceOrder)+",");
			writer.append(String.valueOf(dData.isBreakStep)+",");
			writer.append(String.valueOf(dData.criticalConditionalStep)+",");
			writer.append(String.valueOf(dData.sameWArrayParent)+",");
			writer.append(String.valueOf(dData.sameWArrayType)+",");
			writer.append(String.valueOf(dData.sameWArrayIndex)+",");
			writer.append(String.valueOf(dData.sameRArrayParent)+",");
			writer.append(String.valueOf(dData.sameRArrayType)+",");
			writer.append(String.valueOf(dData.sameRArrayIndex)+",");
			writer.append(String.valueOf(dData.deadEndLength)+",");
			fillCommonRowInfomation(writer, dData);
		}
		
		writer.flush();
		writer.close();	
	}

	private void appendFieldData(String fileName, List<DeadEndData> dataList, String project, String bugID) throws IOException {
		File file = new File(fileName);
		if(!file.exists()){
			file.createNewFile();
		}
		
		FileWriter writer = new FileWriter(fileName, true);
		for(DeadEndData data0: dataList){
			DataDeadEndData dData = (DataDeadEndData)data0;
			writer.append(project+",");
			writer.append(bugID+",");
			writer.append(String.valueOf(dData.traceOrder)+",");
			writer.append(String.valueOf(dData.isBreakStep)+",");
			writer.append(String.valueOf(dData.criticalConditionalStep)+",");
			writer.append(String.valueOf(dData.sameWFieldParent)+",");
			writer.append(String.valueOf(dData.sameWFieldParentType)+",");
			writer.append(String.valueOf(dData.sameWFieldType)+",");
			writer.append(String.valueOf(dData.sameWFieldName)+",");
			writer.append(String.valueOf(dData.sameRFieldParent)+",");
			writer.append(String.valueOf(dData.sameRFieldParentType)+",");
			writer.append(String.valueOf(dData.sameRFieldType)+",");
			writer.append(String.valueOf(dData.sameRFieldName)+",");
			writer.append(String.valueOf(dData.deadEndLength)+",");
			fillCommonRowInfomation(writer, dData);
		}
		
		writer.flush();
		writer.close();	
	}

	private void appendLocalVarData(String fileName, List<DeadEndData> dataList, String project, String bugID) throws IOException {
		File file = new File(fileName);
		if(!file.exists()){
			file.createNewFile();
		}
		
		FileWriter writer = new FileWriter(fileName, true);
		for(DeadEndData data0: dataList){
			DataDeadEndData dData = (DataDeadEndData)data0;
			writer.append(project+",");
			writer.append(bugID+",");
			writer.append(String.valueOf(dData.traceOrder)+",");
			writer.append(String.valueOf(dData.isBreakStep)+",");
			writer.append(String.valueOf(dData.criticalConditionalStep)+",");
			writer.append(String.valueOf(dData.sameWLocalVarType)+",");
			writer.append(String.valueOf(dData.sameWLocalVarName)+",");
			writer.append(String.valueOf(dData.sameRLocalVarType)+",");
			writer.append(String.valueOf(dData.sameRLocalVarName)+",");
			writer.append(String.valueOf(dData.deadEndLength)+",");
			writer.append(String.valueOf(dData.deadEndLength)+",");
			fillCommonRowInfomation(writer, dData);
		}
		
		writer.flush();
		writer.close();	
	}
	
	private void fillCommonRowInfomation(FileWriter writer, DeadEndData data) throws IOException{
		for(int value: data.stepAST){
			writer.append(String.valueOf(value)+",");
		}
		
		for(int value: data.stepContextAST){
			writer.append(String.valueOf(value)+",");
		}
		
		for(int value: data.occurStepAST){
			writer.append(String.valueOf(value)+",");
		}
		
		for(int value: data.occurStepContextAST){
			writer.append(String.valueOf(value)+",");
		}
		
		for(int value: data.deadEndStepAST){
			writer.append(String.valueOf(value)+",");
		}
		
		for(int i=0; i<data.deadEndStepContextAST.length; i++) {
			int value = data.deadEndStepContextAST[i];
			if(i != data.deadEndStepContextAST.length-1) {
				writer.append(String.valueOf(value)+",");				
			}
			else {
				writer.append(String.valueOf(value));
			}
		}
		
		writer.append("\n");
	}
	
}
