package defects4janalysis;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResultWriterIT {
	private ResultWriter writer;

	@TempDir
	private Path tempDir;

	private Path resultFilePath;

	private RunResult sampleRunResult;
	private String sampleRunResultStr;

	@BeforeEach
	void setUp() {
		sampleRunResult = new RunResult();
		sampleRunResult.projectName = "testing";
		sampleRunResult.bugID = 1;
		sampleRunResult.traceLen = 10;
		sampleRunResult.rootCauseOrder = 2;
		sampleRunResult.isOmissionBug = true;
		sampleRunResultStr = "testing,1,10,2,true,,,";
		resultFilePath = tempDir.resolve("testing.txt");
		writer = new ResultWriter(resultFilePath.toString());
	}

	@Test
	void writeResult_WriteTwoRunResults_WritesCorrectly() throws IOException {
		for (int i = 0; i < 2; i++) {
			writer.writeResult(sampleRunResult);
		}
		String actualStringContents = Files.readString(resultFilePath);
		String expectedStringContents = String.join(System.lineSeparator(), sampleRunResultStr, sampleRunResultStr, "");
		assertEquals(expectedStringContents, actualStringContents);
	}

	@Test
	void writeResult_WriteToAlreadyExistingFile_AppendsToExistingFile() throws IOException {
		for (int i = 0; i < 2; i++) {
			writer.writeResult(sampleRunResult);
		}

		writer = new ResultWriter(resultFilePath.toString());
		for (int i = 0; i < 1; i++) {
			writer.writeResult(sampleRunResult);
		}
		String actualStringContents = Files.readString(resultFilePath);
		String expectedStringContents = String.join(System.lineSeparator(), sampleRunResultStr, sampleRunResultStr,
				sampleRunResultStr, "");
		assertEquals(expectedStringContents, actualStringContents);
	}
}
