package tregression.empiricalstudy.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import core.MysqlManager;
import model.Regression;
import tregression.constants.Dataset;
import tregression.empiricalstudy.TestCase;
import tregression.handler.paths.PathConfigurationFactory;
import tregression.handler.paths.Regs4jPathConfiguration;
import com.google.protobuf.compiler.PluginProtos ;
import com.mysql.cj.jdbc.Driver;
import org.eclipse.jdt.internal.jarinjarloader.*;

public class Regs4jProjectConfig extends MavenProjectConfig {
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
		// Use MysqlManager from regs4j.jar to get regressions
	    List<Regression> regressionList = MysqlManager.selectRegressions("select bfc,buggy,bic,work,testcase from regressions where project_full_name='" + projectName + "'");
		String bugId = regs4jPathConfiguration.getBugId(buggyVersionPath);
		Regression regression = regressionList.get(Integer.parseInt(bugId) - 1);
		String testCaseStr = regression.getTestCase();
//		result.add(new TestCase("com.univocity.parsers.issues.github.Github_420", "detectedFormatTest"));
//		result.add(new TestCase("com.univocity.parsers.annotations.AnnotationHelperTest", "shouldCreateAnnotationHelper"));
		List<TestCase> result = new ArrayList<>();
		String[] testClassAndName = testCaseStr.split("#");
		result.add(new TestCase(testClassAndName[0], testClassAndName[1]));
		return result;
	}
	
}
