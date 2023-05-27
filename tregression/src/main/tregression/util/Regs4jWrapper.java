package tregression.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.io.FileUtils;

import core.Migrator;
import core.MysqlManager;
import core.Reducer;
import core.SourceCodeManager;
import model.Regression;
import model.Revision;
import tregression.empiricalstudy.config.MavenProjectConfig;
import tregression.empiricalstudy.config.Regs4jProjectConfig;

/**
 * Wrapper for Regs4J CLI. Refer to core.CLI class in
 * https://github.com/SongXueZhi/regs4j/tree/feature/tregression-integration.
 * Most of the logic in here was copied over from core.CLI.
 *
 */
public class Regs4jWrapper {
    private final SourceCodeManager sourceCodeManager;
    private final Reducer reducer;
    private final Migrator migrator;

    public Regs4jWrapper(SourceCodeManager sourceCodeManager, Reducer reducer, Migrator migrator) {
        super();
        this.sourceCodeManager = sourceCodeManager;
        this.reducer = reducer;
        this.migrator = migrator;
    }

    public static void main(String[] args) {
        final Path repoPath = Paths.get(System.getenv("USERPROFILE"), "Desktop", "regs4j-test-repo");
        // Instantiation
        SourceCodeManager sourceCodeManager = new SourceCodeManager();
        Reducer reducer = new Reducer();
        Migrator migrator = new Migrator();
        Regs4jWrapper wrapper = new Regs4jWrapper(sourceCodeManager, reducer, migrator);
        wrapper.cloneAll(repoPath.toString());
    }

    /**
     * Example usage of this class.
     * Run this to check if everything is setup.
     * 
     * @param args
     */
    private static void exampleUsage() {
        // Instantiation
        SourceCodeManager sourceCodeManager = new SourceCodeManager();
        Reducer reducer = new Reducer();
        Migrator migrator = new Migrator();
        Regs4jWrapper wrapper = new Regs4jWrapper(sourceCodeManager, reducer, migrator);

        // List projects
        System.out.println(wrapper.getProjectNames());

        // Get all regressions in the project "alibaba/fastjson"
        List<Regression> regressions = wrapper.getRegressions("alibaba/fastjson");
        System.out.println(regressions);

        // Checkout "alibaba/fastjson" regression ID 1, and return the paths to working
        // and regression inducing versions
        int regId = 1;
        final Path repoPath = Paths.get(System.getenv("USERPROFILE"), "Desktop", "regs4j-test-repo");
        ProjectPaths clonedProjectPaths = wrapper.checkout("alibaba/fastjson", regressions.get(regId - 1), regId,
                repoPath.toString());
        System.out.println(clonedProjectPaths);

        // Compile
        boolean compilationSuccessful = wrapper.mvnCompileProjects(clonedProjectPaths);
        System.out.println(compilationSuccessful);
    }

    /**
     * Clones every regression into specified repoPath.
     */
    public void cloneAll(String repoPath) {

        // List projects
        List<String> projectNames = getProjectNames();

        for (String projectName : projectNames) {
            List<Regression> regressions = getRegressions(projectName);
            System.out.println(regressions);
            for (int i = 0; i < regressions.size(); i++) {
                int regId = 1;
                ProjectPaths clonedProjectPaths = checkout(projectName, regressions.get(regId - 1), regId,
                        repoPath.toString());
                System.out.println(clonedProjectPaths);

                boolean compilationSuccessful = mvnCompileProjects(clonedProjectPaths);
                System.out.println(compilationSuccessful);
            }
        }
    }

    public List<String> getProjectNames() {
        return MysqlManager.selectProjects("select distinct project_full_name from regressions");
    }

    public List<Regression> getRegressions(String projectName) {
        return MysqlManager.selectRegressions(
                "select bfc,buggy,bic,work,testcase from regressions where project_full_name='" + projectName + "'");
    }

