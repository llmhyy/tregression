package tregression;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import microbat.model.trace.TraceNode;
import microbat.model.value.PrimitiveValue;
import microbat.model.value.VarValue;
import microbat.model.variable.VirtualVar;
import microbat.recommendation.UserFeedback;
import sav.common.core.Pair;
import tregression.empiricalstudy.CausalityNode;
import tregression.empiricalstudy.RootCauseFinder;

public class StepChangeType {
	public static final int IDT = 0;
	public static final int SRC = 1;
	public static final int DAT = 2;
	public static final int CTL = 3;
	
	private int type;
	private TraceNode matchingStep;
	private List<Pair<VarValue, VarValue>> wrongVariableList;
	
	public StepChangeType(int type, TraceNode matchedStep) {
		super();
		this.type = type;
		this.matchingStep = matchedStep;
	}

	public StepChangeType(int type, TraceNode matchingStep, List<Pair<VarValue, VarValue>> wrongVariableList) {
		super();
		this.type = type;
		this.matchingStep = matchingStep;
		this.wrongVariableList = wrongVariableList;
	}
	
	public String getTypeString(){
		if(type==SRC){
			return UserFeedback.UNCLEAR;
		}
		else if(type==DAT){
			return UserFeedback.WRONG_VARIABLE_VALUE;
		}
		else if(type==CTL){
			return UserFeedback.WRONG_PATH;
		}
		else if(type==IDT){
			return UserFeedback.CORRECT;
		}
		
		return "unkown";
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public List<Pair<VarValue, VarValue>> getWrongVariableList() {
		return wrongVariableList;
	}

	public void setWrongVariableList(List<Pair<VarValue, VarValue>> wrongVariableList) {
		this.wrongVariableList = wrongVariableList;
	}

	public TraceNode getMatchingStep() {
		return matchingStep;
	}

	public void setMatchingStep(TraceNode matchingStep) {
		this.matchingStep = matchingStep;
	}

	public VarValue getWrongVariable(TraceNode node, boolean isOnBefore, RootCauseFinder finder) {
		Map<CausalityNode, VarValue> guidance = finder.getCausalityGraph().getGuidance();
		if(finder.getCausalityGraph().getGuidance()!=null){
			CausalityNode cNode = new CausalityNode(node, isOnBefore);
			VarValue value = guidance.get(cNode);
			if(value != null ){
				if(!node.getWrittenVariables().contains(value)){
					return value;					
				}
			}
		}
		
		List<VarValue> virList = new ArrayList<>();
		List<VarValue> primitiveList = new ArrayList<>();
		List<VarValue> referenceList = new ArrayList<>();
		for(Pair<VarValue, VarValue> pair: wrongVariableList){
			
			VarValue var = pair.first();
			if(!isOnBefore){
				var = pair.second();
			}
			
			String varID = var.getVarID();
			if(varID.endsWith(":0")){
				continue;
			}
			
			if(node.getWrittenVariables().contains(var)){
				continue;
			}
			
			if(var.getVariable() instanceof VirtualVar){
				virList.add(var);
			}
			else if(var instanceof PrimitiveValue){
				primitiveList.add(var);
			}
			else{
				referenceList.add(var);
			}
		}
		
		if(!primitiveList.isEmpty()){
			return primitiveList.get(0);
		}
		
		if(!referenceList.isEmpty()){
			return referenceList.get(0);
		}
		
		if(!virList.isEmpty()){
			return virList.get(0);			
		}
		
		Pair<VarValue, VarValue> pair = wrongVariableList.get(0);
		return isOnBefore? pair.first() : pair.second();
	}

}
