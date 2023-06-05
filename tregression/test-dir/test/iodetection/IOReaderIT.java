package iodetection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import iodetection.IOReader.IOResult;

import static iodetection.IOWriterIT.TEMP_FILE_NAME;
import static iodetection.IOWriterIT.SAMPLE_WRONG_VAR_VAL_FILE_CONTENT;
import static iodetection.IOWriterIT.SAMPLE_WRONG_BRANCH_FILE_CONTENT;
import static iodetection.IOWriterIT.INPUT_VAR_ID_FMT;
import static iodetection.IOWriterIT.OUTPUT_VAR_ID;
import static iodetection.IOWriterIT.SAMPLE_WRONG_VAR_VAL_OUTPUT_CONTENT;
import static iodetection.IOWriterIT.SAMPLE_INPUT_CONTENT;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IOReaderIT {
    private IOReader reader;
    @TempDir
    private Path tempDir;

    private Path tempFile;

    private List<IOResult> expectedInputs;
    private IOResult expectedWrongVarValOutput;
    private IOResult expectedWrongBranchOutput;

    @BeforeEach
    void setUp() {
        reader = new IOReader();
        tempFile = tempDir.resolve(TEMP_FILE_NAME);
        expectedInputs = new ArrayList<>();
        expectedInputs.add(new IOResult(String.format(INPUT_VAR_ID_FMT, 1), 1));
        expectedInputs.add(new IOResult(String.format(INPUT_VAR_ID_FMT, 2), 2));
        expectedWrongVarValOutput = new IOResult(OUTPUT_VAR_ID, 1);
        expectedWrongBranchOutput = new IOResult(null, 1);
    }

    @Nested
    class ReadInputs {
        @Test
        void readInputs_FileWithInputsAndOutputs_GetsInputsCorrectly() throws IOException {
            Files.writeString(tempFile, SAMPLE_WRONG_VAR_VAL_FILE_CONTENT);
            List<IOResult> inputs = reader.readInputs(tempFile).get();
            assertEquals(expectedInputs, inputs);
        }

        @Test
        void readInputs_FileWithNoInputs_GetsInputsCorrectly() throws IOException {
            Files.writeString(tempFile, System.lineSeparator() + SAMPLE_WRONG_VAR_VAL_OUTPUT_CONTENT);
            Optional<List<IOResult>> inputs = reader.readInputs(tempFile);
            assertTrue(inputs.isEmpty());
        }

        @Test
        void readInputs_FileWithNoOutputs_GetsInputsCorrectly() throws IOException {
            Files.writeString(tempFile, SAMPLE_INPUT_CONTENT + System.lineSeparator());
            List<IOResult> inputs = reader.readInputs(tempFile).get();
            assertEquals(expectedInputs, inputs);
        }
    }

    @Nested
    class ReadOutput {
        @Test
        void readOutput_FileWithInputsAndWrongVarValueOutput_GetsOutputCorrectly() throws IOException {
            Files.writeString(tempFile, SAMPLE_WRONG_VAR_VAL_FILE_CONTENT);
            IOResult output = reader.readOutput(tempFile).get();
            assertEquals(expectedWrongVarValOutput, output);
        }

        @Test
        void readOutput_FileWithInputsAndWrongBranchOutput_GetsOutputCorrectly() throws IOException {
            Files.writeString(tempFile, SAMPLE_WRONG_BRANCH_FILE_CONTENT);
            IOResult output = reader.readOutput(tempFile).get();
            assertEquals(expectedWrongBranchOutput, output);
        }

        @Test
        void readOutput_FileWithNoInputs_GetsOutputCorrectly() throws IOException {
            Files.writeString(tempFile, System.lineSeparator() + SAMPLE_WRONG_VAR_VAL_OUTPUT_CONTENT);
            IOResult output = reader.readOutput(tempFile).get();
            assertEquals(expectedWrongVarValOutput, output);
        }

        @Test
        void readOutput_FileWithNoOutput_GetsOutputCorrectly() throws IOException {
            Files.writeString(tempFile, SAMPLE_INPUT_CONTENT + System.lineSeparator());
            Optional<IOResult> output = reader.readOutput(tempFile);
            assertTrue(output.isEmpty());
        }
    }
}
