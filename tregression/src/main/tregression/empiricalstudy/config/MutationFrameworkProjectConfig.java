package tregression.empiricalstudy.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import tregression.constants.Dataset;
import tregression.empiricalstudy.TestCase;
import tregression.handler.paths.MutationFrameworkPathConfiguration;
import tregression.handler.paths.PathConfigurationFactory;

public class MutationFrameworkProjectConfig extends MavenProjectConfig {
	private final static String TEST_FILE_NAME = "testcase.txt";
	private final static String ROOT_CAUSE_NAME = "rootcause.txt";
	private MutationFrameworkProjectConfig(String srcTestFolder, String srcSourceFolder, String bytecodeTestFolder,
			String bytecodeSourceFolder, String buildFolder, String projectName, String bugID) {
		super(srcTestFolder, srcSourceFolder, bytecodeTestFolder, bytecodeSourceFolder, buildFolder, projectName, bugID);
	}
	
	public static MutationFrameworkProjectConfig getConfig(String projectName, String regressionID) {
		return new MutationFrameworkProjectConfig(TEST_DIR, 
				SRC_DIR, 
				TEST_CLASS_DIR, 
				SRC_CLASS_DIR, 
				CLASS_DIR, 
				projectName, 
				regressionID);
	}
	
	@Override
	public List<TestCase> retrieveFailingTestCase(String buggyVersionPath) throws IOException {
		MutationFrameworkPathConfiguration mutationFrameworkPathConfiguration = (MutationFrameworkPathConfiguration) PathConfigurationFactory.createPathConfiguration(Dataset.MUTATION_FRAMEWORK);
		String projectName = mutationFrameworkPathConfiguration.getProjectName(buggyVersionPath);
	    String bugId = mutationFrameworkPathConfiguration.getBugId(buggyVersionPath);
	    String pathWithTestFile = mutationFrameworkPathConfiguration.getBugPath(projectName, bugId);
	    byte[] encoded = Files.readAllBytes(Paths.get(pathWithTestFile, TEST_FILE_NAME));
	    String testCaseStr = new String(encoded);
		List<TestCase> result = new ArrayList<>();
		String[] testClassAndName = testCaseStr.split("#");
		String testMethod = testClassAndName[1].replaceAll("\r", "");
		testMethod = testMethod.replaceAll("\n", "");
		testMethod = testMethod.substring(0, testMethod.indexOf("("));
		result.add(new TestCase(testClassAndName[0], testMethod));
		return result;
	}
}
