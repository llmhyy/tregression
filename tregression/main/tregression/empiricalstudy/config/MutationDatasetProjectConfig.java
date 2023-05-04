package tregression.empiricalstudy.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import tregression.empiricalstudy.TestCase;

import static jmutation.dataset.bug.creator.BuggyProjectCreator.TESTCASE_FILE_NAME;

public class MutationDatasetProjectConfig extends MavenProjectConfig {

	public MutationDatasetProjectConfig(String srcTestFolder, String srcSourceFolder, String bytecodeTestFolder,
			String bytecodeSourceFolder, String buildFolder, String projectName, String regressionID) {
		super(srcTestFolder, srcSourceFolder, bytecodeTestFolder, bytecodeSourceFolder, buildFolder, projectName, regressionID);
	}
	
	@Override
	public List<TestCase> retrieveFailingTestCase(String buggyVersionPath) throws IOException {
		String failingTestFilePathStr = String.join(File.separator, buggyVersionPath, "..", TESTCASE_FILE_NAME);
		Path failingTestFilePath = Paths.get(failingTestFilePathStr);
		String testCaseStr = Files.readString(failingTestFilePath);
        // Example: org.apache.commons.math.analysis.ComposableFunctionTest#testComposition(),54,102
        int idxOfPound = testCaseStr.indexOf("#");
        String testClassName = testCaseStr.substring(0, idxOfPound);
        String testMethodName = testCaseStr.substring(idxOfPound + 1, testCaseStr.indexOf("("));
        TestCase testCase = new TestCase(testClassName, testMethodName);
        List<TestCase> result = new ArrayList<>();
        result.add(testCase);
		return result;
	}
	
    public static ProjectConfig getConfig(String projectName, String regressionID) {
        return new MutationDatasetProjectConfig(PATH_MAVEN_TEST,
        		PATH_MAVEN_SRC, PATH_MAVEN_BUILD_TEST,
        		PATH_MAVEN_BUILD_SRC, PATH_MAVEN_BUILD, projectName, regressionID);
    }
}
