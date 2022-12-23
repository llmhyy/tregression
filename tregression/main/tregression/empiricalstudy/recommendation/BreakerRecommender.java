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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import sav.common.core.SavException;
import tregression.empiricalstudy.training.ControlDeadEndData;
import tregression.empiricalstudy.training.DataDeadEndData;
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
	
	/**
	 * return a sorted list with size at most {@code limit}.
	 * @param records
	 * @param buggyTrace
	 * @param limit
	 * @return
	 * @throws SavException
	 * @throws IOException
	 */
	public List<TraceNode> recommend(List<DeadEndData> records, Trace buggyTrace, int limit) throws SavException, IOException {
		
		List<TraceNode> list = new ArrayList<>();
		
		String pythonHome = new PythonConfig().pythonHome;
		pythonHome = pythonHome + File.separator + "python.exe";
		String workingDir = new PythonConfig().workingDir;
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
		
		String type = "";
		if(!records.isEmpty()){
			DeadEndData record = records.get(0);
			if(record instanceof ControlDeadEndData){
				type = "control";
			}
			else{
				DataDeadEndData dd = (DataDeadEndData)record;
				type = dd.getDataType();
			}
			writer.write(type+'\n');
			writer.flush();
		}
		
		for(DeadEndData record: records){
//			System.out.println(record.getPlainText("1", "1"));
			
			writer.write(record.getPlainText("1", "1"));
			writer.flush();
			
			StringBuffer buffer = new StringBuffer();
			String line = null;
			boolean recording = false;
			while ((line = reader.readLine ()) != null) {
//				System.out.println ("Stdout: " + line);
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
			
//			if(result.isEmpty()){
//				continue;
//			}
			String doubleString = "0.0";
			try{
				doubleString = result.substring(result.indexOf("[[")+2, result.indexOf("]]"));
			}
			catch(Exception e){
//				e.printStackTrace();
				System.out.println("type: " + type);
				stderr = process.getErrorStream();
				containError = printErrorStream(stderr);
				if(containError){
					return list;
				}
				break;
			}
			double prob = Double.valueOf(doubleString);
			
			TraceNode node = buggyTrace.getTraceNode(record.traceOrder);
			node.setSliceBreakerProbability(prob);
			list.add(node);
		}
		
		process.destroy();
		
		Collections.sort(list, new Comparator<TraceNode>() {
			@Override
			public int compare(TraceNode o1, TraceNode o2) {
				double comp = o1.getSliceBreakerProbability() - o2.getSliceBreakerProbability();
				if(comp<0){
					return 1;
				}
				else if(comp>0){
					return -1;
				}
				else{
					return 0;
				}
			}
		});
		
		List<TraceNode> sortedList = new ArrayList<>();
		int size = (limit < list.size())? limit : list.size();
		for(int i=0; i<size; i++){
			sortedList.add(list.get(i));
		}
		
		return sortedList;
	}
}
