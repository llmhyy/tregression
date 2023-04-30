package defects4janalysis;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ResultWriter {
	
	private final String path;
	
	public ResultWriter(final String path) {
		this.path = path;
	}
	
	public void writeTitle() {
		FileWriter fileWriter = null;
		try {
			File file = new File(this.path);
			file.createNewFile();
			fileWriter = new FileWriter(file);
			
			StringBuffer strBuffer = new StringBuffer();
			this.appendStr(strBuffer, "Project Name");
			this.appendStr(strBuffer, "Bug ID");
			this.appendStr(strBuffer, "Trace Length");
//			this.appendStr(strBuffer, "Trail Type");
			this.appendStr(strBuffer, "Root Cause Order");
			this.appendStr(strBuffer, "Is Omission");
			this.appendStr(strBuffer, "Solution Name");
			this.appendStr(strBuffer, "Error Message");
	
			fileWriter.write(strBuffer.toString() + System.lineSeparator());
			
		} catch (Exception e) {
			e.printStackTrace();
		}finally {
			try {
				if (fileWriter != null) {
					fileWriter.flush();
					fileWriter.close();					
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void writeResult(final RunResult result) {
		System.out.println("Writing result to file:" + this.path);
		FileWriter fileWriter = null;
		try {
			File file = new File(this.path);
			file.createNewFile();
			fileWriter = new FileWriter(file, true);
			
			StringBuffer strBuffer = new StringBuffer();
			this.appendStr(strBuffer, result.projectName);
			this.appendStr(strBuffer, String.valueOf(result.bugID));
			this.appendStr(strBuffer, String.valueOf(result.traceLen));
//			this.appendStr(strBuffer, String.valueOf(result.trailType));
			this.appendStr(strBuffer, String.valueOf(result.rootCauseOrder));
			this.appendStr(strBuffer, String.valueOf(result.isOmissionBug));
			this.appendStr(strBuffer, result.solutionName);
			this.appendStr(strBuffer, result.errorMessage);
			fileWriter.write(strBuffer.toString() + System.lineSeparator());
			
		} catch (Exception e) {
			e.printStackTrace();
		}finally {
			try {
				if (fileWriter != null) {
					fileWriter.flush();
					fileWriter.close();					
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void writeResult(final int successCount, final int totalCount) {
		System.out.println("Writing summary to file:" + this.path);
		FileWriter fileWriter = null;
		try {
			File file = new File(this.path);
			file.createNewFile();
			fileWriter = new FileWriter(file, true);
			
			StringBuffer strBuffer = new StringBuffer();
			this.appendStr(strBuffer, String.valueOf(successCount));
			this.appendStr(strBuffer, String.valueOf(totalCount));
			
			fileWriter.write(strBuffer.toString() + "\n");
			
		} catch (Exception e) {
			e.printStackTrace();
		}finally {
			try {
				if (fileWriter != null) {
					fileWriter.flush();
					fileWriter.close();					
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	
	public void appendStr(final StringBuffer buffer, final String string) {
		buffer.append(string);
		buffer.append(",");
	}
}
