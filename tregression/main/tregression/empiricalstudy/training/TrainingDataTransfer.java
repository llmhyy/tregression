package tregression.empiricalstudy.training;

import java.util.ArrayList;
import java.util.List;

import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.model.variable.ArrayElementVar;
import microbat.model.variable.FieldVar;
import microbat.model.variable.LocalVar;
import microbat.model.variable.Variable;
import microbat.recommendation.DataOmissionInspector;
import microbat.recommendation.InspectingRange;
import microbat.recommendation.calculator.Dependency;
import microbat.recommendation.calculator.DependencyCalculator;
import microbat.recommendation.calculator.TraceTraverse;
import microbat.recommendation.calculator.TraceTraversingDistanceCalculator;
import microbat.recommendation.calculator.ASTTraverse;
import microbat.recommendation.calculator.ASTTraversingDistanceCalculator;
import microbat.recommendation.calculator.VariableSimilarity;
import microbat.recommendation.calculator.VariableSimilarityCalculator;
import microbat.util.Settings;
import sav.strategies.dto.AppJavaClassPath;
import tregression.empiricalstudy.DeadEndRecord;
import tregression.empiricalstudy.solutionpattern.SolutionPattern;

public class TrainingDataTransfer {
	private List<TraceNode> criticalConditionalSteps = new ArrayList<>();
	private boolean usingCache = false;
	private boolean isCached = false;
	
	public DED transfer(DeadEndRecord record, Trace buggyTrace){
		Settings.compilationUnitMap.clear();
		Settings.iCompilationUnitMap.clear();
		
		DeadEndData trueData = null;
		List<DeadEndData> falseDatas = new ArrayList<>();
		
		int start = record.getDeadEndOrder();
		int end = record.getOccurOrder();
		
		TraceNode breakStep = buggyTrace.getTraceNode(record.getBreakStepOrder());
		TraceNode occurStep = buggyTrace.getTraceNode(record.getOccurOrder());
		TraceNode deadEndStep = buggyTrace.getTraceNode(record.getDeadEndOrder());
		
		if(record.getType()==DeadEndRecord.CONTROL){
			trueData = transferControl(true, occurStep, breakStep, deadEndStep);
			
			for(int i=start+1; i<end; i++){
				TraceNode step = buggyTrace.getTraceNode(i);
				if(!step.getBreakPoint().equals(breakStep.getBreakPoint())){
					if(step.getMethodSign().equals(occurStep.getMethodSign())){
						DeadEndData d = transferControl(false, occurStep, step, deadEndStep);
						falseDatas.add(d);
					}
					
				}
			}
		}
		else{
			VarValue wrongVar = record.getVarValue();

			InspectingRange range = new InspectingRange(deadEndStep, occurStep);
			DataOmissionInspector inspector = new DataOmissionInspector();
			inspector.setInspectingRange(range);
			List<TraceNode> criticalConditionalSteps = new ArrayList<>();
			if(usingCache && isCached){
				criticalConditionalSteps = this.criticalConditionalSteps;
			}
			else{
				criticalConditionalSteps = inspector.analyze(wrongVar);
				this.criticalConditionalSteps = criticalConditionalSteps;	
				this.isCached = true;
			}
			
			if(wrongVar.getVariable() instanceof LocalVar && !wrongVar.getVarName().equals("this")){
				if(occurStep.getMethodSign().equals(breakStep.getMethodSign())){
					trueData = transferData(true, occurStep, breakStep, deadEndStep, 
							wrongVar, criticalConditionalSteps);									
				}
			}
			else{
				trueData = transferData(true, occurStep, breakStep, deadEndStep, 
						wrongVar, criticalConditionalSteps);
			}
			
			System.currentTimeMillis();
			
			for(int i=start+1; i<end; i++){
				TraceNode step = buggyTrace.getTraceNode(i);
				if(!step.getBreakPoint().equals(breakStep.getBreakPoint())){
					if(wrongVar.getVariable() instanceof LocalVar){
						if(step.getInvocationLevel()==occurStep.getInvocationLevel()){
							boolean label = identifyDataLabel(step, breakStep, record.getSolutionPattern());
							DeadEndData d = transferData(label, occurStep, step, deadEndStep, 
									wrongVar, criticalConditionalSteps);
							falseDatas.add(d);	
						}
					}
					else{
						boolean label = identifyDataLabel(step, breakStep, record.getSolutionPattern());
						DeadEndData d = transferData(label, occurStep, step, deadEndStep, 
								wrongVar, criticalConditionalSteps);
						falseDatas.add(d);						
					}
				}
			}
		}
		
		DED ded = new DED(trueData, falseDatas);
		return ded;
	}
	
	private boolean identifyDataLabel(TraceNode step, TraceNode breakStep, SolutionPattern pattern){
		int errorBound = 1;
		
		if(pattern==null){
			errorBound = 0;
		}
		else if(pattern.getType()==SolutionPattern.INCORRECT_ASSIGNMENT || 
				pattern.getType()==SolutionPattern.MISS_EVALUATED_CONDITION ||
				pattern.getType()==SolutionPattern.INCORRECT_CONDITION ||
				pattern.getType()==SolutionPattern.INVOKE_DIFFERENT_METHOD){
			errorBound = 0;
		}
		
		if(step.getMethodSign().equals(breakStep.getMethodSign())){
			if(Math.abs(step.getLineNumber()-breakStep.getLineNumber())<=errorBound){
				return true;
			}
		}
		
		return false;
	}

