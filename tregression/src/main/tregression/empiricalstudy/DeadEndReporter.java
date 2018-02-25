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

import tregression.empiricalstudy.training.ControlDeadEndData;
import tregression.empiricalstudy.training.DataDeadEndData;
import tregression.empiricalstudy.training.DeadEndData;
import tregression.model.StepOperationTuple;

public class DeadEndReporter {
	private String fileTitle = "dead-end";
	
	private File file;
	
	private Sheet dataSheet;
	private Sheet controlSheet;
	
	private Workbook book;
	private int lastDataRowNum = 1;
	private int lastControlRowNum = 1;
	
	private int filePage = 0;
	private int trialNumberLimitPerFile = 1000000;
	
	public DeadEndReporter() throws IOException{
		String fileName = fileTitle + filePage + ".xlsx";
		file = new File(fileName);
		
		while(file.exists()){
			InputStream excelFileToRead = new FileInputStream(file);
			book = new XSSFWorkbook(excelFileToRead);
			dataSheet = book.getSheet("data");
			controlSheet = book.getSheet("control");
			
			lastDataRowNum = dataSheet.getPhysicalNumberOfRows();
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
		createDataSheet();
		createControlSheet();
	}
	
	private void createControlSheet() {
		controlSheet = book.createSheet("control");
		
		List<String> titles = new ArrayList<>();
		titles.add("project");
		titles.add("bug_ID");
		titles.add("test_case");
		titles.add("is_break_step");
		
		titles.add("move_ups");
		titles.add("move_downs");
		titles.add("move_rights");
		
		titles.add("data_dependency");
		titles.add("control_dependency");
		
		Row row = controlSheet.createRow(0);
		for(int i = 0; i < titles.size(); i++){
			row.createCell(i).setCellValue(titles.get(i)); 
		}
		
		this.lastControlRowNum = 1;
	}

	private void createDataSheet() {
		dataSheet = book.createSheet("data");
		
		List<String> titles = new ArrayList<>();
		titles.add("project");
		titles.add("bug_ID");
		titles.add("test_case");
		titles.add("is_break_step");
		
		titles.add("critical_conditional_step");
		titles.add("w_local_var_type");
		titles.add("w_local_var_name");
		titles.add("w_field_parent");
		titles.add("w_field_type");
		titles.add("w_field_name");
		titles.add("w_arraye_parent");
		titles.add("w_arraye_type");
		titles.add("w_arraye_index");
		
		titles.add("r_local_var_type");
		titles.add("r_local_var_name");
		titles.add("r_field_parent");
		titles.add("r_field_type");
		titles.add("r_field_name");
		titles.add("r_arraye_parent");
		titles.add("r_arraye_type");
		titles.add("r_arraye_index");
		
		Row row = dataSheet.createRow(0);
		for(int i = 0; i < titles.size(); i++){
			row.createCell(i).setCellValue(titles.get(i)); 
		}
		
		this.lastDataRowNum = 1;
	}

	public void export(List<DeadEndData> dataList, String project, int bugID) {
		
		if(!dataList.isEmpty()) {
			for(DeadEndData data: dataList) {
				if(data instanceof DataDeadEndData){
					Row row = this.dataSheet.createRow(this.lastDataRowNum);
					fillDataRowInformation(row, (DataDeadEndData)data, project, bugID);
					this.lastDataRowNum++;
				}
				else if(data instanceof ControlDeadEndData){
					Row row = this.controlSheet.createRow(this.lastControlRowNum);
					fillControlRowInformation(row, (ControlDeadEndData)data, project, bugID);
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
	
	private void fillDataRowInformation(Row row, DataDeadEndData data, String project, int bugID) {
		
		row.createCell(0).setCellValue(project);
		row.createCell(1).setCellValue(bugID);
		row.createCell(2).setCellValue(data.testcase);
		row.createCell(3).setCellValue(data.isBreakStep);
		
		row.createCell(4).setCellValue(data.criticalConditionalStep);
		
		row.createCell(5).setCellValue(data.sameWLocalVarType);
		row.createCell(6).setCellValue(data.sameWLocalVarName);
		row.createCell(7).setCellValue(data.sameWFieldParent);
		row.createCell(8).setCellValue(data.sameWFieldType);
		row.createCell(9).setCellValue(data.sameWFieldName);
		row.createCell(10).setCellValue(data.sameWArrayParent);
		row.createCell(11).setCellValue(data.sameWArrayType);
		row.createCell(12).setCellValue(data.sameWArrayIndex);
		
		
		row.createCell(13).setCellValue(data.sameRLocalVarType);
		row.createCell(14).setCellValue(data.sameRLocalVarName);
		row.createCell(15).setCellValue(data.sameRFieldParent);
		row.createCell(16).setCellValue(data.sameRFieldType);
		row.createCell(17).setCellValue(data.sameRFieldName);
		row.createCell(18).setCellValue(data.sameRArrayParent);
		row.createCell(19).setCellValue(data.sameRArrayType);
		row.createCell(20).setCellValue(data.sameRArrayIndex);
		
	}
	
	private void fillControlRowInformation(Row row, ControlDeadEndData data, String project, int bugID) {
		
		row.createCell(0).setCellValue(project);
		row.createCell(1).setCellValue(bugID);
		row.createCell(2).setCellValue(data.testcase);
		row.createCell(3).setCellValue(data.isBreakStep);
		
		row.createCell(4).setCellValue(data.moveUps);
		row.createCell(5).setCellValue(data.moveDowns);
		row.createCell(6).setCellValue(data.moveRights);
		
		row.createCell(7).setCellValue(data.dataDependency);
		row.createCell(8).setCellValue(data.controlDependency);
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
