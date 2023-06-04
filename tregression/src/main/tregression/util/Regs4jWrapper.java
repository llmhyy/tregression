package tregression.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import core.Migrator;
import core.MysqlManager;
import core.Reducer;
import core.SourceCodeManager;
import model.Regression;
import model.Revision;
import net.lingala.zip4j.ZipFile;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(Regs4jWrapper.class);
    private static final int DEFAULT_TEST_TIMEOUT_SEC = 120;
    private static final String CLONE_SUCCESS_MSG = "SUCCESS";

    public Regs4jWrapper(SourceCodeManager sourceCodeManager, Reducer reducer, Migrator migrator) {
        super();
        this.sourceCodeManager = sourceCodeManager;
        this.reducer = reducer;
        this.migrator = migrator;
    }

    public static void main(String[] args) {
//        final Path repoPath = Paths.get(System.getenv("USERPROFILE"), "Desktop", "regs4j-test-repo");
        final Path repoPath = Paths.get("E:\\david\\Regs4j");
        // Instantiation
        SourceCodeManager sourceCodeManager = new SourceCodeManager();
        Reducer reducer = new Reducer();
        Migrator migrator = new Migrator();
        Regs4jWrapper wrapper = new Regs4jWrapper(sourceCodeManager, reducer, migrator);
        wrapper.cloneAll(repoPath.toString());
    }

    /**
     * Example usage of this class. Run this to check if everything is setup.
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
        String projectName = "alibaba/fastjson";
        List<Regression> regressions = wrapper.getRegressions(projectName);
        System.out.println(regressions);

        // Checkout "alibaba/fastjson" regression ID 1, and return the paths to working
        // and regression inducing versions
        int regId = 1;
        final Path repoPath = Paths.get(System.getenv("USERPROFILE"), "Desktop", "regs4j-test-repo");
        ProjectPaths checkoutDestinationPaths = wrapper.generateProjectPaths(repoPath.toString(), projectName, regId);
        boolean checkoutSuccess = wrapper.checkout(projectName, regressions.get(regId - 1), checkoutDestinationPaths);
        LOGGER.info("Checkout success {}", checkoutSuccess);

        // Compile
        boolean compilationSuccessful = wrapper.mvnCompileProjects(checkoutDestinationPaths);
        LOGGER.info("Compilation success {}", compilationSuccessful);
    }

    /**
     * Clones every regression into specified repoPath.
     */
    public void cloneAll(String repoPath) {
        String pathToResults = repoPath + File.separator + "result.txt";
        Set<String> regressionsAlreadyCloned = getRegressionsAlreadyCheckedOut(pathToResults);

        // List projects
        List<String> projectNames = getProjectNames();

        for (String projectName : projectNames) {
            List<Regression> regressions = getRegressions(projectName);
            LOGGER.info("Regressions {}", regressions);
            for (int regId = 1; regId <= regressions.size(); regId++) {
                LOGGER.info("Start clone for {} {}", projectName, regId);
                if (isCheckedOut(regressionsAlreadyCloned, projectName, regId)) {
                    LOGGER.info("{} {} already exists", projectName, regId);
                    continue;
                }

                ProjectPaths checkoutDestinationPaths = generateProjectPaths(repoPath, projectName, regId);
                String cloneMessage = cloneSingleRegression(projectName, regId, regressions.get(regId - 1),
                        checkoutDestinationPaths);
                try {
                    // Delete original regression folder after
                    // cloning it is complete, regardless of
                    // failure or success
                    deleteIfExists(checkoutDestinationPaths.getBasePath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                storeResult(projectName, regId, cloneMessage, pathToResults);
            }
        }
    }

    private static final String RESULT_DELIM = ",";

    private void storeResult(String projectName, int regId, String message, String path) {
        LOGGER.info("Writing result for {} {} to file {}", projectName, regId, path);
        FileWriter fileWriter = null;
        try {
            File file = new File(path);
            file.createNewFile();
            fileWriter = new FileWriter(file, true);
            fileWriter.write(
                    String.join(RESULT_DELIM, projectName, String.valueOf(regId), message) + System.lineSeparator());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (fileWriter != null) {
                    fileWriter.flush();
                    fileWriter.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Set<String> getRegressionsAlreadyCheckedOut(String path) {
        Set<String> result = new HashSet<>();
        try (FileReader fileReader = new FileReader(path);
                BufferedReader bufferedReader = new BufferedReader(fileReader)) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String[] split = line.split(RESULT_DELIM);
                String key = formKey(split[0], Integer.parseInt(split[1]));
                result.add(key);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    private String formKey(String projectName, int regId) {
        return projectName + "#" + regId;
    }

    private String cloneSingleRegression(String projectName, int regId, Regression regression,
            ProjectPaths checkoutDestinationPaths) {
        boolean checkoutSuccessful = checkout(projectName, regression, checkoutDestinationPaths);
        LOGGER.info("Checkout successful? {}", checkoutSuccessful);

        if (!checkoutSuccessful) {
            return "CHECKOUT FAILURE";
        }

        boolean compilationSuccessful = mvnCompileProjects(checkoutDestinationPaths);
        LOGGER.info("Compilation successful? {}", compilationSuccessful);

        if (!compilationSuccessful) {
            return "COMPILATION FAILURE";
        }

        boolean testsSuccessful = mvnRunTest(checkoutDestinationPaths, regression.getTestCase());
        LOGGER.info("Tests successful? {}", testsSuccessful);

        if (!testsSuccessful) {
            return "TEST RESULT INCORRECT";
        }

        boolean compressionSuccessful = compress(checkoutDestinationPaths.getBasePath());
        LOGGER.info("Compression successful? {}", compressionSuccessful);
        if (!compressionSuccessful) {
            return "COMPRESSION FAILURE";
        }
        return CLONE_SUCCESS_MSG;
    }

    public List<String> getProjectNames() {
        return MysqlManager.selectProjects("select distinct project_full_name from regressions");
    }

    public List<Regression> getRegressions(String projectName) {
        return MysqlManager.selectRegressions(
                "select bfc,buggy,bic,work,testcase from regressions where project_full_name='" + projectName + "'");
    }

    public boolean compress(Path path) {
        try (ZipFile zippedRegression = new ZipFile(path.toString() + ".zip")) {
            zippedRegression.addFolder(path.toFile());
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Extract contents from a zipped folder.
     * 
     * @param path Zipped folder path
     * @return
     */
    public boolean extract(Path path) {
        String pathStr = path.toString();
        try (ZipFile zippedRegression = new ZipFile(pathStr)) {
            zippedRegression.extractAll(pathStr.substring(0, pathStr.lastIndexOf(File.separator)));
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
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
    public boolean checkout(String projectFullName, Regression regression, ProjectPaths checkoutDestPaths) {
        String testCaseStr = regression.getTestCase();
        if (testCaseStr.isEmpty()) {
            return false;
        }

        File projectDir = sourceCodeManager.getProjectDir(projectFullName);
        Revision rfc = regression.getRfc();
        File rfcDir = sourceCodeManager.checkout(rfc, projectDir, projectFullName);
        if (rfcDir == null)
            return false;
        rfc.setLocalCodeDir(rfcDir);
        regression.setRfc(rfc);
        Revision ric = regression.getRic();
        File ricDir = sourceCodeManager.checkout(ric, projectDir, projectFullName);
        if (ricDir == null)
            return false;
        ric.setLocalCodeDir(ricDir);
        regression.setRic(ric);
        Revision working = regression.getWork();
        File workDir = sourceCodeManager.checkout(working, projectDir, projectFullName);
        if (workDir == null)
            return false;
        working.setLocalCodeDir(workDir);
        regression.setWork(working);
        List<Revision> needToTestMigrateRevisionList = Arrays.asList(ric, working);
        migrateTestAndDependency(rfc, needToTestMigrateRevisionList, testCaseStr);
        Path ricPath = ricDir.toPath();
        Path testFilePath = Regs4jProjectConfig.getTestFilePath(ricPath.toString());
        Path basePath = checkoutDestPaths.getBasePath();
        Path newRICPath = checkoutDestPaths.getRicPath();
        Path newWorkPath = checkoutDestPaths.getWorkingPath();
        try {
            Files.writeString(testFilePath, testCaseStr);
            Files.createDirectories(basePath);
            deleteIfExists(newRICPath);
            deleteIfExists(newWorkPath);
            FileUtils.moveDirectory(ricPath.toFile(), newRICPath.toFile());
            FileUtils.moveDirectory(workDir, newWorkPath.toFile());
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean mvnCompileProjects(ProjectPaths paths) {
        final String mvnTestCompileCmd = "test-compile";
        boolean ricCompilationSuccess = MavenProjectConfig.executeMavenCmd(paths.getRicPath(), mvnTestCompileCmd);
        return MavenProjectConfig.executeMavenCmd(paths.getWorkingPath(), mvnTestCompileCmd) && ricCompilationSuccess;
    }

    public boolean mvnRunTest(ProjectPaths paths, String testCaseStr) {
        final String mvnTestCmd = String.format("test -Dtest=%s", testCaseStr);
        boolean ricTestSuccess = MavenProjectConfig.executeMavenCmd(paths.getRicPath(), mvnTestCmd,
                DEFAULT_TEST_TIMEOUT_SEC);
        return MavenProjectConfig.executeMavenCmd(paths.getWorkingPath(), mvnTestCmd, DEFAULT_TEST_TIMEOUT_SEC)
                && !ricTestSuccess;
    }

    /**
     * Returns whether the regression is checked-out successfully.
     * 
     * @param paths
     * @return
     */
    public boolean isCheckedOut(Set<String> regressionsAlreadyCloned, String project, int regId) {
        return regressionsAlreadyCloned.contains(formKey(project, regId));
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

    public ProjectPaths generateProjectPaths(String repoPath, String projectFullName, int bugId) {
        String bugIdStr = String.valueOf(bugId);
        Path basePath = Paths.get(repoPath, projectFullName.replace("/", "_"), bugIdStr);
        Path newRICPath = basePath.resolve("ric");
        Path newWorkPath = basePath.resolve("work");
        return new ProjectPaths(newWorkPath, newRICPath, basePath);
    }

    /**
     * Paths to working (before regression) and regression inducing versions after
     * checking out, and path to the folder containing both of them (basePath).
     * 
     * @author bchenghi
     *
     */
    public static class ProjectPaths {
        private final Path workingPath;
        private final Path ricPath;
        private final Path basePath;

        public ProjectPaths(Path workingPath, Path ricPath, Path basePath) {
            super();
            this.workingPath = workingPath;
            this.ricPath = ricPath;
            this.basePath = basePath;
        }

        public Path getWorkingPath() {
            return workingPath;
        }

        public Path getRicPath() {
            return ricPath;
        }

        public Path getBasePath() {
            return basePath;
        }

        @Override
        public String toString() {
            return "ProjectPaths [workingPath=" + workingPath + ", ricPath=" + ricPath + ", basePath=" + basePath + "]";
        }
    }
}
