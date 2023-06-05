package iodetection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import iodetection.IODetector.NodeVarValPair;
import microbat.model.trace.TraceNode;
import microbat.model.value.GraphNode;
import microbat.model.value.VarValue;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IOWriterIT {
    static final String TEMP_FILE_NAME = "io.txt";
    static final String INPUT_VAR_ID_FMT = "input-%d";
    static final String OUTPUT_VAR_ID = "output-1";
    static final String SAMPLE_INPUT_CONTENT = String.join(" ", String.format(INPUT_VAR_ID_FMT, 1), "1", 
            String.format(INPUT_VAR_ID_FMT, 2), "2") + System.lineSeparator();
    static final String SAMPLE_WRONG_VAR_VAL_OUTPUT_CONTENT = OUTPUT_VAR_ID + " " + 1 + System.lineSeparator();;
    static final String SAMPLE_WRONG_VAR_VAL_FILE_CONTENT = SAMPLE_INPUT_CONTENT + SAMPLE_WRONG_VAR_VAL_OUTPUT_CONTENT;
    static final String SAMPLE_WRONG_BRANCH_FILE_CONTENT = SAMPLE_INPUT_CONTENT + 1 + System.lineSeparator();

    private IOWriter writer;

    @TempDir
    private Path tempDir;

    private Path tempFile;

    private List<NodeVarValPair> inputs;
    private NodeVarValPair wrongVarValOutput;
    private NodeVarValPair wrongBranchOutput;

    @BeforeEach
    void setUp() {
        writer = new IOWriter();
        tempFile = tempDir.resolve(TEMP_FILE_NAME);

        inputs = new ArrayList<>();
        inputs.add(createNodeVarValPair(1));
        inputs.add(createNodeVarValPair(2));
        
        TraceNode outputNode = new TraceNode();
        outputNode.setOrder(1);
        VarValue varValue = new VarValueStub(OUTPUT_VAR_ID);
        wrongVarValOutput = new NodeVarValPair(outputNode, varValue);
        wrongBranchOutput = new NodeVarValPair(outputNode, null);
    }

    private NodeVarValPair createNodeVarValPair(int id) {
        TraceNode node = new TraceNode();
        node.setOrder(id);
        VarValue varValue = new VarValueStub(String.format(INPUT_VAR_ID_FMT, id));
        return new NodeVarValPair(node, varValue);
    }

    @Test
    void writeIO_InputsAndWrongVarValOutputProvided_WritesIO() throws IOException {
        writer.writeIO(inputs, wrongVarValOutput, tempFile);
        String fileContents = Files.readString(tempFile);
        assertEquals(SAMPLE_WRONG_VAR_VAL_FILE_CONTENT, fileContents);
    }
    
    @Test
    void writeIO_InputsAndWrongBranchOutputProvided_WritesIO() throws IOException {
        writer.writeIO(inputs, wrongBranchOutput, tempFile);
        String fileContents = Files.readString(tempFile);
        assertEquals(SAMPLE_WRONG_BRANCH_FILE_CONTENT, fileContents);
    }

    @Test
    void writeIO_InputsIsEmpty_WritesOutputCorrectly() throws IOException {
        inputs = new ArrayList<>();
        writer.writeIO(inputs, wrongVarValOutput, tempFile);
        String fileContents = Files.readString(tempFile);
        assertEquals(System.lineSeparator() + SAMPLE_WRONG_VAR_VAL_OUTPUT_CONTENT, fileContents);
    }

    @Test
    void writeIO_OutputIsEmpty_WritesInputsCorrectly() throws IOException {
        wrongVarValOutput = null;
        writer.writeIO(inputs, wrongVarValOutput, tempFile);
        String fileContents = Files.readString(tempFile);
        assertEquals(SAMPLE_INPUT_CONTENT + System.lineSeparator(), fileContents);
    }

    @Test
    void writeIO_WritesMultipleTimes_Overwrites() throws IOException {
        int count = 3;
        for (int i = 0; i < count; i++) {
            writer.writeIO(inputs, wrongVarValOutput, tempFile);
        }
        String fileContents = Files.readString(tempFile);
        assertEquals(SAMPLE_WRONG_VAR_VAL_FILE_CONTENT, fileContents);
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
