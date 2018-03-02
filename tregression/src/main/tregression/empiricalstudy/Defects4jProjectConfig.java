package tregression.empiricalstudy;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import microbat.Activator;
import microbat.preference.MicrobatPreference;

public class Defects4jProjectConfig {
	public String srcTestFolder;
	public String srcSourceFolder;
	public String bytecodeTestFolder;
	public String bytecodeSourceFolder;
	public String buildFolder;
	
	public List<String> additionalSourceFolder = new ArrayList<>();
	
	public String projectName;
	public int bugID;
	
	public String rootPath = ""+File.separator+"home"+File.separator+"linyun"+File.separator+"doc"+File.separator+"git_space"+File.separator+"defects4j"+File.separator+"framework"+File.separator+"bin"+File.separator+"defects4j";
	public String javaHome = Activator.getDefault().getPreferenceStore().getString(MicrobatPreference.JAVA7HOME_PATH);
	

	private Defects4jProjectConfig(String srcTestFolder, String srcSourceFolder, String bytecodeTestFolder,
			String bytecodeSourceFolder, String buildFolder, String projectName, int bugID) {
		super();
		this.srcTestFolder = srcTestFolder;
		this.srcSourceFolder = srcSourceFolder;
		this.bytecodeTestFolder = bytecodeTestFolder;
		this.bytecodeSourceFolder = bytecodeSourceFolder;
		this.buildFolder = buildFolder;
		this.projectName = projectName;
		this.bugID = bugID;
	}
	
	public static Defects4jProjectConfig getD4JConfig(String projectName, int bugID) {
		Defects4jProjectConfig config = null;
		if(projectName.equals("Chart")) {
			config = new Defects4jProjectConfig("tests", "source", "build-tests", "build", "build", projectName, bugID);
		}
		else if (projectName.equals("Closure")) {
			config = new Defects4jProjectConfig("test", "src", "build"+File.separator+"test", "build"+File.separator+"classes", "build", projectName, bugID);
		}
		else if (projectName.equals("Lang")) {
			if(bugID<21){
				config = new Defects4jProjectConfig("src"+File.separator+"test"+File.separator+"java", "src"+File.separator+"main"+File.separator+"java", "target"+File.separator+"tests", "target"+File.separator+"classes", "target", projectName, bugID);
			}
			else if(bugID<42){
				config = new Defects4jProjectConfig("src"+File.separator+"test"+File.separator+"java", "src"+File.separator+"main"+File.separator+"java", "target"+File.separator+"test-classes", "target"+File.separator+"classes", "target", projectName, bugID);				
			}
			else{
				config = new Defects4jProjectConfig("src"+File.separator+"test", "src"+File.separator+"java", "target"+File.separator+"tests", "target"+File.separator+"classes", "target", projectName, bugID);
			}
			
			if(bugID>=36 && bugID<=41){
				config.srcSourceFolder = "src"+File.separator+"java";
				config.srcTestFolder = "src"+File.separator+"test";
			}
		}
		else if (projectName.equals("Math")) {
			if(bugID<85){
				config = new Defects4jProjectConfig("src"+File.separator+"test"+File.separator+"java", "src"+File.separator+"main"+File.separator+"java", "target"+File.separator+"test-classes", "target"+File.separator+"classes", "target", projectName, bugID);	
			}
			else{
				config = new Defects4jProjectConfig("src"+File.separator+"test", "src"+File.separator+"java", "target"+File.separator+"test-classes", "target"+File.separator+"classes", "target", projectName, bugID);
			}
		}
		else if (projectName.equals("Mockito")) {
			if(bugID<12 || bugID==20 || bugID==21){
				config = new Defects4jProjectConfig("test", "src", "build"+File.separator+"classes"+File.separator+"test", "build"+File.separator+"classes"+File.separator+"main", "build", projectName, bugID);				
			}
			else{
				config = new Defects4jProjectConfig("test", "src", "target"+File.separator+"test-classes", "target"+File.separator+"classes", "target", projectName, bugID);
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
				config = new Defects4jProjectConfig("src"+File.separator+"test"+File.separator+"java", "src"+File.separator+"main"+File.separator+"java", "target"+File.separator+"test-classes", "target"+File.separator+"classes", "target", projectName, bugID);				
			}
			else{
				config = new Defects4jProjectConfig("src"+File.separator+"test"+File.separator+"java", "src"+File.separator+"main"+File.separator+"java", "build"+File.separator+"tests", "build"+File.separator+"classes", "build", projectName, bugID);
			}
		}
		
		return config;
	}
}
