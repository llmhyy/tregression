package tregression.empiricalstudy.solutionpattern;

import microbat.model.trace.Trace;
import tregression.empiricalstudy.DeadEndRecord;
import tregression.empiricalstudy.EmpiricalTrial;

public class PatternIdentifier {
	
	
	public SolutionPattern identifyPattern(EmpiricalTrial trial, Trace buggyTrace){
		
		for(DeadEndRecord record: trial.getDeadEndRecordList()){
			SolutionPattern solutionPattern = identifyPattern(record);
		}
		
		return null;
	}

	private SolutionPattern identifyPattern(DeadEndRecord record) {
		if(record.getType()==DeadEndRecord.DATA){
			
		}
		else if(record.getType()==DeadEndRecord.CONTROL){
			
		}
		
		return null;
	}
}
