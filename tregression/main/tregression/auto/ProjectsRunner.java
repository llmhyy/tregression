package tregression.auto;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import tregression.auto.result.ResultWriter;
import tregression.auto.result.RunResult;
import tregression.empiricalstudy.EmpiricalTrial;
import tregression.empiricalstudy.TrialGenerator0;
import tregression.empiricalstudy.config.ProjectConfig;

public abstract class ProjectsRunner {
    protected final String basePath;
    protected final String resultPath;
    protected final int maxThreadsCount;
    protected int hangingThreads = 0;

    protected List<String> filter = new ArrayList<>();
    protected List<String> targetBugs = new ArrayList<>();

    public ProjectsRunner(final String basePath, final String resultPath) {
        this(basePath, resultPath, 5);
    }

    public ProjectsRunner(final String basePath, final String resultPath, final int maxThreadCount) {
        this.basePath = basePath;
        this.resultPath = resultPath;
        this.maxThreadsCount = 5;
        
        final String targetBugPath = Paths.get(basePath, "bugs.txt").toString();
        try (BufferedReader reader = new BufferedReader(new FileReader(targetBugPath))) {
        	String line;
			while ((line = reader.readLine()) != null) {
				this.targetBugs.add(line);
			}
        } catch (IOException e) {
        	e.printStackTrace();
        }
    }

    public void run() {
        this.filter = new ArrayList<>();
        for (RunResult result : this.loadProcessedResult()) {
            this.filter.add(result.projectName + ":" + result.bugID);
        }

        ResultWriter writer = new ResultWriter(resultPath);
        File baseFolder = new File(this.basePath);
        for (String projectName : baseFolder.list()) {
            ProjectsRunner.printMsg("Processing: " + projectName);
            final String projectPath = Paths.get(this.basePath, projectName).toString();
            File projectFolder = new File(projectPath);
            if (!projectFolder.isDirectory()) {
            	continue;
            }
            for (String bugID_str : projectFolder.list()) {
                if (this.filter.contains(projectName + ":" + bugID_str)) {
                    ProjectsRunner.printMsg("Skip: " + projectName + " " + bugID_str);
                    continue;
                }
                
                final String id = projectName + ":" + bugID_str;
                if (!this.targetBugs.contains(id)) {
                	continue;
                }
                
                if (!id.equals("Lang:22")) {
                	continue;
                }

                RunResult result = this.runProject(projectName, bugID_str);
                if (result != null) {
                    writer.writeResult(result);
                }
                if (this.hangingThreads > this.maxThreadsCount) {
                    break;
                }
            }
        }
    }

    public abstract RunResult runProject(final String projectName, final String bugID_str);

    protected List<EmpiricalTrial> generateTrials(final String bugFolder, final String fixFolder,
            final ProjectConfig config) {
        final TrialGenerator0 generator0 = new TrialGenerator0();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<List<EmpiricalTrial>> future = executor.submit(() -> {
            return generator0.generateTrials(bugFolder, fixFolder, false, false, false, 3, true, true, config, "");
        });
        try {
            return future.get(10, TimeUnit.MINUTES);
        } catch (TimeoutException e) {
            return null;
        } catch (InterruptedException e) {
            return null;
        } catch (ExecutionException e) {
            return null;
        } finally {
            future.cancel(true);
            executor.shutdown();
        }
    }

    protected List<RunResult> loadProcessedResult() {
        List<RunResult> results = new ArrayList<>();
        try {
            FileReader fileReader = new FileReader(this.resultPath);
            BufferedReader reader = new BufferedReader(fileReader);
            String line; // first line is the headers
            while ((line = reader.readLine()) != null) {
                RunResult result = RunResult.parseString(line);
                results.add(result);
            }
            reader.close();
        } catch (FileNotFoundException e) {

        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }

    public static String genMsg(final String message) {
        return "[Runner]: " + message;
    }

    public static void printMsg(final String message) {
        System.out.println(ProjectsRunner.genMsg(message));
    }
}
