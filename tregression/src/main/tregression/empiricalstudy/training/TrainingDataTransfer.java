package tregression.empiricalstudy.training;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;

import microbat.codeanalysis.ast.ASTEncoder;
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
import microbat.recommendation.calculator.Traverse;
import microbat.recommendation.calculator.TraversingDistanceCalculator;
import microbat.recommendation.calculator.VariableSimilarity;
import microbat.recommendation.calculator.VariableSimilarityCalculator;
import microbat.util.JavaUtil;
import microbat.util.MinimumASTNodeFinder;
import microbat.util.Settings;
import sav.strategies.dto.AppJavaClassPath;
import tregression.empiricalstudy.DeadEndRecord;

public class TrainingDataTransfer {
	public DED transfer(DeadEndRecord record, Trace buggyTrace){
		Settings.compilationUnitMap.clear();
		Settings.iCompilationUnitMap.clear();
		
		DeadEndData trueData;
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
			List<TraceNode> criticalConditionalSteps = inspector.analyze(wrongVar);
			
			trueData = transferData(true, occurStep, breakStep, deadEndStep, 
					wrongVar, criticalConditionalSteps);
			
			for(int i=start+1; i<end; i++){
				TraceNode step = buggyTrace.getTraceNode(i);
				if(!step.getBreakPoint().equals(breakStep.getBreakPoint())){
					if(wrongVar.getVariable() instanceof LocalVar){
						if(step.getInvocationLevel()==occurStep.getInvocationLevel()){
							boolean label = identifyDataLabel(step, breakStep);
							DeadEndData d = transferData(label, occurStep, step, deadEndStep, 
									wrongVar, criticalConditionalSteps);
							falseDatas.add(d);	
						}
					}
					else{
						boolean label = identifyDataLabel(step, breakStep);
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
	
	private boolean identifyDataLabel(TraceNode step, TraceNode breakStep){
		if(step.getMethodSign().equals(breakStep.getMethodSign())){
			if(Math.abs(step.getLineNumber()-breakStep.getLineNumber())<=1){
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
			return DataDeadEndData.ARRAY_ELEMENT;
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
		
		data.setASTInfo(step, occurStep, deadEndStep);
		
		return data;
	}
}
