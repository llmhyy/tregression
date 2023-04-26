package iodetection;

import java.nio.file.Path;
import java.util.List;

import java.io.FileWriter;  
import java.io.IOException;

import microbat.model.value.VarValue;

public class IOWriter {

	public void writeIO(List<VarValue> inputs, List<VarValue> outputs, final String path) {
		//TODO
		// Write the variable id to the target file in the following format
		// first line: input_var_id_1 input_var_id_2 input_var_id_3 ...
		// second line: output_var_id_1 output_var_id_2 ...
		// var_id are separated by space
		// var_id = var.getVarID;
		String var_id = inputs.get(0).getVarID();
	}
	
}
