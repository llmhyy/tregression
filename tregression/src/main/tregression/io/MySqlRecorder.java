package tregression.io;

import java.util.List;

import microbat.model.trace.Trace;
import tregression.empiricalstudy.EmpiricalTrial;
import tregression.empiricalstudy.MendingRecord;
import tregression.model.PairList;
import tregression.separatesnapshots.DiffMatcher;

public class MySqlRecorder {

	/**
	 * The mending information can be retrieved through trial.
	 * 
	 * @param trial
	 * @param buggyTrace
	 * @param correctTrace
	 * @param diffMatcher
	 * @param pairList
	 * @param realcauseNode
	 */
	public void record(EmpiricalTrial trial, Trace buggyTrace, Trace correctTrace, 
			DiffMatcher diffMatcher, PairList pairList) {
		// TODO Auto-generated method stub
		List<MendingRecord> mendingRecords = trial.getRootCauseFinder().getMendingRecordList();
		
	}
	
}
