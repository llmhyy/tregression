package tregression.auto;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import iodetection.IODetector;
import iodetection.StoredIOParser;
import iodetection.IODetector.InputsAndOutput;
import iodetection.IODetector.NodeVarValPair;
import microbat.model.trace.Trace;
import tregression.model.PairList;

public abstract class ProjectsDebugRunner extends ProjectsRunner {

	public ProjectsDebugRunner(final String basePath, final String resultPath) {
		super(basePath, resultPath);
	}
	
	public ProjectsDebugRunner(final String basePath, final String resultPath, final int maxThreadCount) {
		super(basePath, resultPath, maxThreadCount);
	}
	
	/**
	 * Look for stored IO and return the parsed results. If no IO has been stored, 
	 * detect IO, store and return the results.
	 * 
	 * @param trace
	 * @param testSrcPath
	 * @param pairList
	 * @param IOStoragePath
	 * @param projectName
	 * @param bugID
	 * @return
	 */
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
		return ioDetector.detect(inputs, output);
	}
	
	/**
	 * Detect and return IO.
	 * 
	 * @param trace
	 * @param testSrcPath
	 * @param pairList
	 * @return
	 */
	protected Optional<InputsAndOutput> detectIO(final Trace trace, final String testSrcPath, final PairList pairList) {
		IODetector ioDetector = new IODetector(trace, testSrcPath, pairList);
		return ioDetector.detect();
	}
	
	public Optional<NodeVarValPair> getOutput(final Trace trace, final String testSrcPath, final PairList pairList, final String IOStoragePath, final String projectName, final String bugID) {
		IODetector ioDetector = new IODetector(trace, testSrcPath, pairList);
		StoredIOParser IOParser = new StoredIOParser(IOStoragePath, projectName, bugID);
		HashMap<String, List<String[]>> storedIO = IOParser.getStoredIO();
		if (storedIO == null) {
			// stored IO not found, detect output
			Optional<NodeVarValPair> result = ioDetector.detectOutput();
			return result;
		}
		// read from stored IO
		List<String[]> output = storedIO.get(InputsAndOutput.OUTPUT_KEY);
		return ioDetector.detect(output);
	}

}
