package tregression.auto;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.io.FileUtils;

import microbat.model.trace.Trace;
import net.lingala.zip4j.ZipFile;
import tregression.auto.result.RunResult;
import tregression.empiricalstudy.DeadEndRecord;
import tregression.empiricalstudy.EmpiricalTrial;
import tregression.empiricalstudy.config.ProjectConfig;
import tregression.empiricalstudy.config.Regs4jProjectConfig;
import tregression.empiricalstudy.solutionpattern.SolutionPattern;

public class Regs4jRunner extends ProjectsRunner {

    public Regs4jRunner(String basePath, String resultPath, int maxThreadCount) {
        super(basePath, resultPath, maxThreadCount);
    }

    public Regs4jRunner(String basePath, String resultPath) {
        super(basePath, resultPath);
    }

    @Override
    public RunResult runProject(String projectName, String zippedBugIdStr) {
        if (!zippedBugIdStr.contains(".zip"))
            return null;
        String bugIdStr = zippedBugIdStr.substring(0, zippedBugIdStr.indexOf("."));
        RunResult result = new RunResult();
        try {
            Integer.valueOf(bugIdStr);
        } catch (NumberFormatException e) {
            return null;
        }

        result.projectName = projectName;
        result.bugID = Integer.valueOf(bugIdStr);

        final ProjectConfig config = Regs4jProjectConfig.getConfig(projectName, bugIdStr);
        if (config == null) {
            result.errorMessage = ProjectsRunner.genMsg("Cannot generate project config");
            return result;
        }

        Path pathToRegression = Paths.get(basePath, projectName, bugIdStr);
        extract(pathToRegression.toString() + ".zip");
        final String bugFolder = pathToRegression.resolve("ric").toString();
        final String fixFolder = pathToRegression.resolve("work").toString();

        if (!(new File(bugFolder).exists()) || !(new File(fixFolder).exists())) {
            result.errorMessage = ProjectsRunner
                    .genMsg(String.format("Working (%s) or Buggy (%s) project not found", fixFolder, bugFolder));
            return result;
        }

        List<EmpiricalTrial> trials = this.generateTrials(bugFolder, fixFolder, config);
        if (trials == null || trials.isEmpty()) {
            result.errorMessage = ProjectsRunner.genMsg("No trials generated");
            return result;
        }
        analyseTrials(trials, result);
        deleteIfExists(pathToRegression.toString());
        return result;
    }

    /**
     * Extracts the zipped directory. It does not delete the original zip directory.
     * 
     * @param path
     * @return
     */
    private boolean extract(String path) {
        try (ZipFile zippedRegression = new ZipFile(path)) {
            zippedRegression.extractAll(path.substring(0, path.lastIndexOf(File.separator)));
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean deleteIfExists(String pathStr) {
        Path path = Paths.get(pathStr);
        if (Files.exists(path)) {
            try {
                FileUtils.deleteDirectory(path.toFile());
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    private void analyseTrials(List<EmpiricalTrial> trials, RunResult result) {
        StringBuilder solutionNameStringBuilder = new StringBuilder();
        for (int i = 0; i < trials.size(); i++) {
            EmpiricalTrial trial = trials.get(i);
            System.out.println(trial);
            Trace trace = trial.getBuggyTrace();
            if (trace == null) {
                result.errorMessage = "[Trials Generation]: " + trial.getExceptionExplanation();
                return;
            }
            result.traceLen = trace.size();
            result.isOmissionBug = trial.getBugType() == EmpiricalTrial.OVER_SKIP;
            result.rootCauseOrder = trial.getRootcauseNode() == null ? -1 : trial.getRootcauseNode().getOrder();
            for (DeadEndRecord deadEndRecord : trial.getDeadEndRecordList()) {
                SolutionPattern solutionPattern = deadEndRecord.getSolutionPattern();
                if (solutionPattern != null) {
                    solutionNameStringBuilder.append(deadEndRecord.getSolutionPattern().getTypeName() + ":");
                }
            }
        }
        result.solutionName = solutionNameStringBuilder.toString();
    }
}
