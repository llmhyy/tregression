package tregression.empiricalstudy.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
	
	protected void retrieveDependencies() {
		this.dependencies = new ArrayList<String>();
	}
	
	public static Defects4jProjectConfig getConfig(String projectDirectory, String projectName, String regressionId) {
		Defects4jProjectConfig config = null;
		TregressionProperties prop = new TregressionProperties();
		try {
			FileInputStream input = new FileInputStream(projectDirectory + File.separator + "tregression.properties");
			prop.load(input);
		} catch (FileNotFoundException e) {
			System.out.println("Properties file not found. Exiting");
			return config;
		} catch (IOException e) {
			System.out.println("Properties file corrupted. Exiting");
			return config;
		}
		
		String buildDirectory = prop.getProperty("bin_test").split("/")[0];
		config = new Defects4jProjectConfig(prop.getProperty("src_test"),
											prop.getProperty("src_class"),
											prop.getProperty("bin_test"),
											prop.getProperty("bin_class"),
											buildDirectory, projectName, regressionId);
		return config;
	}
}
