package tregression.auto;

import java.util.HashMap;
import java.util.List;
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
	
	protected Optional<InputsAndOutput> getIO(final Trace trace, final String testSrcPath, final PairList pairList, final String IOStoragePath, final String projectName, final String bugID) {
		IODetector ioDetector = new IODetector(trace, testSrcPath, pairList);
		StoredIOParser IOParser = new StoredIOParser(IOStoragePath, projectName, bugID);
		HashMap<String, List<String[]>> storedIO = IOParser.getStoredIO();
		if (storedIO == null) {
			// stored IO not found, detect IO and store
			Optional<InputsAndOutput> result = ioDetector.detect();
			IOParser.storeIO(result);
			return result;
		}
		// read from stored IO
		List<String[]> inputs = storedIO.get(InputsAndOutput.INPUTS_KEY);
		List<String[]> output = storedIO.get(InputsAndOutput.OUTPUT_KEY);
		int outputNodeID = Integer.valueOf(output.get(0)[0]);
		String outputVar = output.get(0)[1];
		return ioDetector.detect(outputNodeID, outputVar);
	}

}
