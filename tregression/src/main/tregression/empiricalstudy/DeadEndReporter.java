package tregression.empiricalstudy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.eclipse.jdt.core.dom.ASTNode;

import microbat.codeanalysis.ast.ASTEncoder;
import tregression.empiricalstudy.training.ControlDeadEndData;
import tregression.empiricalstudy.training.DataDeadEndData;
import tregression.empiricalstudy.training.DeadEndData;

public class DeadEndReporter {
	private String fileTitle = "dead-end";
	
	private File file;
	
	private Sheet localDataSheet;
	private Sheet fieldSheet;
	private Sheet arraySheet;
	private Sheet controlSheet;
	
	public static String LOCAL_DATA_SHEET = "local_var";
	public static String FIELD_SHEET = "field";
	public static String ARRAY_SHEET = "array";
	public static String CONTROL_SHEET = "control";
	
	private Workbook book;
	private int lastLocalVarRowNum = 1;
	private int lastFieldRowNum = 1;
	private int lastArrayRowNum = 1;
	private int lastControlRowNum = 1;
	
	private int filePage = 0;
	private int trialNumberLimitPerFile = 1000000;
	
	public DeadEndReporter() throws IOException{
		String fileName = fileTitle + filePage + ".xlsx";
		file = new File(fileName);
		while(file.exists()){
			InputStream excelFileToRead = new FileInputStream(file);
			book = new XSSFWorkbook(excelFileToRead);
			localDataSheet = book.getSheet(LOCAL_DATA_SHEET);
			fieldSheet = book.getSheet(FIELD_SHEET);
			arraySheet = book.getSheet(ARRAY_SHEET);
			controlSheet = book.getSheet(CONTROL_SHEET);
			
			lastLocalVarRowNum = localDataSheet.getPhysicalNumberOfRows();
			lastFieldRowNum = fieldSheet.getPhysicalNumberOfRows();
			lastArrayRowNum = arraySheet.getPhysicalNumberOfRows();
			lastControlRowNum = controlSheet.getPhysicalNumberOfRows();
//			if(lastRowNum > trialNumberLimitPerFile){
//				filePage++;
//				fileName = fileTitle + filePage + ".xlsx";
//				file = new File(fileName);
//			}
//			else{
//				break;
//			}
			break;
		}
		
		if(!file.exists()){
			initializeNewExcel();
		}
	}
	
	private void initializeNewExcel() {
		book = new XSSFWorkbook();
		createLocalVarSheet();
		createFieldSheet();
		createArraySheet();
		createControlSheet();
	}
	
	private void createControlSheet() {
		controlSheet = book.createSheet("control");
		
		List<String> titles = new ArrayList<>();
		titles.add("project");
		titles.add("bug_ID");
		titles.add("test_case");
		titles.add("trace_order");
		titles.add("is_break_step");
		
		titles.add("move_ups");
		titles.add("move_downs");
		titles.add("move_rights");
		
		titles.add("data_dependency");
		titles.add("control_dependency");
		
		List<String> cTitles = getCommonTitles();
		titles.addAll(cTitles);
		
		Row row = controlSheet.createRow(0);
		for(int i = 0; i < titles.size(); i++){
			row.createCell(i).setCellValue(titles.get(i)); 
		}
		
		this.lastControlRowNum = 1;
	}
	
	private List<String> getCommonPrefix(){
		List<String> list = new ArrayList<>();
		list.add("b");
		list.add("bc");
		list.add("o");
		list.add("oc");
		list.add("d");
		list.add("dc");
		
		return list;
	}
	
	private List<String> getCommonTitles(){
		List<String> titles = new ArrayList<>();
		titles.add("length");
		
		List<String> commonPrefix = getCommonPrefix();
		
		for(String str: commonPrefix){
			for(int i=0; i<ASTEncoder.getDimensions(); i++){
				int type = i+1;
				String columnName = null;
				if(type<=ASTEncoder.baseASTNodeNumber){
					columnName = ASTNode.nodeClassForType(type).getName();
				}
				else{
					columnName = ASTEncoder.getAbstractASTType(i);
				}
				
				if(columnName.contains(".")){
					columnName = columnName.substring(columnName.lastIndexOf(".")+1, columnName.length());
				}
				
				titles.add(str+"-"+columnName);
			}
		}
		
		return titles;
	}
	
