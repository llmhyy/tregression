package tregression.empiricalstudy.solutionpattern;

import java.util.ArrayList;
import java.util.List;

import tregression.empiricalstudy.DeadEndRecord;
import tregression.empiricalstudy.EmpiricalTrial;

public class PatternIdentifier {
	
	List<PatternDetector> patternDetectors = new ArrayList<>();
	
	public PatternIdentifier(){
		//TODO initialize the pattern detectors
		patternDetectors.add(new MissingIfThrow());
		patternDetectors.add(new MissingIfReturn());
		patternDetectors.add(new IncorrectCondition());
		patternDetectors.add(new MissingAssignment());
		patternDetectors.add(new ExtraNestedIfBlock());
		patternDetectors.add(new MissingIfBlockBody());
		patternDetectors.add(new IncorrectAssignment());
	}
	
	public void identifyPattern(EmpiricalTrial trial){
		for(DeadEndRecord record: trial.getDeadEndRecordList()){
			identifyPattern(record, trial);
		}
	}

	private void identifyPattern(DeadEndRecord record, EmpiricalTrial trial) {
		for(PatternDetector detector: patternDetectors){
			boolean detected = detector.detect(record, trial);
			if(detected){
				record.setSolutionPattern(detector.getSolutionPattern());
				break;
			}
		}
	}
}
