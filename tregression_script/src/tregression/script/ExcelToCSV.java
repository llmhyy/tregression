package tregression.script;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelToCSV {
	
	String folder = "resource";
	
	
	
	public static void main(String[] args){
		
		ExcelToCSV etc = new ExcelToCSV();
		String excelFileName = etc.folder + File.separator + "dead-end0.xlsx";
		
		if(new File(excelFileName).exists()){
			System.currentTimeMillis();
		}
		else{
			System.currentTimeMillis();
		}
		
		String sheetName = "control";
		
		try {
			List<List<Integer>> data = etc.readXLSX(excelFileName, sheetName);
			String csvFileName = etc.folder + File.separator + "dead-end.csv";
			
			etc.writeCSV(csvFileName, data);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeCSV(String fileName, List<List<Integer>> data) throws IOException{
		FileWriter writer = new FileWriter("test.csv");

		for(List<Integer> l: data){
			for(Integer i: l){
				writer.append(String.valueOf(i));
				writer.append(',');
			}
			writer.append('\n');				
		}
		
		writer.flush();
		writer.close();			
	}
	
	@SuppressWarnings("resource")
	public List<List<Integer>> readXLSX(String fileName, String sheetName) throws IOException {
		List<List<Integer>> list = new ArrayList<>();
		
		File file = new File(fileName);
		InputStream excelFileToRead = new FileInputStream(file);
		
		XSSFWorkbook wb = new XSSFWorkbook(excelFileToRead);
		XSSFSheet sheet = wb.getSheet(sheetName);
		XSSFRow row;
		XSSFCell cell;

		Iterator<Row> rows = sheet.rowIterator();
		while (rows.hasNext()) {
			row = (XSSFRow) rows.next();

			if (row.getRowNum() > 0) {
				Iterator<Cell> cells = row.cellIterator();
				List<Integer> l = new ArrayList<>();
				while (cells.hasNext()) {
					cell = (XSSFCell) cells.next();
					int i = cell.getColumnIndex();
					
					if(i>=5){
//						int index = i-5;
						double value = cell.getNumericCellValue();
						l.add((int)value);
					}
				}
				
				list.add(l);
			}
		}
		
		return list;
	}
	
}
