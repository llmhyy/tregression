package iodetection;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import iodetection.IODetector.NodeVarValPair;

import java.io.IOException;

import microbat.model.value.VarValue;

public class IOWriter {
	static final String IO_DELIMITER = " ";

	public void writeIO(List<NodeVarValPair> inputs, NodeVarValPair output, final Path filePath) throws IOException {
		String ioStr = formIOStr(inputs, output);
		Files.write(filePath, ioStr.getBytes());
	}

	private String formVarValueRow(List<NodeVarValPair> nodeVarValPairs) {
		StringBuilder strBuilder = new StringBuilder();
		for (NodeVarValPair nodeVarValPair : nodeVarValPairs) {
		    strBuilder.append(formSingleIOStr(nodeVarValPair));
	        strBuilder.append(IO_DELIMITER);
		}
		int len = strBuilder.length();
		if (len > 0) {
			strBuilder.deleteCharAt(len - 1);
		}
		return strBuilder.toString();
	}

	String formIOStr(List<NodeVarValPair> inputs, NodeVarValPair output) {
		String inputRow = formVarValueRow(inputs);
		return String.join(System.lineSeparator(), inputRow, formSingleIOStr(output)) + System.lineSeparator();
	}
	
	private StringBuilder formSingleIOStr(NodeVarValPair nodeVarValPair) {
	    StringBuilder strBuilder = new StringBuilder();
        if (nodeVarValPair == null) return strBuilder;
        strBuilder.append(nodeVarValPair.getVarVal().getVarID());
        strBuilder.append(IO_DELIMITER);
        strBuilder.append(nodeVarValPair.getNode().getOrder());
        return strBuilder;
	}
}
