package tregression.empiricalstudy.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import tregression.empiricalstudy.TestCase;

public class Defects4jProjectConfig extends ProjectConfig{
	
	private Defects4jProjectConfig(String srcTestFolder, String srcSourceFolder, String bytecodeTestFolder,
			String bytecodeSourceFolder, String buildFolder, String projectName, String bugID) {
		super(srcTestFolder, srcSourceFolder, bytecodeTestFolder, bytecodeSourceFolder, buildFolder, projectName, bugID);
	}
	
	public String rootPath = ""+File.separator+"home"+File.separator+"linyun"+File.separator+"doc"+File.separator+"git_space"+File.separator+"defects4j"+File.separator+"framework"+File.separator+"bin"+File.separator+"defects4j";

	public static Defects4jProjectConfig getConfig(String projectName, String regressionID) {
		int bugID = Integer.valueOf(regressionID);
		Defects4jProjectConfig config = null;
		if(projectName.equals("Chart")) {
			config = new Defects4jProjectConfig("tests", "source", "build-tests", "build", "build", projectName, regressionID);
		}
		else if (projectName.equals("Closure")) {
			config = new Defects4jProjectConfig("test", "src", "build"+File.separator+"test", "build"+File.separator+"classes", "build", projectName, regressionID);
		}
		else if (projectName.equals("Lang")) {
			if(bugID<21){
				config = new Defects4jProjectConfig("src"+File.separator+"test"+File.separator+"java", "src"+File.separator+"main"+File.separator+"java", "target"+File.separator+"tests", "target"+File.separator+"classes", "target", projectName, regressionID);
			}
			else if(bugID<42){
				config = new Defects4jProjectConfig("src"+File.separator+"test"+File.separator+"java", "src"+File.separator+"main"+File.separator+"java", "target"+File.separator+"test-classes", "target"+File.separator+"classes", "target", projectName, regressionID);				
			}
			else{
				config = new Defects4jProjectConfig("src"+File.separator+"test", "src"+File.separator+"java", "target"+File.separator+"tests", "target"+File.separator+"classes", "target", projectName, regressionID);
			}
			
			if(bugID>=36 && bugID<=41){
				config.srcSourceFolder = "src"+File.separator+"java";
				config.srcTestFolder = "src"+File.separator+"test";
			}
		}
		else if (projectName.equals("Math")) {
			if(bugID<85){
				config = new Defects4jProjectConfig("src"+File.separator+"test"+File.separator+"java", "src"+File.separator+"main"+File.separator+"java", "target"+File.separator+"test-classes", "target"+File.separator+"classes", "target", projectName, regressionID);	
			}
			else{
				config = new Defects4jProjectConfig("src"+File.separator+"test", "src"+File.separator+"java", "target"+File.separator+"test-classes", "target"+File.separator+"classes", "target", projectName, regressionID);
			}
		}
		else if (projectName.equals("Mockito")) {
			if(bugID<12 || bugID==20 || bugID==21 || bugID==18 || bugID==19){
				config = new Defects4jProjectConfig("test", "src", "build"+File.separator+"classes"+File.separator+"test", "build"+File.separator+"classes"+File.separator+"main", "build", projectName, regressionID);				
			}
			else{
				config = new Defects4jProjectConfig("test", "src", "target"+File.separator+"test-classes", "target"+File.separator+"classes", "target", projectName, regressionID);
			}
			
			List<String> addSrcList = new ArrayList<>();
			addSrcList.add("mockmaker" + File.separator + "bytebuddy"+ File.separator + "main" + File.separator + "java");
			addSrcList.add("mockmaker" + File.separator + "bytebuddy"+ File.separator + "test" + File.separator + "java");
			addSrcList.add("mockmaker" + File.separator + "cglib"+ File.separator + "main" + File.separator + "java");
			addSrcList.add("mockmaker" + File.separator + "cglib"+ File.separator + "test" + File.separator + "java");
			config.additionalSourceFolder = addSrcList;
		}
		else if (projectName.equals("Time")) {
			if(bugID<12){
				config = new Defects4jProjectConfig("src"+File.separator+"test"+File.separator+"java", "src"+File.separator+"main"+File.separator+"java", "target"+File.separator+"test-classes", "target"+File.separator+"classes", "target", projectName, regressionID);				
			}
			else{
				config = new Defects4jProjectConfig("src"+File.separator+"test"+File.separator+"java", "src"+File.separator+"main"+File.separator+"java", "build"+File.separator+"tests", "build"+File.separator+"classes", "build", projectName, regressionID);
			}
		}
		
		return config;
	}
	
	@Override
	public List<TestCase> retrieveFailingTestCase(String buggyVersionPath) throws IOException {
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
	
}
