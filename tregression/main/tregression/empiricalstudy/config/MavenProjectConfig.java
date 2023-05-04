package tregression.empiricalstudy.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;

public class MavenProjectConfig extends ProjectConfig {

    public static final String PATH_MAVEN_TEST = "src" + File.separator + "test" + File.separator + "java";
    public static final String PATH_MAVEN_SRC = "src" + File.separator + "main" + File.separator + "java";
    public static final String PATH_MAVEN_BUILD = "target";
    public static final String PATH_MAVEN_BUILD_SRC = "target" + File.separator + "test-classes";
    public static final String PATH_MAVEN_BUILD_TEST = "target" + File.separator + "classes";
    

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
        return new MavenProjectConfig(PATH_MAVEN_TEST,
        		PATH_MAVEN_SRC, PATH_MAVEN_BUILD_TEST,
        		PATH_MAVEN_BUILD_SRC, PATH_MAVEN_BUILD, projectName, regressionID);
    }

    public static List<String> getMavenDependencies(String path) {
        Path projectRoot = Paths.get(path);
        try {
            return readAllDependency(projectRoot);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public static List<String> readAllDependency(Path projectRoot) throws Exception {
        executeMavenCopyDepsCmd(projectRoot);
        return getAllJarRelativePathsFromRoot(projectRoot.resolve("target").resolve("dependency"), projectRoot);
    }

    /**
     * Executes `mvn dependency:copy-dependencies` command.
     * It copies all dependencies to src/target/dependency directory.
     * 
     * @param root
     * @return
     */
    private static boolean executeMavenCopyDepsCmd(Path root) {
    	InvocationRequest request = new DefaultInvocationRequest();
    	request.setPomFile( new File( root + File.separator + "pom.xml" ) );
    	request.setGoals( Collections.singletonList( "dependency:copy-dependencies" ) );
    	 
    	Invoker invoker = new DefaultInvoker();
    	invoker.setMavenHome(new File(System.getenv("MAVEN_HOME")));
    	try {
    		InvocationResult invocationResult = invoker.execute( request );
    		return invocationResult.getExitCode() == 0;
    	} catch (MavenInvocationException e) {
    		e.printStackTrace();
    		return false;
    	}
    }

    private static List<String> getAllJarRelativePathsFromRoot(Path startPath, final Path projectRoot) throws IOException {
        final List<String> result = new ArrayList<>();
        // Only filter directories or jar files.
        try (Stream<Path> stream = Files.list(startPath).filter(new Predicate<Path>() {
            public boolean test(Path path) {
                return path.toString().endsWith("jar") || Files.isDirectory(path);
            }
        })) {
            // loop through the stream of directories/jar files
            stream.forEach(new Consumer<Path>() {
                @Override
                public void accept(Path path) {
                    try {
                        // if the path is a directory, recursively call this method on the directory. 
                        // Else if it is a jar, add to result
                        if (Files.isDirectory(path)) {
                            getAllJarRelativePathsFromRoot(path, projectRoot);
                        } else {
                            result.add("." + File.separator + projectRoot.relativize(path).toString());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        return result;
    }
}
