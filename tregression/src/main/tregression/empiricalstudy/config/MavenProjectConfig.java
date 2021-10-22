package tregression.empiricalstudy.config;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.SystemUtils;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;


public class MavenProjectConfig extends ProjectConfig {

	public final static String M2AFFIX = ".m2" + File.separator + "repository";
	private Model model;

	private MavenProjectConfig(String srcTestFolder, String srcSourceFolder, String bytecodeTestFolder,
			String bytecodeSourceFolder, String buildFolder, String projectName, String regressionID, Model model) {
		super(srcTestFolder, srcSourceFolder, bytecodeTestFolder, bytecodeSourceFolder, buildFolder, projectName,
				regressionID);
		this.model = model;
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


	public static ProjectConfig getConfig(File pom, String projectName, String regressionID) {
		MavenXpp3Reader mavenReader = new MavenXpp3Reader();
		Model model;
		try {
			model = mavenReader.read(new FileReader(pom));
		} catch (IOException | XmlPullParserException e) {
			System.out.println("Probelm parsing poim.xml. Exiting");
			return null;
		}
		Build build = model.getBuild();
		String temp;
		String testSrc = (temp = build.getTestSourceDirectory()) == null ?
				"src/test/java" : temp;
		String mainSrc = (temp = build.getSourceDirectory()) == null ?
				"src/main/java" : temp;
		String testOut = (temp = build.getTestOutputDirectory()) == null ?
				"target/test-classes" : temp;
		String mainOut = (temp = build.getOutputDirectory()) == null ?
				"target/classes" : temp;
		String work = (temp = build.getDirectory()) == null ? "target" : temp;
		return new MavenProjectConfig(testSrc, mainSrc, testOut, mainOut,
									  work, projectName, regressionID, model);
	}
	
	private static String getUserHomePath() {
		return SystemUtils.getUserHome().toString();
	}
	
	private String stripMavenVar(String var) {
		return var.substring(2, var.length()-1);
	}
	
	protected void retrieveDependencies() {
		Properties properties = this.model.getProperties();
		List<Dependency> dependencies = this.model.getDependencies();
		this.dependencies = new ArrayList<>();
		String userHome = getUserHomePath();
		Pattern pattern = Pattern.compile("\\$\\{[a-zA-Z0-9.]+\\}");
		for (Dependency d : dependencies) {
			StringBuilder sb = new StringBuilder(userHome);
			sb.append(File.separator).append(M2AFFIX).append(File.separator)
			  .append(d.getGroupId().replace(".", File.separator)).append(File.separator)
			  .append(d.getArtifactId()).append(File.separator).append(d.getVersion())
			  .append(File.separator).append(d.getArtifactId()).append("-")
			  .append(d.getVersion()).append(".").append(d.getType());
			String s = sb.toString();
			Matcher m = pattern.matcher(s);
			while(m.find()) {
				String var = m.group();
				s = s.replace(var, properties.getProperty(stripMavenVar(var)));
			}
			this.dependencies.add(s);
		}
	}

}
