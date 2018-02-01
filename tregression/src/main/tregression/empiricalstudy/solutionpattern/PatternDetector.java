package tregression.empiricalstudy.solutionpattern;

import tregression.empiricalstudy.DeadEndRecord;
import tregression.empiricalstudy.EmpiricalTrial;

public abstract class PatternDetector {
	public abstract boolean detect(DeadEndRecord deadEndRecord, EmpiricalTrial trial);
	public abstract SolutionPattern getSolutionPattern();
}