	private void createLocalVarSheet() {
		localDataSheet = book.createSheet(LOCAL_DATA_SHEET);
		
		List<String> titles = new ArrayList<>();
		titles.add("project");
		titles.add("bug_ID");
		titles.add("test_case");
		titles.add("trace_order");
		titles.add("is_break_step");
		
		titles.add("critical_conditional_step");
		titles.add("w_local_var_type");
		titles.add("w_local_var_name");
		
		titles.add("r_local_var_type");
		titles.add("r_local_var_name");
		
		List<String> cTitles = getCommonTitles();
		titles.addAll(cTitles);
		
		Row row = localDataSheet.createRow(0);
		for(int i = 0; i < titles.size(); i++){
			row.createCell(i).setCellValue(titles.get(i)); 
		}
		
		this.lastLocalVarRowNum = 1;
	}
	
	private void createFieldSheet() {
		fieldSheet = book.createSheet(FIELD_SHEET);
		
		List<String> titles = new ArrayList<>();
		titles.add("project");
		titles.add("bug_ID");
		titles.add("test_case");
		titles.add("trace_order");
		titles.add("is_break_step");
		
		titles.add("critical_conditional_step");
		titles.add("w_parent");
		titles.add("w_parent_type");
		titles.add("w_type");
		titles.add("w_name");
		
		titles.add("r_parent");
		titles.add("r_parent_type");
		titles.add("r_type");
		titles.add("r_name");
		
		List<String> cTitles = getCommonTitles();
		titles.addAll(cTitles);
		
		Row row = fieldSheet.createRow(0);
		for(int i = 0; i < titles.size(); i++){
			row.createCell(i).setCellValue(titles.get(i)); 
		}
		
		this.lastFieldRowNum = 1;
	}
	
	private void createArraySheet() {
		arraySheet = book.createSheet(ARRAY_SHEET);
		
		List<String> titles = new ArrayList<>();
		titles.add("project");
		titles.add("bug_ID");
		titles.add("test_case");
		titles.add("trace_order");
		titles.add("is_break_step");
		
		titles.add("critical_conditional_step");
		titles.add("w_parent");
		titles.add("w_type");
		titles.add("w_name");
		
		titles.add("r_parent");
		titles.add("r_type");
		titles.add("r_name");
		
		List<String> cTitles = getCommonTitles();
		titles.addAll(cTitles);
		
		Row row = arraySheet.createRow(0);
		for(int i = 0; i < titles.size(); i++){
			row.createCell(i).setCellValue(titles.get(i)); 
		}
		
		this.lastArrayRowNum = 1;
	}
	
	public void export(List<DeadEndData> dataList, String project, int bugId) {
		export(dataList, project, String.valueOf(bugId));
	}

	public void export(List<DeadEndData> dataList, String project, String bugId) {
		
		if(!dataList.isEmpty()) {
			for(DeadEndData data: dataList) {
				if(data instanceof DataDeadEndData){
					DataDeadEndData ddd = (DataDeadEndData)data;
					if(ddd.type==DataDeadEndData.LOCAL_VAR){
						Row row = this.localDataSheet.createRow(this.lastLocalVarRowNum);
						fillLocalVarRowInformation(row, ddd, project, bugId);
						this.lastLocalVarRowNum++;
					}
					else if(ddd.type==DataDeadEndData.FIELD){
						Row row = this.fieldSheet.createRow(this.lastFieldRowNum);
						fillFieldRowInformation(row, ddd, project, bugId);
						this.lastFieldRowNum++;
					}
					else if(ddd.type==DataDeadEndData.ARRAY_ELEMENT){
						Row row = this.arraySheet.createRow(this.lastArrayRowNum);
						fillArrayRowInformation(row, ddd, project, bugId);
						this.lastArrayRowNum++;
					}
					
				}
				else if(data instanceof ControlDeadEndData){
					Row row = this.controlSheet.createRow(this.lastControlRowNum);
					fillControlRowInformation(row, (ControlDeadEndData)data, project, bugId);
					this.lastControlRowNum++;
				}
				
			}
		}
		
		writeToExcel(book, file.getName());
		
//		if(lastRowNum > trialNumberLimitPerFile){
//			filePage++;
//			String fileName = fileTitle + filePage + ".xlsx";
//			file = new File(fileName);
//			
//			initializeNewExcel();
//		}
	}
	