	private int getDataType(VarValue wrongValue){
		Variable var = wrongValue.getVariable();
		if(var instanceof LocalVar){
			return DataDeadEndData.LOCAL_VAR;
		}
		else if(var instanceof FieldVar){
			return DataDeadEndData.FIELD;
		}
		else if(var instanceof ArrayElementVar){
			return DataDeadEndData.FIELD;
//			return DataDeadEndData.ARRAY_ELEMENT;
		}
		
		return -1;
	}
	
	private DeadEndData transferData(boolean label, TraceNode occurStep, TraceNode step, 
			TraceNode deadEndStep, VarValue wrongValue, List<TraceNode> criticalConditionalSteps) {
		DataDeadEndData data = new DataDeadEndData();
		data.isBreakStep = label ? 1 : 0;
		data.type = getDataType(wrongValue);
		
		boolean isCriticalStep = criticalConditionalSteps.contains(step);
		data.criticalConditionalStep = isCriticalStep ? 1 : 0;
		
		AppJavaClassPath appPath = occurStep.getTrace().getAppJavaClassPath();
		ASTTraversingDistanceCalculator tCal = 
				new ASTTraversingDistanceCalculator(appPath);
		ASTTraverse traverse = tCal.calculateASTTravsingDistance(step.getBreakPoint(), occurStep.getBreakPoint());
		data.astMoveUps = traverse.getMoveUps();
		data.astMoveRights = traverse.getMoveRights();
		data.astMoveDowns = traverse.getMoveDowns();
		
		TraceTraverse traceTraverse = new TraceTraversingDistanceCalculator().calculateASTTravsingDistance(step, occurStep);
		data.traceMoveOuts = traceTraverse.getMoveOuts();
		data.traceMoveIns = traceTraverse.getMoveIns();
		data.traceMoveDowns = traceTraverse.getMoveDowns();
		
		VariableSimilarityCalculator varCal = new VariableSimilarityCalculator(wrongValue);
		VariableSimilarity[] vs = varCal.calculateVarSimilarity(step);
		
		data.sameRLocalVarType = vs[0].isSameLocalVarType;
		data.sameRLocalVarName = vs[0].isSameLocalVarName;
		data.sameRFieldParent = vs[0].isSameFieldParent;
		data.sameRFieldType = vs[0].isSameFieldType;
		data.sameRFieldParentType = vs[0].isSameFieldParentType;
		data.sameRFieldName = vs[0].isSameFieldName;
		data.sameRArrayType = vs[0].isSameArrayType;
		data.sameRArrayParent = vs[0].isSameArrayParent;
		data.sameRArrayIndex = vs[0].isSameArrayIndex;
		
		data.sameWLocalVarType = vs[1].isSameLocalVarType;
		data.sameWLocalVarName = vs[1].isSameLocalVarName;
		data.sameWFieldParent = vs[1].isSameFieldParent;
		data.sameWFieldParentType = vs[1].isSameFieldParentType;
		data.sameWFieldType = vs[1].isSameFieldType;
		data.sameWFieldName = vs[1].isSameFieldName;
		data.sameWArrayType = vs[1].isSameArrayType;
		data.sameWArrayParent = vs[1].isSameArrayParent;
		data.sameWArrayIndex = vs[1].isSameArrayIndex;
		
		data.traceOrder = step.getOrder();
		data.setASTInfo(step, occurStep, deadEndStep);
		return data;
	}
	
	private DeadEndData transferControl(boolean label, TraceNode occurStep, TraceNode step, TraceNode deadEndStep) {
		ControlDeadEndData data = new ControlDeadEndData();
		data.isBreakStep = label ? 1 : 0;
		
		AppJavaClassPath appPath = occurStep.getTrace().getAppJavaClassPath();
		
		ASTTraversingDistanceCalculator tCal = 
				new ASTTraversingDistanceCalculator(appPath);
		ASTTraverse traverse = tCal.calculateASTTravsingDistance(step.getBreakPoint(), occurStep.getBreakPoint());
		data.astMoveUps = traverse.getMoveUps();
		data.astMoveRights = traverse.getMoveRights();
		data.astMoveDowns = traverse.getMoveDowns();
		
		TraceTraverse traceTraverse = new TraceTraversingDistanceCalculator().calculateASTTravsingDistance(step, occurStep);
		data.traceMoveOuts = traceTraverse.getMoveOuts();
		data.traceMoveIns = traceTraverse.getMoveIns();
		data.traceMoveDowns = traceTraverse.getMoveDowns();
		
		DependencyCalculator dCal = new DependencyCalculator(appPath);
		Dependency dependency = dCal.calculateDependency(step.getBreakPoint(), occurStep.getBreakPoint());
		
		data.controlDependency = dependency.getControlDependency();
		data.dataDependency = dependency.getDataDependency();
		
		data.traceOrder = step.getOrder();
		
		data.setASTInfo(step, occurStep, deadEndStep);
		
		return data;
	}

	public List<TraceNode> getCriticalConditionalSteps() {
		return criticalConditionalSteps;
	}

	public void setCriticalConditionalSteps(List<TraceNode> criticalConditionalSteps) {
		this.criticalConditionalSteps = criticalConditionalSteps;
	}

	public boolean isUsingCache() {
		return usingCache;
	}

	public void setUsingCache(boolean usingCache) {
		this.usingCache = usingCache;
	}

	public boolean isCached() {
		return isCached;
	}

	public void setCached(boolean isCached) {
		this.isCached = isCached;
	}

	
}
