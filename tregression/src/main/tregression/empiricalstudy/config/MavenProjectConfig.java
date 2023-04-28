package tregression.empiricalstudy.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class MavenProjectConfig extends ProjectConfig {

    public final static String M2AFFIX = ".m2" + File.separator + "repository";

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
        return new MavenProjectConfig("src" + File.separator + "test" + File.separator + "java",
                "src" + File.separator + "main" + File.separator + "java", "target" + File.separator + "test-classes",
                "target" + File.separator + "classes", "target", projectName, regressionID);
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
        return parseJarAllFilesInPath(projectRoot.resolve("target").resolve("dependency"));
    }

    /**
     * Executes `mvn dependency:copy-dependencies` command.
     * It copies all dependencies to src/target/dependency directory.
     * 
     * @param root
     * @return
     */
    private static List<String> executeMavenCopyDepsCmd(Path root) {
        List<String> cmdList = new ArrayList<>();
        
        cmdList.add("mvn");
        cmdList.add("dependency:copy-dependencies");

        String[] cmds = cmdList.toArray(new String[0]);
        List<String> result = new ArrayList<>();
        try {
            ProcessBuilder pb = new ProcessBuilder(cmds);
            pb.directory(root.toFile());
            pb.redirectErrorStream(true); // merge stdout and stderr
            Process proc = pb.start();

            InputStream stdin = proc.getInputStream();
            InputStreamReader isr = new InputStreamReader(stdin);
            BufferedReader br = new BufferedReader(isr);

            String line = null;
            while ((line = br.readLine()) != null)
                result.add(line);

            stdin.close();

            return result;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    private static List<String> parseJarAllFilesInPath(Path root) throws IOException {
        final List<String> result = new ArrayList<>();
        // Only filter directories or jar files.
        try (Stream<Path> stream = Files.list(root).filter(new Predicate<Path>() {
            public boolean test(Path path) {
                return path.endsWith("jar") || Files.isDirectory(path);
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
                            parseJarAllFilesInPath(path);
                        } else {
                            result.add(path.toRealPath().toString());
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