    /**
     * Clones the corresponding regression (work and ric), writes failing test into
     * a file, then moves them into repoPath\projectName\bugId\<ric or work>
     * 
     * @param projectFullName
     * @param regression
     * @param bugId
     * @param repoPath
     * @return
     */
    public ProjectPaths checkout(String projectFullName, Regression regression, int bugId, String repoPath) {
        File projectDir = sourceCodeManager.getProjectDir(projectFullName);
        Revision rfc = regression.getRfc();
        File rfcDir = sourceCodeManager.checkout(rfc, projectDir, projectFullName);
        rfc.setLocalCodeDir(rfcDir);
        regression.setRfc(rfc);
        Revision ric = regression.getRic();
        File ricDir = sourceCodeManager.checkout(ric, projectDir, projectFullName);
        ric.setLocalCodeDir(ricDir);
        regression.setRic(ric);
        Revision working = regression.getWork();
        File workDir = sourceCodeManager.checkout(working, projectDir, projectFullName);
        working.setLocalCodeDir(workDir);
        regression.setWork(working);
        List<Revision> needToTestMigrateRevisionList = Arrays.asList(ric, working);
        String testCaseStr = regression.getTestCase();
        migrateTestAndDependency(rfc, needToTestMigrateRevisionList, testCaseStr);
        Path ricPath = ricDir.toPath();
        Path testFilePath = Regs4jProjectConfig.getTestFilePath(ricPath.toString());
        String bugIdStr = String.valueOf(bugId);
        Path basePath = Paths.get(repoPath, projectFullName.replace("/", "_"), bugIdStr);
        Path newRICPath = basePath.resolve("ric");
        Path newWorkPath = basePath.resolve("work");
        try {
            Files.writeString(testFilePath, testCaseStr);
            Files.createDirectories(basePath);
            deleteIfExists(newRICPath);
            deleteIfExists(newWorkPath);
            Files.move(ricPath, newRICPath);
            Files.move(workDir.toPath(), newWorkPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ProjectPaths(newWorkPath, newRICPath);
    }

    public boolean mvnCompileProjects(ProjectPaths paths) {
        final String mvnTestCompileCmd = "test-compile";
        boolean ricCompilationSuccess = MavenProjectConfig.executeMavenCmd(paths.getRicPath(), mvnTestCompileCmd);
        return MavenProjectConfig.executeMavenCmd(paths.getWorkingPath(), mvnTestCompileCmd) && ricCompilationSuccess;
    }

    /**
     * Copied from example.CLI.java in CLI.jar (the regs4j jar file).
     * 
     * @param rfc
     * @param needToTestMigrateRevisionList
     * @param testCase
     */
    private void migrateTestAndDependency(final Revision rfc, List<Revision> needToTestMigrateRevisionList,
            String testCase) {
        migrator.equipRfcWithChangeInfo(rfc);
        reducer.reduceTestCases(rfc, testCase);
        needToTestMigrateRevisionList.forEach(new Consumer<Revision>() {
            @Override
            public void accept(Revision revision) {
                migrator.migrateTestFromTo_0(rfc, revision);
            }
        });
    }

    private void deleteIfExists(Path path) throws IOException {
        if (Files.exists(path)) {
            FileUtils.deleteDirectory(path.toFile());
        }
    }

    /**
     * Paths to working (before regression) and regression inducing versions after
     * checking out
     * 
     * @author bchenghi
     *
     */
    public static class ProjectPaths {
        private final Path workingPath;
        private final Path ricPath;

        public ProjectPaths(Path workingPath, Path ricPath) {
            super();
            this.workingPath = workingPath;
            this.ricPath = ricPath;
        }

        public Path getWorkingPath() {
            return workingPath;
        }

        public Path getRicPath() {
            return ricPath;
        }

        @Override
        public String toString() {
            return "ProjectPaths [workingPath=" + workingPath + ", ricPath=" + ricPath + "]";
        }

    }
}
