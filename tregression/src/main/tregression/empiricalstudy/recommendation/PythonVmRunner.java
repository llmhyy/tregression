/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package tregression.empiricalstudy.recommendation;

import java.util.ArrayList;
import java.util.List;

import sav.common.core.SavException;
import sav.strategies.vm.interprocess.InterprocessVmRunner;

/**
 * @author LLT
 *
 */
public class PythonVmRunner extends InterprocessVmRunner {
	
	public PythonVmRunner(RecordWriter inputWriter, PredictionOutputReader outputReader) {
		super(inputWriter, outputReader, true);
	}

	public void start(String pythonHome, String predictorFile, String workingDir) throws SavException {
		List<String> commands = new ArrayList<String>();
		commands.add(pythonHome);
		commands.add(predictorFile);
		
		this.setWorkingDir(workingDir);
		super.startVm(commands, false);
	}
}
