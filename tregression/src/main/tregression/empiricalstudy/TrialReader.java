package tregression.empiricalstudy;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class TrialReader {
	@SuppressWarnings("resource")
	public Map<ReadEmpiricalTrial, ReadEmpiricalTrial> readXLSX(String fileName) throws Exception {
		Map<ReadEmpiricalTrial, ReadEmpiricalTrial> map = new HashMap<>();
		File file = new File(fileName);
		if(file.exists()) {
			InputStream excelFileToRead = new FileInputStream(file);
			
			XSSFWorkbook wb = new XSSFWorkbook(excelFileToRead);
			XSSFSheet sheet = wb.getSheetAt(0);
			XSSFRow row;
			XSSFCell cell;

			Iterator<Row> rows = sheet.rowIterator();
			while (rows.hasNext()) {
				row = (XSSFRow) rows.next();

				if (row.getRowNum() > 0) {
					ReadEmpiricalTrial trial = new ReadEmpiricalTrial();
					
					Iterator<Cell> cells = row.cellIterator();
					while (cells.hasNext()) {
						cell = (XSSFCell) cells.next();
						int i = cell.getColumnIndex();
						switch (i) {
						case 0:
							String project = cell.getStringCellValue();
							trial.setProject(project);
							break;
						case 1:
							String id = cell.getStringCellValue();
							trial.setBugID(id);
							break;
						case 5:
							int rootcaseNode = (int) cell.getNumericCellValue();
							trial.setRootcauseNode(rootcaseNode);
							break;
						case 15:
							String debugType = cell.getStringCellValue();
							trial.setDebugType(debugType);
							break;
						case 18:
							String exception = cell.getStringCellValue();
							trial.setException(exception);
							break;
						case 20:
//							String deadEndType = cell.getStringCellValue();
//							trial.setDeadEndType(deadEndType);
							break;
						}
					}
					
					map.put(trial, trial);
				}
			}
		}
		
		return map;
	}
}
