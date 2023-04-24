/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package tregression.empiricalstudy.recommendation;

import java.io.OutputStream;
import java.io.PrintWriter;

import sav.strategies.vm.interprocess.TcpInputWriter;
import tregression.empiricalstudy.training.DeadEndData;

/**
 * @author LLT
 *
 */
public class RecordWriter extends TcpInputWriter {
	private DeadEndData record;
	private PrintWriter pw;
	
	public RecordWriter() {
		waiting();
	}
	
	public void sendData(DeadEndData input) {
		if (isClosed()) {
			throw new IllegalStateException("InputWriter is closed!");
		}
		this.record = input;
		ready();
	}
	
	@Override
	protected void writeData() {
		String plainText = record.getPlainText("-1", "-1");
		pw.println(plainText);
		record = null;
	}
	
	public void setOutputStream(OutputStream outputStream) {
		this.pw = new PrintWriter(outputStream, true);
	}

	@Override
	public void close() {
		super.close();
		if (pw != null) {
			pw.close();
		}
	}
	
}
