package tregression.empiricalstudy.training;

import java.util.ArrayList;
import java.util.List;

import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.model.variable.LocalVar;
import microbat.recommendation.DataOmissionInspector;
import microbat.recommendation.InspectingRange;
import microbat.recommendation.calculator.Dependency;
import microbat.recommendation.calculator.DependencyCalculator;
import microbat.recommendation.calculator.Traverse;
import microbat.recommendation.calculator.TraversingDistanceCalculator;
import microbat.recommendation.calculator.VariableSimilarity;
import microbat.recommendation.calculator.VariableSimilarityCalculator;
import sav.strategies.dto.AppJavaClassPath;
import tregression.empiricalstudy.DeadEndRecord;

public class TrainingDataTransfer {
	public DED transfer(DeadEndRecord record, Trace buggyTrace){
		DeadEndData trueData;
		List<DeadEndData> falseDatas = new ArrayList<>();
		
		int start = record.getDeadEndOrder();
		int end = record.getOccurOrder();
		
		TraceNode breakStep = buggyTrace.getTraceNode(record.getBreakStepOrder());
		TraceNode occurStep = buggyTrace.getTraceNode(record.getOccurOrder());
		TraceNode deadEndStep = buggyTrace.getTraceNode(record.getDeadEndOrder());
		
		if(record.getType()==DeadEndRecord.CONTROL){
			trueData = transferControl(true, occurStep, breakStep);
			
			for(int i=start; i<end; i++){
				TraceNode step = buggyTrace.getTraceNode(i);
				if(!step.getBreakPoint().equals(breakStep.getBreakPoint())){
					DeadEndData d = transferControl(false, occurStep, step);
					falseDatas.add(d);
				}
			}
		}
		else{
			VarValue wrongVar = record.getVarValue();

			InspectingRange range = new InspectingRange(deadEndStep, occurStep);
			DataOmissionInspector inspector = new DataOmissionInspector();
			inspector.setInspectingRange(range);
			List<TraceNode> criticalConditionalSteps = inspector.analyze(wrongVar);
			
			trueData = transferData(true, occurStep, breakStep, deadEndStep, 
					wrongVar, criticalConditionalSteps);
			
			for(int i=start; i<end; i++){
				TraceNode step = buggyTrace.getTraceNode(i);
				if(!step.getBreakPoint().equals(breakStep.getBreakPoint())){
					if(wrongVar.getVariable() instanceof LocalVar){
						if(step.getInvocationLevel()==occurStep.getInvocationLevel()){
							DeadEndData d = transferData(false, occurStep, step, deadEndStep, 
									wrongVar, criticalConditionalSteps);
							falseDatas.add(d);	
						}
					}
					else{
						DeadEndData d = transferData(false, occurStep, step, deadEndStep, 
								wrongVar, criticalConditionalSteps);
						falseDatas.add(d);						
					}
				}
			}
		}
		
		return new DED(trueData, falseDatas);
	}

	private DeadEndData transferData(boolean label, TraceNode occurStep, TraceNode step, 
			TraceNode deadEndStep, VarValue wrongValue, List<TraceNode> criticalConditionalSteps) {
		DataDeadEndData data = new DataDeadEndData();
		data.isBreakStep = label ? 1 : 0;
		
		boolean isCriticalStep = criticalConditionalSteps.contains(step);
		data.criticalConditionalStep = isCriticalStep ? 1 : 0;
		
		VariableSimilarityCalculator varCal = new VariableSimilarityCalculator(wrongValue);
		VariableSimilarity[] vs = varCal.calculateVarSimilarity(step);
		
		data.sameRLocalVarType = vs[0].isSameLocalVarType;
		data.sameRLocalVarName = vs[0].isSameLocalVarName;
		data.sameRFieldParent = vs[0].isSameFieldParent;
		data.sameRFieldType = vs[0].isSameFieldType;
		data.sameRFieldName = vs[0].isSameFieldName;
		data.sameRArrayType = vs[0].isSameArrayType;
		data.sameRArrayParent = vs[0].isSameArrayParent;
		data.sameRArrayIndex = vs[0].isSameArrayIndex;
		
		data.sameWLocalVarType = vs[1].isSameLocalVarType;
		data.sameWLocalVarName = vs[1].isSameLocalVarName;
		data.sameWFieldParent = vs[1].isSameFieldParent;
		data.sameWFieldType = vs[1].isSameFieldType;
		data.sameWFieldName = vs[1].isSameFieldName;
		data.sameWArrayType = vs[1].isSameArrayType;
		data.sameWArrayParent = vs[1].isSameArrayParent;
		data.sameWArrayIndex = vs[1].isSameArrayIndex;
		
		data.traceOrder = step.getOrder();
		
		return data;
	}
	
	private DeadEndData transferControl(boolean label, TraceNode occurStep, TraceNode step) {
		ControlDeadEndData data = new ControlDeadEndData();
		data.isBreakStep = label ? 1 : 0;
		
		AppJavaClassPath appPath = occurStep.getTrace().getAppJavaClassPath();
		
		TraversingDistanceCalculator tCal = 
				new TraversingDistanceCalculator(appPath);
		Traverse traverse = tCal.calculateASTTravsingDistance(step.getBreakPoint(), occurStep.getBreakPoint());
		data.moveUps = traverse.getMoveUps();
		data.moveRights = traverse.getMoveRights();
		data.moveDowns = traverse.getMoveDowns();
		
		DependencyCalculator dCal = new DependencyCalculator(appPath);
		Dependency dependency = dCal.calculateDependency(step.getBreakPoint(), occurStep.getBreakPoint());
		
		data.controlDependency = dependency.getControlDependency();
		data.dataDependency = dependency.getDataDependency();
		
		data.traceOrder = step.getOrder();
		
		return data;
	}
}
