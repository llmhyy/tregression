/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package tregression.empiricalstudy.recommendation;

/**
 * @author LLT
 *
 */
public class PythonOutput {
	private StringBuffer sb = new StringBuffer();

	public void add(String line) {
		sb.append(line).append("\n");
	}
	
	@Override
	public String toString() {
		return sb.toString();
	}
}
