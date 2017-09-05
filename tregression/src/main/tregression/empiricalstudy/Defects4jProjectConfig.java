package tregression.empiricalstudy;

public class Defects4jProjectConfig {
	public String srcTestFolder;
	public String srcSourceFolder;
	public String bytecodeTestFolder;
	public String bytecodeSourceFolder;
	public String buildFolder;
	
	public String projectName;
	public int bugID;
	
	public String rootPath = "/home/linyun/doc/git_space/defects4j/framework/bin/defects4j";
	public String javaHome = "/home/linyun/java/jdk1.7.0_76/jre";
	

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
			config = new Defects4jProjectConfig("test", "src", "build/test", "build/classes", "build", projectName, bugID);
		}
		else if (projectName.equals("Lang")) {
			if(bugID<42){
				config = new Defects4jProjectConfig("src/test/java", "src/main/java", "target/tests", "target/classes", "target", projectName, bugID);				
			}
			else{
				config = new Defects4jProjectConfig("src/test", "src/java", "target/tests", "target/classes", "target", projectName, bugID);
			}
		}
		else if (projectName.equals("Math")) {
			if(bugID<85){
				config = new Defects4jProjectConfig("src/test/java", "src/main/java", "target/test-classes", "target/classes", "target", projectName, bugID);	
			}
			else{
				config = new Defects4jProjectConfig("src/test", "src/java", "target/test-classes", "target/classes", "target", projectName, bugID);
			}
		}
		else if (projectName.equals("Mockito")) {
			config = new Defects4jProjectConfig("test", "src", "build/classes/test", "build/classes/main", "build", projectName, bugID);
		}
		else if (projectName.equals("Time")) {
			if(bugID<12){
				config = new Defects4jProjectConfig("src/test/java", "src/main/java", "target/test-classes", "target/classes", "target", projectName, bugID);				
			}
			else{
				config = new Defects4jProjectConfig("src/test/java", "src/main/java", "build/tests", "build/classes", "build", projectName, bugID);
			}
		}
		
		return config;
	}
}
