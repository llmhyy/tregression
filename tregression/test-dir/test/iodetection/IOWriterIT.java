package iodetection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import microbat.model.value.GraphNode;
import microbat.model.value.VarValue;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IOWriterIT {
	static final String TEMP_FILE_NAME = "io.txt";
	static final String INPUT_VAR_ID_FMT = "input-%d";
	static final String OUTPUT_VAR_ID = "output-1";
	static final String SAMPLE_INPUT_CONTENT = String.format(INPUT_VAR_ID_FMT, 1) + " "
			+ String.format(INPUT_VAR_ID_FMT, 2) + System.lineSeparator();
	static final String SAMPLE_OUTPUT_CONTENT = OUTPUT_VAR_ID + System.lineSeparator();;
	static final String SAMPLE_FILE_CONTENT = SAMPLE_INPUT_CONTENT + SAMPLE_OUTPUT_CONTENT;

	private IOWriter writer;

	@TempDir
	private Path tempDir;

	private Path tempFile;

	private List<VarValue> inputs;
	private VarValue output;

	@BeforeEach
	void setUp() {
		writer = new IOWriter();
		tempFile = tempDir.resolve(TEMP_FILE_NAME);

		inputs = new ArrayList<>();
		inputs.add(new VarValueStub(String.format(INPUT_VAR_ID_FMT, 1)));
		inputs.add(new VarValueStub(String.format(INPUT_VAR_ID_FMT, 2)));
		output = new VarValueStub(OUTPUT_VAR_ID);
	}

	@Test
	void writeIO_InputsAndOutputProvided_WritesIO() throws IOException {
		writer.writeIO(inputs, output, tempFile);
		String fileContents = Files.readString(tempFile);
		assertEquals(SAMPLE_FILE_CONTENT, fileContents);
	}

	@Test
	void writeIO_InputsIsEmpty_WritesOutputCorrectly() throws IOException {
		inputs = new ArrayList<>();
		writer.writeIO(inputs, output, tempFile);
		String fileContents = Files.readString(tempFile);
		assertEquals(System.lineSeparator() + SAMPLE_OUTPUT_CONTENT, fileContents);
	}

	@Test
	void writeIO_oUTPUTIsEmpty_WritesInputsCorrectly() throws IOException {
		output = null;
		writer.writeIO(inputs, output, tempFile);
		String fileContents = Files.readString(tempFile);
		assertEquals(SAMPLE_INPUT_CONTENT + System.lineSeparator(), fileContents);
	}

	@Test
	void writeIO_WritesMultipleTimes_Overwrites() throws IOException {
		int count = 3;
		for (int i = 0; i < count; i++) {
			writer.writeIO(inputs, output, tempFile);
		}
		String fileContents = Files.readString(tempFile);
		assertEquals(SAMPLE_FILE_CONTENT, fileContents);
	}

	/**
	 * We could use Mockito in the future.
	 *
	 */
	private class VarValueStub extends VarValue {
		private static final long serialVersionUID = 1L;
		private final String varID;

		public VarValueStub(String varID) {
			super();
			this.varID = varID;
		}

		@Override
		public String getVarID() {
			return varID;
		}

		@Override
		public boolean isTheSameWith(GraphNode node) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public VarValue clone() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getHeapID() {
			// TODO Auto-generated method stub
			return null;
		}

	}
}
