package tregression.auto.result;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.*;

public class RunResult {
	
	public String projectName = " ";
	public int bugID = -1;
	public long traceLen = -1;
	public long rootCauseOrder = -1;
	public boolean isOmissionBug = false;
	public String solutionName = " ";
	public String errorMessage = " ";
	
	public final static String DELIMITER = ",";
	
	public static void main(String[] args) {
		final String path = "E:\\david\\Mutation_Dataset\\result.txt";
		List<RunResult> successCases = new ArrayList<>();
		System.out.println("Reading ... ");
		try (BufferedReader br = new BufferedReader(new FileReader(path))) {
			String line;
			while ((line = br.readLine()) != null) {
				RunResult result = RunResult.parseString(line);
				if (result.errorMessage.equals(" ")) {
					successCases.add(result);
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println("Writting ...");
		final String outputPath = "E:\\david\\Mutation_Dataset\\good_cases.txt";
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputPath))) {
			for (RunResult result : successCases) {
				bw.write(result.toString());
				bw.newLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println("Finish");
	}
	
	
	public RunResult() {
		
	}
	
	public RunResult(final RunResult result) {
		this.projectName = result.projectName;
		this.bugID = result.bugID;
		this.traceLen = result.traceLen;
		this.rootCauseOrder = result.rootCauseOrder;
		this.isOmissionBug = result.isOmissionBug;
		this.solutionName = result.solutionName;
		this.errorMessage = result.errorMessage;
	}
	
	public boolean isSuccess() {
		return this.traceLen != -1;
	}
	
	public static RunResult parseString(final String string) {
		RunResult result = new RunResult();
		String[] tokens = string.split(RunResult.DELIMITER);
		
		final String projName = tokens[0];
		result.projectName = projName == " " ? null : projName;
		
		final String bugID_str = tokens[1];
		result.bugID = Integer.valueOf(bugID_str);
		
		final String traceLen_str = tokens[2];
		result.traceLen = Integer.valueOf(traceLen_str);
		
		final String rootCauseOrder_str = tokens[3];
		result.rootCauseOrder = Integer.valueOf(rootCauseOrder_str);
		
		final String isOmissionBug_str = tokens[4];
		result.isOmissionBug = Boolean.valueOf(isOmissionBug_str);
		
		final String solutionName = tokens[5];
		result.solutionName = solutionName == " " ? null : solutionName;
		
		final String errMsg = tokens[6];
		result.errorMessage = errMsg == " " ? null : errMsg;
		
		return result;
	}
	
	@Override
	public String toString() {
		StringBuilder strBuilder = new StringBuilder();
		this.appendStr(strBuilder, this.projectName);
		this.appendStr(strBuilder, String.valueOf(this.bugID));
		this.appendStr(strBuilder, String.valueOf(this.traceLen));
		this.appendStr(strBuilder, String.valueOf(this.rootCauseOrder));
		this.appendStr(strBuilder, String.valueOf(this.isOmissionBug));
		this.appendStr(strBuilder, this.solutionName);
		this.appendStr(strBuilder, this.errorMessage);
		return strBuilder.toString();
	}
	
	protected void appendStr(final StringBuilder buffer, final String string) {
		buffer.append(string);
		buffer.append(RunResult.DELIMITER);
	}

	public String getFormattedInfo() {
		StringBuilder builder = new StringBuilder();
		builder.append("--------------------------------\n");
		builder.append("ProjectName: " + this.projectName + "\n");
		builder.append("Bug ID: " + this.bugID + "\n");
		builder.append("Length: " + this.traceLen + "\n");
		builder.append("Root Cause Order: " + this.rootCauseOrder + "\n");
		builder.append("isOmissionBug: " + this.isOmissionBug + "\n");
		builder.append("SolutationName: " + this.solutionName + "\n");
		builder.append("Error Message: " + this.errorMessage + "\n");
		builder.append("--------------------------------\n");
		return builder.toString();
	}
}
