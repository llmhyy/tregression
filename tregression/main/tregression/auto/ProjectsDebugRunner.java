package tregression.auto;

import java.util.Optional;

import iodetection.IODetector;
import iodetection.IODetector.InputsAndOutput;
import microbat.model.trace.Trace;
import tregression.model.PairList;

public abstract class ProjectsDebugRunner extends ProjectsRunner {

	public ProjectsDebugRunner(final String basePath, final String resultPath) {
		super(basePath, resultPath);
	}
	
	public ProjectsDebugRunner(final String basePath, final String resultPath, final int maxThreadCount) {
		super(basePath, resultPath, maxThreadCount);
	}
	
	protected Optional<InputsAndOutput> getIO(final Trace trace, final String testSrcPath, final PairList pairList) {
		IODetector ioDetector = new IODetector(trace, testSrcPath, pairList);
		return ioDetector.detect();
	}

}
