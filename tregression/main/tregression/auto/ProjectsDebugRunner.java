package tregression.auto;

import java.util.Optional;

import iodetection.IOReader.IOResult;
import microbat.model.trace.Trace;
import tregression.model.PairList;

public abstract class ProjectsDebugRunner extends ProjectsRunner {

	public ProjectsDebugRunner(final String basePath, final String resultPath) {
		super(basePath, resultPath);
	}
	
	public ProjectsDebugRunner(final String basePath, final String resultPath, final int maxThreadCount) {
		super(basePath, resultPath, maxThreadCount);
	}
	

}
