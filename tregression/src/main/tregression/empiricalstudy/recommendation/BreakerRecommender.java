package tregression.empiricalstudy.recommendation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import sav.common.core.SavException;
import tregression.empiricalstudy.training.DeadEndData;

public class BreakerRecommender {
	private boolean printErrorStream(InputStream error) {
		BufferedReader reader = new BufferedReader (new InputStreamReader(error));
		String line = null;
		int avail = 0;
		try {
			avail = error.available();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		if(avail==0){
			return false;
		}
		
    	try {
			while ((line = reader.readLine ()) != null) {
				System.err.println ("Std error: " + line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
    	return true;
	}
	
	public List<TraceNode> recommend(List<DeadEndData> records, Trace buggyTrace, int limit) throws SavException, IOException {
		
		List<TraceNode> list = new ArrayList<>();
		
		String pythonHome = "C:\\Program Files\\Python36";
		pythonHome = pythonHome + File.separator + "python.exe";
		String workingDir = "E:\\linyun\\git_space\\train";
		String predictionFile = workingDir + File.separator + "prediction_server.py";
		
		List<String> commands = new ArrayList<String>();
		commands.add(pythonHome);
		commands.add(predictionFile);
		
		ProcessBuilder processBuilder = new ProcessBuilder(commands);
		if(workingDir!=null){
			File workingDirFile = new File(workingDir);
			if(workingDirFile.exists()){
				processBuilder.directory(workingDirFile);
			}
		}
		
		Process process = processBuilder.start();
		processBuilder.redirectErrorStream(true);
		
		InputStream stderr = process.getErrorStream();
		boolean containError = printErrorStream(stderr);
		if(containError){
			return list;
		}
		
		OutputStream stdin = process.getOutputStream();
		InputStream stdout = process.getInputStream();
		
		BufferedReader reader = new BufferedReader (new InputStreamReader(stdout));
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stdin));
		
		for(DeadEndData record: records){
			System.out.println(record.getPlainText("1", "1"));
			
			writer.write(record.getPlainText("1", "1"));
			writer.flush();
			
			StringBuffer buffer = new StringBuffer();
			String line = null;
			boolean recording = false;
			while ((line = reader.readLine ()) != null) {
				System.out.println ("Stdout: " + line);
				if(line.contains("@@PythonEnd@@")){
					break;
				}
				
				if(recording){
					buffer.append(line);					
				}
				
				if(line.contains("@@PythonStart@@")){
					recording = true;
				}
				
			}
			
			String result = buffer.toString();
			
			String doubleString = result.substring(result.indexOf("[[")+2, result.indexOf("]]"));
			double prob = Double.valueOf(doubleString);
			
			TraceNode node = buggyTrace.getTraceNode(record.traceOrder);
			node.setSliceBreakerProbability(prob);
			
			if(list.size()<limit){
				list.add(node);
			}
			else{
				int lowProIndex = 0;
				TraceNode lowProNode = list.get(0);
				if(list.get(1).getSliceBreakerProbability()<lowProNode.getSliceBreakerProbability()){
					lowProNode = list.get(1);
					lowProIndex = 1;
				}
				
				if(list.get(2).getSliceBreakerProbability()<lowProNode.getSliceBreakerProbability()){
					lowProNode = list.get(2);
					lowProIndex = 2;
				}
				
				if(node.getSliceBreakerProbability()>lowProNode.getSliceBreakerProbability()){
					list.set(lowProIndex, node);
				}
			}
		}
		
		process.destroy();
		
		return list;
	}
}
