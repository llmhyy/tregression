package tregression.empiricalstudy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
		
		Row row = sheet.createRow(0);
		for (Header header : Header.values()) {
			row.createCell(header.getIndex()).setCellValue(header.getTitle()); 
		}
	}
	
	public void export(List<EmpiricalTrial> trialList, String project, int bugID) {
		export(trialList, project, String.valueOf(bugID), null);
	}
	
	public void export(List<EmpiricalTrial> trialList, String project, String bugID, String mutationType) {
		
		if(!trialList.isEmpty()) {
			for(EmpiricalTrial trial: trialList) {
				Row row = sheet.createRow(lastRowNum);
				fillRowInformation(row, trial, project, bugID, mutationType);
				lastRowNum++;
			}
		}
		else {
			Row row = sheet.createRow(lastRowNum);
			fillRowInformation(row, null, project, bugID, mutationType);
		}
		
		writeToExcel(book, file.getName());
		
		if(lastRowNum > trialNumberLimitPerFile){
			filePage++;
			String fileName = "defects4j" + filePage + ".xlsx";
			file = new File(fileName);
			
			initializeNewExcel();
		}
	}
	
	private void fillRowInformation(Row row, EmpiricalTrial trial, String project, String bugID, String mutationType) {
		if (trial==null) {
			trial = new EmpiricalTrial(-1, -1, null, null, 0, 0, 0, -1, -1, null, false);
		}
		setCellValue(row, Header.PROJECT, project);
		setCellValue(row, Header.BUG_ID, bugID);
		setCellValue(row, Header.MUTATION_TYPE, mutationType);
		setCellValue(row, Header.TESTCASE, trial.getTestcase());
		
		int order = -1;
		if(trial.getRootcauseNode()!=null) {
			order = trial.getRootcauseNode().getOrder();
		}
		setCellValue(row, Header.FOUND_CAUSE, order);
		
		order = -1;
		if(trial.getRootCauseFinder()!=null) {
			if(!trial.getRootCauseFinder().getRealRootCaseList().isEmpty()){
				order = trial.getRootCauseFinder().getRealRootCaseList().get(0).getRoot().getOrder();				
			}
		}
		setCellValue(row, Header.GENERAL_CAUSE, order);
		setCellValue(row, Header.BUGGY_TRACE_LENGTH, trial.getBuggyTraceLength());
		setCellValue(row, Header.CORRECT_TRACE_LENGTH, trial.getCorrectTranceLength());
		setCellValue(row, Header.TRACE_COLLECTION_TIME, trial.getTraceCollectionTime());
		setCellValue(row, Header.TRACE_MATCH_TIME, trial.getTraceMatchTime());
		setCellValue(row, Header.SIMULATION_TIME, trial.getSimulationTime());
		setCellValue(row, Header.EXPLANATION_SIZE, trial.getTotalVisitedNodesNum());
		
		if(trial.getVisitedRegressionNodes()!=null) {
			setCellValue(row, Header.REGRESSION_EXPLANATION, trial.getVisitedRegressionNodes().toString());			
		}
		
		if(trial.getVisitedCorrectNodes()!=null) {
			setCellValue(row, Header.CORRECT_EXPLANATION, trial.getVisitedCorrectNodes().toString());			
		}
		
		setCellValue(row, Header.TYPE, EmpiricalTrial.getTypeStringName(trial.getBugType()));
		setCellValue(row, Header.OVERSKIP, trial.getOverskipLength());
		StringBuffer buf = new StringBuffer();
		if(trial.getCheckList()!=null) {
			for(StepOperationTuple t: trial.getCheckList()) {
				buf.append(t.toString());
				buf.append("\n");
			}
			setCellValue(row, Header.CHECK_LIST, buf.toString());
		}
		
		if (trial.getExceptionExplanation()!=null) {
			setCellValue(row, Header.EXCEPTION, trial.getExceptionExplanation());
		}
		setCellValue(row, Header.MULTI_THREAD, trial.isMultiThread());
		
		int count = Header.DEADEND_TYPE.getIndex();
		List<DeadEndRecord> mendings = trial.getDeadEndRecordList();
		for(DeadEndRecord r: mendings){
			row.createCell(count++).setCellValue(r.getTypeString());
			row.createCell(count++).setCellValue(r.getOccurOrder());
			row.createCell(count++).setCellValue(r.getDeadEndOrder());
			row.createCell(count++).setCellValue(r.getBreakStepOrder());
			String type = "*unexpected*";
			if(r.getSolutionPattern()!=null){
				type = r.getSolutionPattern().getTypeName();
			}
			row.createCell(count++).setCellValue(type);
			
			String varType = "";
			if(r.getType()==DeadEndRecord.DATA){
				varType = r.getVarValue().getVariable().getClass().getName();
				row.createCell(count++).setCellValue(varType);
			}
		}	
	}
	
	private void setCellValue(Row row, Header header, boolean value) {
		row.createCell(header.getIndex()).setCellValue(value);
	}

	private void setCellValue(Row row, Header header, int value) {
		row.createCell(header.getIndex()).setCellValue(value);
	}

	private void setCellValue(Row row, Header header, String value) {
		row.createCell(header.getIndex()).setCellValue(value);
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
	
	private static enum Header {
		PROJECT ("project"), 
		BUG_ID ("bug_ID"),
		MUTATION_TYPE ("mutation type"),
		TESTCASE("test case"),
		FOUND_CAUSE ("found cause"),
		GENERAL_CAUSE ("general cause"),
		
		BUGGY_TRACE_LENGTH ("buggy trace length"),
		CORRECT_TRACE_LENGTH ("correct trace length"),
		TRACE_COLLECTION_TIME ("trace collection time"),
		TRACE_MATCH_TIME ("trace match time"),
		SIMULATION_TIME ("simulation time"),
		EXPLANATION_SIZE ("explanation size"),
		REGRESSION_EXPLANATION ("regression explanation nodes"),
		CORRECT_EXPLANATION ("correct explanation nodes"),
		
		TYPE ("type"),
		OVERSKIP ("overskip steps"),
		CHECK_LIST ("checklist"),
		
		EXCEPTION ("exception"),
		MULTI_THREAD ("multi thread"),
		
		DEADEND_TYPE ("type"),
		DEADEND_OCCUR ("occur"),
		DEADEND ("dead end"),
		DEADEND_BREAK ("break"),
		DEADEND_SOLUTION ("solution"),
		VAR_TYPE ("var type");
		
		private String title;
		private Header(String title) {
			this.title = title;
		}
		
		public String getTitle() {
			return title;
		}
		
		public int getIndex() {
			return ordinal();
		}
	}
}
