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

import microbat.model.variable.ArrayElementVar;
import microbat.model.variable.FieldVar;
import microbat.model.variable.LocalVar;
import microbat.model.variable.Variable;
import tregression.model.StepOperationTuple;

public class MutationTrialRecorder {
	private File file;
	
	Sheet[] sheets;
	int[] lastRowNums = new int[]{1, 1, 1};
	
	private Workbook book;
	
	
	private String fileName = "defects4j";
	private String excelFolder = "";
	
	public MutationTrialRecorder() throws IOException{
		this("defects4j", "");
	}
	
	public MutationTrialRecorder(String initFileName, String excelFolder) throws IOException {
		String fileTitle = initFileName;
		fileName = fileTitle + ".xlsx";
		this.excelFolder = excelFolder;
		file = new File(excelFolder + fileName);
		
		if(file.exists()){
			InputStream excelFileToRead = new FileInputStream(file);
			book = new XSSFWorkbook(excelFileToRead);
			
			sheets = new Sheet[3];
			for(int i=0; i<3; i++){
				sheets[i] = book.getSheetAt(i);
				lastRowNums[i] = sheets[i].getPhysicalNumberOfRows();
			}
		}
		
		if(!file.exists()){
			initializeNewExcel();
		}
	}
	
	private String getSheetName(int index){
		if(index==0){
			return "control";
		}
		else if(index==1){
			return "field";
		}
		else if(index==2){
			return "local_var";
		}
		
		return "unknown";
	}
	
	private void initializeNewExcel() {
		sheets = new Sheet[3];
		book = new XSSFWorkbook();
		for(int i=0; i<sheets.length; i++){
			String name = getSheetName(i);
			Sheet sheet = book.createSheet(name);
			sheets[i] = sheet;
			lastRowNums[i] = 1;
		}
		
		book = new XSSFWorkbook();
		for(int i=0; i<sheets.length; i++){
			String name = getSheetName(i);
			sheets[i] = book.createSheet(name);
			
			Row row = sheets[i].createRow(0);
			for (Header header : Header.values()) {
				row.createCell(header.getIndex()).setCellValue(header.getTitle()); 
			}
		}
	}
	
	public void export(List<EmpiricalTrial> trialList, String project, int bugID) {
		export(trialList, project, String.valueOf(bugID), null);
	}
	
	public void export(List<EmpiricalTrial> trialList, String project, String bugID, String mutationType) {
		
		if(!trialList.isEmpty()) {
			for(EmpiricalTrial trial: trialList) {
				
				if(trial.getRootcauseNode()!=null){
					List<DeadEndRecord> list = trial.getDeadEndRecordList();
					
					if(!list.isEmpty()){
						DeadEndRecord record = list.get(0);
						
						int index = -1;
						if(record.getType()==DeadEndRecord.CONTROL){
							index = 0;
						}
						else{
							Variable var = record.getVarValue().getVariable();
							if(var instanceof FieldVar || var instanceof ArrayElementVar){
								index = 1;
							}
							else if(var instanceof LocalVar){
								index = 2;
							}
						}
						
						if(index != -1){
							Row row = sheets[index].createRow(lastRowNums[index]);
							fillRowInformation(row, trial, project, bugID, mutationType);
							lastRowNums[index] = lastRowNums[index] + 1;
						}
					}
					
					
				}
				
				
			}
			
			writeToExcel(book, file.getAbsolutePath());
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
		
		setCellValue(row, Header.BREAK_TO_BUG, trial.isBreakSlice());
		
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
