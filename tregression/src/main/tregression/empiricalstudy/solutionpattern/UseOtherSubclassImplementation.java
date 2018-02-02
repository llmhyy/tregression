package tregression.empiricalstudy.solutionpattern;

import tregression.empiricalstudy.DeadEndRecord;
import tregression.empiricalstudy.EmpiricalTrial;

public class UseOtherSubclassImplementation extends PatternDetector{

	@Override
	public boolean detect(DeadEndRecord deadEndRecord, EmpiricalTrial trial) {
		
		
		return false;
	}

	@Override
	public SolutionPattern getSolutionPattern() {
		// TODO Auto-generated method stub
		return null;
	}
	
}
