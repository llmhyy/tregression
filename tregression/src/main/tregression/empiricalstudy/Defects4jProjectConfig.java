package tregression.empiricalstudy;

public class Defects4jProjectConfig {
	public String srcTestFolder;
	public String srcSourceFolder;
	public String bytecodeTestFolder;
	public String bytecodeSourceFolder;

	public Defects4jProjectConfig(String srcTestFolder, String srcSourceFolder, String bytecodeTestFolder,
			String bytecodeSourceFolder) {
		super();
		this.srcTestFolder = srcTestFolder;
		this.srcSourceFolder = srcSourceFolder;
		this.bytecodeTestFolder = bytecodeTestFolder;
		this.bytecodeSourceFolder = bytecodeSourceFolder;
	}
	
	public static Defects4jProjectConfig getD4JConfig(String projectName) {
		if(projectName.equals("Chart")) {
			return new Defects4jProjectConfig("tests", "source", "build-tests", "build");
		}
		else if (projectName.equals("Closure")) {
			return new Defects4jProjectConfig("test", "src", "build/classes", "build/test");
		}
		else if (projectName.equals("Lang")) {
			return new Defects4jProjectConfig("src/test/java", "src/main/java", "target/classes", "target/tests");
		}
		else if (projectName.equals("Match")) {
			return new Defects4jProjectConfig("src/test/java", "src/main/java", "target/classes", "target/test-classes");
		}
		else if (projectName.equals("Mockito")) {
			return new Defects4jProjectConfig("test", "src", "buildSrc/build/classes/main", "buildSrc/build/classes/test");
		}
		else if (projectName.equals("Time")) {
			return new Defects4jProjectConfig("src/test/java", "src/main/java", "target/classes", "target/test-classes");
		}
		
		return null;
	}
}
