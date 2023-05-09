package iodetection;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import java.io.IOException;

import microbat.model.value.VarValue;

public class IOWriter {
	static final String VAR_VAL_DELIMITER = " ";

	public void writeIO(List<VarValue> inputs, VarValue output, final Path filePath)
			throws IOException {
		String ioStr = formIOStr(inputs, output, filePath);
		Files.write(filePath, ioStr.getBytes());
	}

	private String formVarValueRow(List<VarValue> varValues) {
		StringBuilder strBuilder = new StringBuilder();
		for (VarValue varValue : varValues) {
			strBuilder.append(varValue.getVarID());
			strBuilder.append(VAR_VAL_DELIMITER);
		}
		int len = strBuilder.length();
		if (len > 0) {
			strBuilder.deleteCharAt(len - 1);
		}
		return strBuilder.toString();
	}

	String formIOStr( List<VarValue> inputs, VarValue output, final Path filePath) {
		String inputRow = formVarValueRow(inputs);
		return String.join(System.lineSeparator(), inputRow, output.getVarID()) + System.lineSeparator();
	}
}
