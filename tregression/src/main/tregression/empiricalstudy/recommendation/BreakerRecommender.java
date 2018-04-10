package tregression.empiricalstudy.recommendation;

import java.io.File;

import sav.common.core.SavException;
import tregression.empiricalstudy.training.DeadEndData;

public class BreakerRecommender {
	public void recommend(DeadEndData record) throws SavException {
		RecordWriter inputWriter = new RecordWriter();
		PredictionOutputReader outputReader = new PredictionOutputReader();
		PythonVmRunner vmRunner = new PythonVmRunner(inputWriter, outputReader);
		try {
			String pythonHome = "C:\\Program Files\\Python36";
			pythonHome = pythonHome + File.separator + "python.exe";
			String workingDir = "E:\\linyun\\git_space\\train";
			String prediction = workingDir + File.separator + "prediction_server.py";
			
			
			vmRunner.start(pythonHome, prediction, workingDir);
//			String input = record.getPlainText("-1", "-1");
//			System.out.println(input);
			inputWriter.sendData(record);
			PythonOutput output = outputReader.readOutput();
			System.out.println(output);
			
		} finally {
			vmRunner.stop();
		}
	}
}
