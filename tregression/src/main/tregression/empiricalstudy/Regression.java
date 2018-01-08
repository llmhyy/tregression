package tregression.empiricalstudy;

import microbat.model.trace.Trace;
import tregression.model.PairList;

public class Regression {
	private Trace buggyTrace;
	private Trace correctTrace;
	private PairList pairList;

	public Regression(Trace buggyTrace, Trace correctTrace, PairList pairList) {
		super();
		this.buggyTrace = buggyTrace;
		this.correctTrace = correctTrace;
		this.pairList = pairList;
	}

	public Trace getBuggyTrace() {
		return buggyTrace;
	}

	public void setBuggyTrace(Trace buggyTrace) {
		this.buggyTrace = buggyTrace;
	}

	public Trace getCorrectTrace() {
		return correctTrace;
	}

	public void setCorrectTrace(Trace correctTrace) {
		this.correctTrace = correctTrace;
	}

	public PairList getPairList() {
		return pairList;
	}

	public void setPairList(PairList pairList) {
		this.pairList = pairList;
	}

}
