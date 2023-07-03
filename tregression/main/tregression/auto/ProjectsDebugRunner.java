package tregression.auto;

import java.util.Optional;

import iodetection.IODetector;
import iodetection.IODetector.InputsAndOutput;
import microbat.model.trace.Trace;
import tregression.io.StoredIOParser;
import tregression.model.PairList;

public abstract class ProjectsDebugRunner extends ProjectsRunner {

	public ProjectsDebugRunner(final String basePath, final String resultPath) {
		super(basePath, resultPath);
	}
	
	public ProjectsDebugRunner(final String basePath, final String resultPath, final int maxThreadCount) {
		super(basePath, resultPath, maxThreadCount);
	}
	
	protected Optional<InputsAndOutput> getIO(final Trace trace, final String testSrcPath, final PairList pairList, final String IOFilePath, final String projectName, final String bugID) {
		IODetector ioDetector = new IODetector(trace, testSrcPath, pairList);
		if (IOFilePath == null) {
			return ioDetector.detect();
		}
		// get stored output node
		StoredIOParser IOParser = new StoredIOParser(IOFilePath);
		String[] outputInfo = IOParser.getOutputInfo(projectName, bugID);
		// stored output not found
		if (outputInfo == null) {
			return ioDetector.detect();
		}
		int outputNodeID = Integer.valueOf(outputInfo[2]);
		String outputVar = outputInfo[3];
		return ioDetector.detect(outputNodeID, outputVar);
	}

}
