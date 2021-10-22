package tregression.empiricalstudy.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import microbat.Activator;
import microbat.preference.MicrobatPreference;
import tregression.empiricalstudy.TestCase;

public abstract class ProjectConfig {
	
	public String javaHome = Activator.getDefault().getPreferenceStore().getString(MicrobatPreference.JAVA7HOME_PATH);
	public List<String> additionalSourceFolder = new ArrayList<>();
	
	public String srcTestFolder;
	public String srcSourceFolder;
	public String bytecodeTestFolder;
	public String bytecodeSourceFolder;
	public String buildFolder;
	
	public String projectName;
	public String regressionID;
	protected List<String> dependencies = null;
	
	public ProjectConfig(String srcTestFolder, String srcSourceFolder, String bytecodeTestFolder,
			String bytecodeSourceFolder, String buildFolder, String projectName, String regressionID) {
		this.srcTestFolder = srcTestFolder;
		this.srcSourceFolder = srcSourceFolder;
		this.bytecodeTestFolder = bytecodeTestFolder;
		this.bytecodeSourceFolder = bytecodeSourceFolder;
		this.buildFolder = buildFolder;
		this.projectName = projectName;
		this.regressionID = regressionID;
	}
	
	public static List<TestCase> retrieveFailingTestCase(String buggyVersionPath) throws IOException {
		String failingFile = buggyVersionPath + File.separator + "failing_tests";
		File file = new File(failingFile);

		BufferedReader reader = new BufferedReader(new FileReader(file));

		List<TestCase> list = new ArrayList<>();
		String line = null;
		while ((line = reader.readLine()) != null) {
			if (line.startsWith("---")) {
				String testClass = line.substring(line.indexOf(" ") + 1, line.indexOf("::"));
				String testMethod = line.substring(line.indexOf("::") + 2, line.length());
				System.currentTimeMillis();
				TestCase tc = new TestCase(testClass, testMethod);
				list.add(tc);
			}
		}
		reader.close();

		return list;
	}
	
	public List<String> getDependencies(){
		if (this.dependencies == null)
				this.retrieveDependencies();
		return this.dependencies;
	}
	
	protected abstract void retrieveDependencies();
}
