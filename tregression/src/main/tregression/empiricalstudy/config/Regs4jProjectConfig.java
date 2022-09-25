package tregression.empiricalstudy.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import tregression.constants.Dataset;
import tregression.empiricalstudy.TestCase;
import tregression.handler.paths.PathConfigurationFactory;
import tregression.handler.paths.Regs4jPathConfiguration;

public class Regs4jProjectConfig extends MavenProjectConfig {
	private final static String TEST_FILE_NAME = "tests.txt";
	private Regs4jProjectConfig(String srcTestFolder, String srcSourceFolder, String bytecodeTestFolder,
			String bytecodeSourceFolder, String buildFolder, String projectName, String bugID) {
		super(srcTestFolder, srcSourceFolder, bytecodeTestFolder, bytecodeSourceFolder, buildFolder, projectName, bugID);
	}
	
	public static Regs4jProjectConfig getConfig(String projectName, String regressionID) {
		return new Regs4jProjectConfig(TEST_DIR, 
				SRC_DIR, 
				TEST_CLASS_DIR, 
				SRC_CLASS_DIR, 
				CLASS_DIR, 
				projectName, 
				regressionID);
	}
	
	@Override
	public List<TestCase> retrieveFailingTestCase(String buggyVersionPath) throws IOException {
		Regs4jPathConfiguration regs4jPathConfiguration = (Regs4jPathConfiguration) PathConfigurationFactory.createPathConfiguration(Dataset.REGS4J);
		String projectName = regs4jPathConfiguration.getProjectName(buggyVersionPath);
	    String bugId = regs4jPathConfiguration.getBugId(buggyVersionPath);
	    String pathWithTestFile = regs4jPathConfiguration.getBugPath(projectName, bugId);
	    byte[] encoded = Files.readAllBytes(Paths.get(pathWithTestFile, TEST_FILE_NAME));
	    String testCaseStr = new String(encoded);
		List<TestCase> result = new ArrayList<>();
		String[] testClassAndName = testCaseStr.split("#");
		String testMethod = testClassAndName[1].replaceAll("\r", "");
		testMethod = testMethod.replaceAll("\n", "");
		result.add(new TestCase(testClassAndName[0], testClassAndName[1]));
//		result.add(new TestCase("com.univocity.parsers.issues.github.Github_420", "detectedFormatTest"));
//		result.add(new TestCase("com.univocity.parsers.annotations.AnnotationHelperTest", "shouldCreateAnnotationHelper"));
		return result;
	}
	
}
