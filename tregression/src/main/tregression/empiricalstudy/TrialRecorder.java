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
	private int trialNumberLimitPerFile = 300000;
	
	private String fileName = "defects4j";
	private String excelFolder = "";
	
	public TrialRecorder() throws IOException{
		this("defects4j", "");
	}
	
	public TrialRecorder(String initFileName, String excelFolder) throws IOException {
		String fileTitle = initFileName;
		fileName = fileTitle + filePage + ".xlsx";
		this.excelFolder = excelFolder;
		file = new File(excelFolder + fileName);
		
		while(file.exists()){
			InputStream excelFileToRead = new FileInputStream(file);
			book = new XSSFWorkbook(excelFileToRead);
			sheet = book.getSheetAt(0);
			
			lastRowNum = sheet.getPhysicalNumberOfRows();
			if(lastRowNum > trialNumberLimitPerFile){
				filePage++;
				fileName = fileTitle + filePage + ".xlsx";
				file = new File(excelFolder + fileName);
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
	
	public File export(List<EmpiricalTrial> trialList, String project, int bugID) {
		return export(trialList, project, String.valueOf(bugID), null);
	}
	
	public File export(List<EmpiricalTrial> trialList, String project, String bugID, String mutationType) {
		
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
		
		writeToExcel(book, file.getAbsolutePath());
		
		if(lastRowNum > trialNumberLimitPerFile){
			filePage++;
			String outputName = fileName + filePage + ".xlsx";
			file = new File(excelFolder + outputName);
			
			initializeNewExcel();
		}
		return file;
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
		
		setCellValue(row, Header.BREAK_TO_BUG, trial.isBreakSlice());
		setCellValue(row, Header.EXECTION_TIME, trial.getExecutionTime());
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
				String str = r.getVarValue().getVariable().getClass().getName();
				varType = str.substring(str.lastIndexOf("."), str.length());
				row.createCell(count++).setCellValue(varType);
			}
		}	
	}
	
	private void setCellValue(Row row, Header header, boolean value) {
		row.createCell(header.getIndex()).setCellValue(value);
	}
	
	private void setCellValue(Row row, Header header, long value) {
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
	
	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
}
