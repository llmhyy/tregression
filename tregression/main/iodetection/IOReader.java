package iodetection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class IOReader {

    public Optional<List<IOResult>> readInputs(final Path path) {
        try {
            String[] lines = getLines(path);
            String inputLine = lines[0];
            if (inputLine.isEmpty()) {
                return Optional.empty();
            }
            List<IOResult> result = new ArrayList<>();
            String[] splitLine = inputLine.split(IOWriter.IO_DELIMITER);
            for (int i = 0; i < splitLine.length; i += 2) {
                result.add(new IOResult(splitLine[i], Integer.valueOf(splitLine[i + 1])));
            }
            return Optional.of(result);
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public Optional<IOResult> readOutput(final Path path) {
        try {
            String[] lines = getLines(path);
            String[] outputStrs = lines[1].split(IOWriter.IO_DELIMITER);
            int outputStrsLen = outputStrs.length;
            if (outputStrsLen == 2) {
                // Wrong Var Value
                return Optional.of(new IOResult(outputStrs[0], Integer.valueOf(outputStrs[1])));
            }
            // Wrong Branch
            return Optional.of(new IOResult(null, Integer.valueOf(outputStrs[0])));
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        } catch (IndexOutOfBoundsException e) {
            return Optional.empty();
        }
    }

    private String[] getLines(final Path path) throws IOException {
        String fileContents = Files.readString(path);
        return fileContents.split(System.lineSeparator());
    }

    public static class IOResult {
        private final String varValStr;
        private final int nodeOrder;

        public IOResult(String varValStr, int nodeOrder) {
            super();
            this.varValStr = varValStr;
            this.nodeOrder = nodeOrder;
        }

        public String getVarValStr() {
            return varValStr;
        }

        public int getNodeOrder() {
            return nodeOrder;
        }

        @Override
        public int hashCode() {
            return Objects.hash(nodeOrder, varValStr);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            IOResult other = (IOResult) obj;
            return nodeOrder == other.nodeOrder && Objects.equals(varValStr, other.varValStr);
        }

    }
}
