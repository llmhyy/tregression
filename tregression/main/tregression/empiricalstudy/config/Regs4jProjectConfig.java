package tregression.empiricalstudy.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import tregression.empiricalstudy.TestCase;

public class Regs4jProjectConfig extends MavenProjectConfig {
    private static final String TEST_FILE_NAME = "tests.txt";

    private Regs4jProjectConfig(String srcTestFolder, String srcSourceFolder, String bytecodeTestFolder,
            String bytecodeSourceFolder, String buildFolder, String projectName, String bugID) {
        super(srcTestFolder, srcSourceFolder, bytecodeTestFolder, bytecodeSourceFolder, buildFolder, projectName,
                bugID);
    }

    public static Regs4jProjectConfig getConfig(String projectName, String regressionID) {
        return new Regs4jProjectConfig(PATH_MAVEN_TEST, PATH_MAVEN_SRC, PATH_MAVEN_BUILD_TEST, PATH_MAVEN_BUILD_SRC, PATH_MAVEN_BUILD, projectName,
                regressionID);
    }

    @Override
    public List<TestCase> retrieveFailingTestCase(String buggyVersionPath) throws IOException {
        byte[] encoded = Files.readAllBytes(getTestFilePath(buggyVersionPath));
        String testCaseStr = new String(encoded);
        List<TestCase> result = new ArrayList<>();
        String[] testClassAndName = testCaseStr.split("#");
        String testMethod = testClassAndName[1].replaceAll("\r", "");
        testMethod = testMethod.replace("\n", "");
        result.add(new TestCase(testClassAndName[0], testMethod));
        return result;
    }

    public static Path getTestFilePath(String buggyVersionPath) {
        return Paths.get(buggyVersionPath, TEST_FILE_NAME);
    }
}