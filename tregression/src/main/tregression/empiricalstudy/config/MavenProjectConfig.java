package tregression.empiricalstudy.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.SystemUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

import tregression.empiricalstudy.TestCase;


public class MavenProjectConfig extends ProjectConfig {

	public final static String M2AFFIX = ".m2" + File.separator + "repository";
	protected final static String TEST_DIR = "src" + File.separator+"test" + File.separator + "test";
	protected final static String SRC_DIR = "src" + File.separator+"test" + File.separator + "java";
	protected final static String CLASS_DIR = "target";
	protected final static String SRC_CLASS_DIR = CLASS_DIR + File.separator + "classes";
	protected final static String TEST_CLASS_DIR = CLASS_DIR + File.separator + "test-classes";

	public MavenProjectConfig(String srcTestFolder, String srcSourceFolder, String bytecodeTestFolder,
			String bytecodeSourceFolder, String buildFolder, String projectName, String regressionID) {
		super(srcTestFolder, srcSourceFolder, bytecodeTestFolder, bytecodeSourceFolder, buildFolder, projectName,
				regressionID);
	}

	public static boolean check(String path) {

		File f = new File(path);
		if (f.exists() && f.isDirectory()) {
			for (String file : f.list()) {
				if (file.toString().equals("pom.xml")) {
					return true;
				}
			}
		}

		return false;
	}


	public static ProjectConfig getConfig(String projectName, String regressionID) {
		return new MavenProjectConfig(TEST_DIR, 
				SRC_DIR, 
				TEST_CLASS_DIR, 
				SRC_CLASS_DIR, 
				CLASS_DIR, 
				projectName, 
				regressionID);
	}
	
	public static List<String> getMavenDependencies(String path){
		String pomPath = path + File.separator + "pom.xml";
		File pomFile = new File(pomPath);

		try {
			List<String> dependencies = readAllDependency(pomFile);
			
			return dependencies;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return new ArrayList<String>();
	}

	@SuppressWarnings("unchecked")
	public static List<String> readAllDependency(File pom) throws Exception {
		MavenXpp3Reader mavenReader = new MavenXpp3Reader();
		Model pomModel = mavenReader.read(new FileReader(pom));
		List<Dependency> dependencies = pomModel.getDependencies();
		List<String> result = new ArrayList<>();
		String usrHomePath = getUserHomePath();
		for (Dependency dependency : dependencies) {
			StringBuilder sb = new StringBuilder(usrHomePath);
			sb.append(File.separator).append(M2AFFIX).append(File.separator)
					.append(dependency.getGroupId().replace(".", File.separator)).append(File.separator)
					.append(dependency.getArtifactId()).append(File.separator).append(dependency.getVersion())
					.append(File.separator).append(dependency.getArtifactId()).append("-")
					.append(dependency.getVersion()).append(".").append(dependency.getType());
			result.add(sb.toString());
		}
		return result;
	}

	private static String getUserHomePath() {
		return SystemUtils.getUserHome().toString();
	}
	
	@Override
	public List<TestCase> retrieveFailingTestCase(String buggyVersionPath) throws IOException {
		/*
		 * 1. Run mvn test
		 * 2. Read output
		 * 3. Parse maven output
		 * 4. OutputHandler can create list of failing test case?
		 * 5. 
		 */
		String failingFile = buggyVersionPath + File.separator + "failing_tests";
		File file = new File(failingFile);

		BufferedReader reader = new BufferedReader(new FileReader(file));

		List<TestCase> list = new ArrayList<>();
		String line = null;
		while ((line = reader.readLine()) != null) {
			if (line.startsWith("---")) {
				String testClass = line.substring(line.indexOf(" ") + 1, line.indexOf("::"));
				String testMethod = line.substring(line.indexOf("::") + 2, line.length());
				System.currentTimeMillis();
				TestCase tc = new TestCase(testClass, testMethod);
				list.add(tc);
			}
		}
		reader.close();

		return list;
	}
}
