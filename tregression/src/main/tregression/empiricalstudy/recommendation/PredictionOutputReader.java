/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package tregression.empiricalstudy.recommendation;

import java.io.BufferedReader;
import java.io.IOException;

import sav.common.core.utils.SingleTimer;
import sav.strategies.vm.interprocess.TcpOutputReader;

/**
 * @author LLT
 *
 */
public class PredictionOutputReader extends TcpOutputReader {
	private static final String OUTPUT_START_TOKEN = "@@PythonStart@@";
	private static final String OUTPUT_END_TOKEN = "@@PythonEnd@@";
	private volatile PythonOutput predicutResult;
	
	public PredictionOutputReader() {
		waiting();
	}
	
	public boolean isMatched(String line) {
		return OUTPUT_START_TOKEN.equals(line);
	}

	@Override
	protected void readData(BufferedReader br) {
		String line = null;
		PythonOutput output = new PythonOutput();
		try {
			while ((line = br.readLine()) != null) {
				if (line.endsWith(OUTPUT_END_TOKEN)) {
					this.predicutResult = output;
					ready();
					return;
				}
				output.add(line);
			}
		} catch (IOException e) {
			// do nothing
		}
	}

	public PythonOutput readOutput() {
		SingleTimer timer = SingleTimer.start("read output");
		while (isWaiting()) {
			if (timer.getExecutionTime() > 1000l) {
				System.out.println("timeout!");
				return null;
			}
		}
		PythonOutput output = predicutResult;
		predicutResult = null;
		waiting();
		return output;
	}

}
