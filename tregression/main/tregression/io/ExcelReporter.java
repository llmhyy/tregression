package tregression.io;

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

import sav.common.core.utils.CollectionUtils;
import sav.common.core.utils.StringUtils;
import tregression.model.Trial;

public class ExcelReporter {
	
	private File file;
	private Sheet sheet;
	private Workbook book;
	private int lastRowNum = 1;
	
	private int filePage = 0;
	private int trialNumberLimitPerFile = 3000;
	
	private double[] unclearRates;
	private String projectName;
	
	public ExcelReporter(String projectName, double[] unclearRates) throws IOException{
		
		projectName = projectName + "_regression";
		
		this.unclearRates = unclearRates;
		this.projectName = projectName;
		
		String fileName = projectName + filePage + ".xlsx";
		file = new File(fileName);
		
		while(file.exists()){
			InputStream excelFileToRead = new FileInputStream(file);
			book = new XSSFWorkbook(excelFileToRead);
			sheet = book.getSheetAt(0);
			
			lastRowNum = sheet.getPhysicalNumberOfRows();
			if(lastRowNum > trialNumberLimitPerFile){
				filePage++;
				fileName = projectName + filePage + ".xlsx";
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
		titles.add("test case");
		titles.add("mutation type");
		titles.add("mutation file");
		titles.add("mutation line");
		titles.add("total steps");
		titles.add("original total steps");
		titles.add("trace diff");
		titles.add("time");
		
		for(int i=0; i<unclearRates.length; i++){
			String jumpSteps = "jump steps";
			String result = "result";
			String detail = "detail";
			
			titles.add(jumpSteps);
			titles.add(result);
			titles.add(detail);
		}
		
		Row row = sheet.createRow(0);
		for(int i = 0; i < titles.size(); i++){
			row.createCell(i).setCellValue(titles.get(i)); 
		}
	}
	
	public void export(List<Trial> trialList) {
		Row row = sheet.createRow(lastRowNum);
		
		fillRowInformation(row, trialList);
		
		writeToExcel(book, file.getName());
        
        lastRowNum++;
        
        if(lastRowNum > trialNumberLimitPerFile){
        	filePage++;
        	String fileName = projectName + filePage + ".xlsx";
			file = new File(fileName);
			
			initializeNewExcel();
        }
	}

	private void fillRowInformation(Row row, List<Trial> trialList) {
		Trial trial = trialList.get(0);
		int idx = 0;
		row.createCell(idx++).setCellValue(trial.getTestCaseName());
		row.createCell(idx++).setCellValue(trial.getMutationType());
		row.createCell(idx++).setCellValue(trial.getMutatedFile());
		row.createCell(idx++).setCellValue(trial.getMutatedLineNumber());
		row.createCell(idx++).setCellValue(trial.getTotalSteps());
		row.createCell(idx++).setCellValue(trial.getOriginalTotalSteps());
		row.createCell(idx++).setCellValue(Math.abs(trial.getOriginalTotalSteps()-trial.getTotalSteps()));
		row.createCell(idx++).setCellValue(trial.getTime());
		row.createCell(idx++).setCellValue(CollectionUtils.getSize(trial.getJumpSteps()));
		row.createCell(idx++).setCellValue(trial.getResult());
		row.createCell(idx++).setCellValue(StringUtils.toString(trial.getJumpSteps(), ""));
		
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

//	public void export(Trial clearLoopTrial, Trial unclearLoopTrial, 
//			Trial clearNonloopTrial, Trial unclearNonloopTrial) throws IOException{
//		
//		Row row = sheet.createRow(lastRowNum);
//		fillRowInformation(row, clearLoopTrial, unclearLoopTrial, clearNonloopTrial, unclearNonloopTrial);
//		
////		for(int i=0; i<titles.length; i++){
////        	sheet.autoSizeColumn(i);
////        }
//		
//        writeToExcel(book, file.getName());
//        
//        lastRowNum++;
//	}
	
//	private void fillRowInformation(Row row, Trial clearLoopTrial, Trial unclearLoopTrial,
//			Trial clearNonloopTrial, Trial unclearNonloopTrial) {
//		row.createCell(0).setCellValue(clearLoopTrial.getTestCaseName());
//		row.createCell(1).setCellValue(clearLoopTrial.getMutatedFile());
//		row.createCell(2).setCellValue(clearLoopTrial.getMutatedLineNumber());
//		row.createCell(3).setCellValue(clearLoopTrial.getTotalSteps());
//		row.createCell(4).setCellValue(clearLoopTrial.getTime());
//		
//		
//		row.createCell(5).setCellValue(clearLoopTrial.getJumpSteps().size());
//		row.createCell(6).setCellValue(unclearLoopTrial.getJumpSteps().size());
//		row.createCell(7).setCellValue(unclearLoopTrial.getUnclearFeedbackNumber());
//		
//		row.createCell(8).setCellValue(clearNonloopTrial.getJumpSteps().size());
//		row.createCell(9).setCellValue(unclearNonloopTrial.getJumpSteps().size());
//		row.createCell(10).setCellValue(unclearNonloopTrial.getUnclearFeedbackNumber());
//		
//		row.createCell(11).setCellValue(clearLoopTrial.isBugFound());
//		row.createCell(12).setCellValue(unclearLoopTrial.isBugFound());
//		row.createCell(13).setCellValue(clearNonloopTrial.isBugFound());
//		row.createCell(14).setCellValue(unclearNonloopTrial.isBugFound());
//		
//		row.createCell(15).setCellValue(clearLoopTrial.getJumpSteps().toString());
//		row.createCell(16).setCellValue(unclearLoopTrial.getJumpSteps().toString());
//		row.createCell(17).setCellValue(clearNonloopTrial.getJumpSteps().toString());
//		row.createCell(18).setCellValue(unclearNonloopTrial.getJumpSteps().toString());
//	}

	

	
}
