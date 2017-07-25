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

import tregression.model.StepOperationTuple;

public class TrialRecorder {
	private File file;
	private Sheet sheet;
	private Workbook book;
	private int lastRowNum = 1;
	
	private int filePage = 0;
	private int trialNumberLimitPerFile = 3000;
	
	public TrialRecorder() throws IOException{
		String fileTitle = "defects4j";
		String fileName = fileTitle + filePage + ".xlsx";
		file = new File(fileName);
		
		while(file.exists()){
			InputStream excelFileToRead = new FileInputStream(file);
			book = new XSSFWorkbook(excelFileToRead);
			sheet = book.getSheetAt(0);
			
			lastRowNum = sheet.getPhysicalNumberOfRows();
			if(lastRowNum > trialNumberLimitPerFile){
				filePage++;
				fileName = fileTitle + filePage + ".xlsx";
				file = new File(fileName);
			}
			else{
				break;
			}
		}
		
		if(!file.exists()){
			initializeNewExcel();
		}
	}
	
	private void initializeNewExcel() {
		lastRowNum = 1;
		
		book = new XSSFWorkbook();
		sheet = book.createSheet("data");
		
		List<String> titles = new ArrayList<>();
		titles.add("project");
		titles.add("bug_ID");
		titles.add("type");
		titles.add("overskip steps");
		titles.add("rootcause node");
		titles.add("checklist");
		
		Row row = sheet.createRow(0);
		for(int i = 0; i < titles.size(); i++){
			row.createCell(i).setCellValue(titles.get(i)); 
		}
	}
	
	public void export(List<EmpiricalTrial> trialList, String project, int bugID) {
		
		for(EmpiricalTrial trial: trialList) {
			Row row = sheet.createRow(lastRowNum);
			fillRowInformation(row, trial, project, bugID);
			lastRowNum++;
		}
        
        writeToExcel(book, file.getName());
        
        if(lastRowNum > trialNumberLimitPerFile){
        	filePage++;
        	String fileName = "defects4j" + filePage + ".xlsx";
			file = new File(fileName);
			
			initializeNewExcel();
        }
	}
	
	private void fillRowInformation(Row row, EmpiricalTrial trial, String project, int bugID) {
		row.createCell(0).setCellValue(project);
		row.createCell(1).setCellValue(bugID);
		row.createCell(2).setCellValue(EmpiricalTrial.getTypeStringName(trial.getBugType()));
		row.createCell(3).setCellValue(trial.getOverskipLength());
		row.createCell(4).setCellValue(trial.getRootcauseNode().getOrder());
		
		StringBuffer buf = new StringBuffer();
		for(StepOperationTuple t: trial.getCheckList()) {
			buf.append(t.toString());
			buf.append("\n");
		}
		row.createCell(5).setCellValue(buf.toString());
		
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
