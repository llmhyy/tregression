package tregression.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

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
 * Most of the logic from there was copied over.
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

    /**
     * Example usage of this class.
     * 
     * @param args
     */
    public static void main(String[] args) {
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
        // (before regression) and regression inducing versions
        ProjectPaths clonedProjectPaths = wrapper.checkout("alibaba/fastjson", regressions.get(0));
        System.out.println(clonedProjectPaths);
        
        boolean compilationSuccessful = wrapper.mvnCompileProjects(clonedProjectPaths);
        System.out.println(compilationSuccessful);
    }

    public List<String> getProjectNames() {
        return MysqlManager.selectProjects("select distinct project_full_name from regressions");
    }

    public List<Regression> getRegressions(String projectName) {
        return MysqlManager.selectRegressions(
                "select bfc,buggy,bic,work,testcase from regressions where project_full_name='" + projectName + "'");
    }

    public ProjectPaths checkout(String projectFullName, Regression regression) {
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
        Path testFilePath = Regs4jProjectConfig.getTestFilePath(ricDir.toPath().toString());
        try {
            Files.writeString(testFilePath, testCaseStr);
        } catch (IOException e) {
             e.printStackTrace();
        }
        return new ProjectPaths(workDir.toPath(), ricDir.toPath());
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
