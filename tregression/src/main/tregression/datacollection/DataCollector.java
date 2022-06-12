package tregression.datacollection;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import microbat.Activator;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.stepvectorizer.StepVector;
import microbat.stepvectorizer.StepVectorizer;
import tregression.StepChangeType;
import tregression.StepChangeTypeChecker;
import tregression.empiricalstudy.RootCauseFinder;
import tregression.preference.TregressionPreference;
import tregression.views.BuggyTraceView;

/**
 * DataCollector is used to collect the feedback and the step vector for model training.
 * @author David
 *
 */
public class DataCollector {
	private BuggyTraceView buggyView;
	
	private Trace buggyTrace;
	private Trace correctTrace;
	
	private RootCauseFinder finder;
	
	private final String folderPath = "C:/Users/arkwa/Documents/NUS/Dissertation/Classifier/Feedback/data/";
	
	private final String feedbackFolderName = "Feedbacks";
	private final String vectorFolderName = "StepVectorizations";
	
	public DataCollector(BuggyTraceView buggyView, Trace correctTrace, RootCauseFinder finder) {
		this.buggyView = buggyView;
		this.buggyTrace = this.buggyView.getTrace();
		this.correctTrace = correctTrace;
		this.finder = finder;
	}
	
	public void exportFeedbackData(List<TraceNode> nodes, String id, String projectName) {
	
		// Create directory if don't exist
		Path feedbackDirPath = Paths.get(this.folderPath, this.feedbackFolderName);
		feedbackDirPath.toFile().mkdir();
		
		Path projectDirPath = Paths.get(this.folderPath, this.feedbackFolderName, projectName);
		projectDirPath.toFile().mkdir();
		
		String fileName = this.constructFeedbackFileName(projectName, id);
		Path fullPath = Paths.get(this.folderPath, this.feedbackFolderName, projectName, fileName);

		StepChangeTypeChecker typeChecker = new StepChangeTypeChecker(this.buggyTrace, this.correctTrace);
		StepVectorizer vectorizer = new StepVectorizer(this.buggyTrace);
		
		try {
			File file = fullPath.toFile();
			file.createNewFile();
			FileWriter writer = new FileWriter(file);
			for (TraceNode node : nodes) {
				StepChangeType type = typeChecker.getType(node, true, this.buggyView.getPairList(), buggyView.getDiffMatcher());
				if (type.getType() == StepChangeType.DAT) {
					VarValue wrongVar = type.getWrongVariable(node, true, this.finder);
					int varIndex = vectorizer.getVarIndex(wrongVar);
					writer.write(projectName + "," + id + "," + node.getOrder() + "," + type.getType() + "," + varIndex + '\n');
				} else {
					writer.write(projectName + "," + id + "," + node.getOrder() + "," + type.getType() + '\n');
				}
			}
			writer.close();
		} catch (IOException e) {
			System.out.println("Failed when " + fullPath);
			e.printStackTrace();
		}
		
		System.out.println("Finished exporting the feedbacks");
	}
	
	public void exportVectorData(List<TraceNode> nodes, String id, String projectName) {
		
		// Create directory if don't exist
		Path vectorDirPath = Paths.get(this.folderPath, this.vectorFolderName);
		vectorDirPath.toFile().mkdir();
		
		Path projectDirPath = Paths.get(this.folderPath, this.vectorFolderName, projectName);
		projectDirPath.toFile().mkdir();
		
		String fileName = this.constructVectorFileName(projectName, id);
		Path fullPath = Paths.get(this.folderPath, this.vectorFolderName, projectName, fileName);
		
		StepVectorizer vectorizer = new StepVectorizer(this.buggyTrace);
		
		try {
			File file = fullPath.toFile();
			file.createNewFile();
			FileWriter writer = new FileWriter(file);
			for (TraceNode node : nodes) {
				StepVector vector = vectorizer.vectorize(node.getOrder());
				String data = vector.convertToCSV();
				writer.write(data + '\n');
			}
			writer.close();
		} catch (IOException e) {
			System.out.println("Failed when " + fullPath);
			e.printStackTrace();
		}
		System.out.println("Finished exporting the vectors");
	}
	
	private String constructFeedbackFileName(String projectName, String id) {
		return projectName + "_feedback_" + id + ".txt";
	}
	
	private String constructVectorFileName(String projectName, String id) {
		return projectName + "_vector_" + id + ".txt";
	}
	
}
