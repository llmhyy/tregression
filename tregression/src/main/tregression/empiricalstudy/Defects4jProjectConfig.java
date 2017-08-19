package tregression.empiricalstudy;

public class Defects4jProjectConfig {
	public String srcTestFolder;
	public String srcSourceFolder;
	public String bytecodeTestFolder;
	public String bytecodeSourceFolder;
	public String buildFolder;
	

	public Defects4jProjectConfig(String srcTestFolder, String srcSourceFolder, String bytecodeTestFolder,
			String bytecodeSourceFolder, String buildFolder) {
		super();
		this.srcTestFolder = srcTestFolder;
		this.srcSourceFolder = srcSourceFolder;
		this.bytecodeTestFolder = bytecodeTestFolder;
		this.bytecodeSourceFolder = bytecodeSourceFolder;
		this.buildFolder = buildFolder;
	}
	
	public static Defects4jProjectConfig getD4JConfig(String projectName, int bugID) {
		if(projectName.equals("Chart")) {
			return new Defects4jProjectConfig("tests", "source", "build-tests", "build", "build");
		}
		else if (projectName.equals("Closure")) {
			return new Defects4jProjectConfig("test", "src", "build/test", "build/classes", "build");
		}
		else if (projectName.equals("Lang")) {
			if(bugID<42){
				return new Defects4jProjectConfig("src/test/java", "src/main/java", "target/tests", "target/classes", "target");				
			}
			else{
				return new Defects4jProjectConfig("src/test", "src/java", "target/tests", "target/classes", "target");
			}
		}
		else if (projectName.equals("Math")) {
			if(bugID<85){
				return new Defects4jProjectConfig("src/test/java", "src/main/java", "target/test-classes", "target/classes", "target");	
			}
			else{
				return new Defects4jProjectConfig("src/test", "src/java", "target/test-classes", "target/classes", "target");
			}
		}
		else if (projectName.equals("Mockito")) {
			return new Defects4jProjectConfig("test", "src", "build/classes/test", "build/classes/main", "build");
		}
		else if (projectName.equals("Time")) {
			return new Defects4jProjectConfig("src/test/java", "src/main/java", "target/test-classes", "target/classes", "target");
		}
		
		return null;
	}
}
