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
	private String csvFolder = "";
	
	private String fileSuffix = "";
	
	public DeadEndCSVWriter(String fileSuffix, Object o) {
		this.fileSuffix = fileSuffix;
	}
	
	public DeadEndCSVWriter(String csvFolder) {
		this.csvFolder = csvFolder;
	}
	
	public void export(List<DeadEndData> dataList, String project, String bugID) throws IOException{
		String fileName = null;
		
		DeadEndData data = dataList.get(0);
		
		if(data instanceof DataDeadEndData){
			DataDeadEndData ddd = (DataDeadEndData)data;
			if(ddd.type==DataDeadEndData.LOCAL_VAR){
				fileName = LOCAL_DATA + fileSuffix + ".csv";
				appendLocalVarData(fileName, dataList, project, bugID);
			}
			else if(ddd.type==DataDeadEndData.FIELD){
				fileName = FIELD + fileSuffix + ".csv";
				appendFieldData(fileName, dataList, project, bugID);
			}
			else if(ddd.type==DataDeadEndData.ARRAY_ELEMENT){
				fileName = ARRAY + fileSuffix + ".csv";
				appendArrayElementData(fileName, dataList, project, bugID);
			}
		}
		else if(data instanceof ControlDeadEndData){
			fileName = CONTROL + fileSuffix + ".csv";
			appendControlData(fileName, dataList, project, bugID);
		}
	}

	private void appendControlData(String fileName, List<DeadEndData> dataList, String project, String bugID) throws IOException{
		File file = new File(csvFolder + fileName);
		if(!file.exists()){
			file.createNewFile();
		}
		
		FileWriter writer = new FileWriter(file.getAbsolutePath(), true);
		for(DeadEndData data0: dataList){
			ControlDeadEndData dData = (ControlDeadEndData)data0;
			String plainText = dData.getPlainText(project, bugID);
			writer.append(plainText);
		}
		
		writer.flush();
		writer.close();	
	}

	private void appendArrayElementData(String fileName, List<DeadEndData> dataList, String project, String bugID) throws IOException{
		File file = new File(csvFolder + fileName);
		if(!file.exists()){
			file.createNewFile();
		}
		
		FileWriter writer = new FileWriter(file.getAbsolutePath(), true);
		for(DeadEndData data0: dataList){
			DataDeadEndData dData = (DataDeadEndData)data0;
			String plainText = dData.getPlainText(project, bugID);
			writer.append(plainText);
		}
		
		writer.flush();
		writer.close();	
	}

	private void appendFieldData(String fileName, List<DeadEndData> dataList, String project, String bugID) throws IOException {
		File file = new File(csvFolder + fileName);
		if(!file.exists()){
			file.createNewFile();
		}
		
		FileWriter writer = new FileWriter(file.getAbsolutePath(), true);
		for(DeadEndData data0: dataList){
			DataDeadEndData dData = (DataDeadEndData)data0;
			String plainText = dData.getPlainText(project, bugID);
			System.currentTimeMillis();
			writer.append(plainText);
		}
		
		writer.flush();
		writer.close();	
	}

	private void appendLocalVarData(String fileName, List<DeadEndData> dataList, String project, String bugID) throws IOException {
		File file = new File(csvFolder + fileName);
		if(!file.exists()){
			file.createNewFile();
		}
		
		FileWriter writer = new FileWriter(file.getAbsolutePath(), true);
		for(DeadEndData data0: dataList){
			DataDeadEndData dData = (DataDeadEndData)data0;
			String plainText = dData.getPlainText(project, bugID);
			writer.append(plainText);
		}
		
		writer.flush();
		writer.close();	
	}
	
}