	private void fillLocalVarRowInformation(Row row, DataDeadEndData data, String project, String bugId) {
		
		row.createCell(0).setCellValue(project);
		row.createCell(1).setCellValue(bugId);
		row.createCell(2).setCellValue(data.testcase);
		row.createCell(3).setCellValue(data.traceOrder);
		row.createCell(4).setCellValue(data.isBreakStep);
		
		row.createCell(5).setCellValue(data.criticalConditionalStep);
		
		row.createCell(6).setCellValue(data.sameWLocalVarType);
		row.createCell(7).setCellValue(data.sameWLocalVarName);
		
		
		row.createCell(8).setCellValue(data.sameRLocalVarType);
		row.createCell(9).setCellValue(data.sameRLocalVarName);
		
		row.createCell(10).setCellValue(data.deadEndLength);
		
		fillCommonRowInfomation(row, 11, data);
		
	}
	
	private void fillFieldRowInformation(Row row, DataDeadEndData data, String project, String bugId) {
		
		row.createCell(0).setCellValue(project);
		row.createCell(1).setCellValue(bugId);
		row.createCell(2).setCellValue(data.testcase);
		row.createCell(3).setCellValue(data.traceOrder);
		row.createCell(4).setCellValue(data.isBreakStep);
		
		row.createCell(5).setCellValue(data.criticalConditionalStep);
		
		row.createCell(6).setCellValue(data.sameWFieldParent);
		row.createCell(7).setCellValue(data.sameWFieldParentType);
		row.createCell(8).setCellValue(data.sameWFieldType);
		row.createCell(9).setCellValue(data.sameWFieldName);		
		
		row.createCell(10).setCellValue(data.sameRFieldParent);
		row.createCell(11).setCellValue(data.sameRFieldParentType);
		row.createCell(12).setCellValue(data.sameRFieldType);
		row.createCell(13).setCellValue(data.sameRFieldName);
		
		row.createCell(14).setCellValue(data.deadEndLength);
		
		fillCommonRowInfomation(row, 15, data);
		
	}
	
	private void fillCommonRowInfomation(Row row, int startCell, DeadEndData data){
		int count = startCell;
		for(int value: data.stepAST){
			row.createCell(count++).setCellValue(value);
		}
		
		for(int value: data.stepContextAST){
			row.createCell(count++).setCellValue(value);
		}
		
		for(int value: data.occurStepAST){
			row.createCell(count++).setCellValue(value);
		}
		
		for(int value: data.occurStepContextAST){
			row.createCell(count++).setCellValue(value);
		}
		
		for(int value: data.deadEndStepAST){
			row.createCell(count++).setCellValue(value);
		}
		
		for(int value: data.deadEndStepContextAST){
			row.createCell(count++).setCellValue(value);
		}
	}
	
	private void fillArrayRowInformation(Row row, DataDeadEndData data, String project, String bugId) {
		
		row.createCell(0).setCellValue(project);
		row.createCell(1).setCellValue(bugId);
		row.createCell(2).setCellValue(data.testcase);
		row.createCell(3).setCellValue(data.traceOrder);
		row.createCell(4).setCellValue(data.isBreakStep);
		
		row.createCell(5).setCellValue(data.criticalConditionalStep);
		
		row.createCell(6).setCellValue(data.sameWArrayParent);
		row.createCell(7).setCellValue(data.sameWArrayType);
		row.createCell(8).setCellValue(data.sameRArrayIndex);		
		
		row.createCell(9).setCellValue(data.sameRArrayParent);
		row.createCell(10).setCellValue(data.sameRArrayType);
		row.createCell(11).setCellValue(data.sameRArrayIndex);
		
		row.createCell(12).setCellValue(data.deadEndLength);
		
		fillCommonRowInfomation(row, 13, data);
	}
	
	private void fillControlRowInformation(Row row, ControlDeadEndData data, String project, String bugId) {
		
		row.createCell(0).setCellValue(project);
		row.createCell(1).setCellValue(bugId);
		row.createCell(2).setCellValue(data.testcase);
		row.createCell(3).setCellValue(data.traceOrder);
		row.createCell(4).setCellValue(data.isBreakStep);
		
		row.createCell(5).setCellValue(data.astMoveUps);
		row.createCell(6).setCellValue(data.astMoveDowns);
		row.createCell(7).setCellValue(data.astMoveRights);
		
		row.createCell(8).setCellValue(data.dataDependency);
		row.createCell(9).setCellValue(data.controlDependency);
		
		row.createCell(10).setCellValue(data.deadEndLength);
		
		fillCommonRowInfomation(row, 11, data);
	}
	
	private void writeToExcel(Workbook book, String fileName){
		try {
			FileOutputStream fileOut = new FileOutputStream(fileName);
			book.write(fileOut); 
			fileOut.close(); 
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
}
