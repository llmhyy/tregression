package tregression.handler;

import java.nio.file.Paths;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import microbat.util.JavaUtil;
import tregression.auto.ProjectsRunner;
import tregression.auto.Regs4jRunner;

public class Regs4jRunnerHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {

        JavaUtil.sourceFile2CUMap.clear();
        Job job = new Job("Testing Tregression") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                execute();
                return Status.OK_STATUS;
            }
        };
        job.schedule();
        return null;
    }

    private void execute() {
        final String basePath = "E:\\david\\Regs4j";
        final String resultPath = Paths.get(basePath, "regs4j-result.txt").toString();
        ProjectsRunner runner = new Regs4jRunner(basePath, resultPath);
        runner.run();
    }
}
