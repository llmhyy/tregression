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
		titles.add("test case");
		titles.add("found cause");
		titles.add("general cause");
		
		titles.add("buggy trace length");
		titles.add("correct trace length");
		titles.add("trace collection time");
		titles.add("trace match time");
		titles.add("simulation time");
		titles.add("explanation size");
		titles.add("regression explanation nodes");
		titles.add("correct explanation nodes");
		
		titles.add("type");
		titles.add("overskip steps");
		titles.add("checklist");
		
		titles.add("exception");
		titles.add("multi thread");
		
		titles.add("mending type");
		titles.add("mending start");
		titles.add("mending correspondence");
		titles.add("mending return");
		
		Row row = sheet.createRow(0);
		for(int i = 0; i < titles.size(); i++){
			row.createCell(i).setCellValue(titles.get(i)); 
		}
	}
	
	public void export(List<EmpiricalTrial> trialList, String project, int bugID) {
		
		if(!trialList.isEmpty()) {
			for(EmpiricalTrial trial: trialList) {
				Row row = sheet.createRow(lastRowNum);
				fillRowInformation(row, trial, project, bugID);
				lastRowNum++;
			}
		}
		else {
			Row row = sheet.createRow(lastRowNum);
			fillRowInformation(row, null, project, bugID);
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
		if (trial==null) {
			trial = new EmpiricalTrial(-1, -1, null, null, null, 0, 0, 0, -1, -1, null, false);
		}
		
		row.createCell(0).setCellValue(project);
		row.createCell(1).setCellValue(bugID);
		row.createCell(2).setCellValue(trial.getTestcase());
		
		int order = -1;
		if(trial.getRootcauseNode()!=null) {
			order = trial.getRootcauseNode().getOrder();
		}
		row.createCell(3).setCellValue(order);
		
		order = -1;
		if(trial.getRealcauseNode()!=null) {
			order = trial.getRealcauseNode().getRoot().getOrder();
		}
		row.createCell(4).setCellValue(order);
		
		row.createCell(5).setCellValue(trial.getBuggyTraceLength());
		row.createCell(6).setCellValue(trial.getCorrectTranceLength());
		
		row.createCell(7).setCellValue(trial.getTraceCollectionTime());
		row.createCell(8).setCellValue(trial.getTraceMatchTime());
		row.createCell(9).setCellValue(trial.getSimulationTime());
		row.createCell(10).setCellValue(trial.getTotalVisitedNodesNum());
		
		if(trial.getVisitedRegressionNodes()!=null) {
			row.createCell(11).setCellValue(trial.getVisitedRegressionNodes().toString());			
		}
		
		if(trial.getVisitedCorrectNodes()!=null) {
			row.createCell(12).setCellValue(trial.getVisitedCorrectNodes().toString());			
		}
		
		row.createCell(13).setCellValue(EmpiricalTrial.getTypeStringName(trial.getBugType()));
		row.createCell(14).setCellValue(trial.getOverskipLength());
		StringBuffer buf = new StringBuffer();
		if(trial.getCheckList()!=null) {
			for(StepOperationTuple t: trial.getCheckList()) {
				buf.append(t.toString());
				buf.append("\n");
			}
			row.createCell(15).setCellValue(buf.toString());
		}
		
		if (trial.getExceptionExplanation()!=null) {
			row.createCell(16).setCellValue(trial.getExceptionExplanation());
		}
		
		row.createCell(17).setCellValue(trial.isMultiThread());
		
		int count = 18;
		List<DeadEndRecord> mendings = trial.getDeadEndRecordList();
		for(DeadEndRecord r: mendings){
			row.createCell(count++).setCellValue(r.getTypeString());
			row.createCell(count++).setCellValue(r.getOccurOrder());
			row.createCell(count++).setCellValue(r.getCorrespondingStepOnReference());
			row.createCell(count++).setCellValue(r.getBreakStepOrder());
		}	
		
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
