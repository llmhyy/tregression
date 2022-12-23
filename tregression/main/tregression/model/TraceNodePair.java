package tregression.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import microbat.algorithm.graphdiff.GraphDiff;
import microbat.algorithm.graphdiff.HierarchyGraphDiffer;
import microbat.algorithm.graphdiff.SortedGraphMatcher;
import microbat.model.BreakPointValue;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.GraphNode;
import microbat.model.value.PrimitiveValue;
import microbat.model.value.ReferenceValue;
import microbat.model.value.VarValue;
import microbat.model.value.VirtualValue;
import microbat.model.variable.Variable;
import microbat.model.variable.VirtualVar;
import microbat.util.MicroBatUtil;

public class TraceNodePair {

	private TraceNode beforeNode;
	private TraceNode afterdNode;
	
	private boolean isExactlySame;
	
	public TraceNodePair(TraceNode beforeNode, TraceNode afterNode) {
		this.beforeNode = beforeNode;
		this.afterdNode = afterNode;
	}

	public TraceNode getBeforeNode() {
		return beforeNode;
	}

	public void setBeforeNode(TraceNode originalNode) {
		this.beforeNode = originalNode;
	}

	public TraceNode getAfterNode() {
		return afterdNode;
	}

	public void setAfterNode(TraceNode mutatedNode) {
		this.afterdNode = mutatedNode;
	}

	public void setExactSame(boolean b) {
		this.isExactlySame = b;
	}

	public boolean isExactSame(){
		return this.isExactlySame;
	}

	@Override
	public String toString() {
		return "TraceNodePair [originalNode=" + beforeNode + ", mutatedNode="
				+ afterdNode + ", isExactlySame=" + isExactlySame + "]";
	}

	
//	public List<String> findWrongVarIDs() {
//		List<String> wrongVarIDs = new ArrayList<>();
//		
//		for(VarValue mutatedReadVar: mutatedNode.getReadVariables()){
//			VarValue originalReadVar = findCorrespondingVarWithDifferentValue(mutatedReadVar, 
//					originalNode.getReadVariables(), mutatedNode.getReadVariables());
//			if(originalReadVar != null){
//				wrongVarIDs.add(mutatedReadVar.getVarID());				
//			}
//		}
//		
//		for(VarValue mutatedWrittenVar: mutatedNode.getWrittenVariables()){
//			VarValue originalWrittenVar = findCorrespondingVarWithDifferentValue(mutatedWrittenVar, 
//					originalNode.getWrittenVariables(), mutatedNode.getWrittenVariables());
//			if(originalWrittenVar != null){
//				wrongVarIDs.add(mutatedWrittenVar.getVarID());				
//			}
//		}
//		
//		System.currentTimeMillis();
//		
//		return wrongVarIDs;
//	}
	
	public List<VarValue> findSingleWrongWrittenVarID(Trace trace){
		List<VarValue> wrongVars = new ArrayList<>();
		
		for(VarValue mutatedWrittenVar: afterdNode.getWrittenVariables()){
			List<VarValue> mutatedVarList = findCorrespondingVarWithDifferentValue(mutatedWrittenVar, 
					beforeNode.getWrittenVariables(), afterdNode.getWrittenVariables(), trace, Variable.WRITTEN);
			if(!mutatedVarList.isEmpty()){
				for(VarValue value: mutatedVarList){
					wrongVars.add(value);
				}
				
//				return wrongVars;
			}
		}
		
		return wrongVars;
	}
	
	/**
	 * 
	 * @return
	 */
	public List<VarValue> findSingleWrongReadVar(Trace mutatedTrace){
		
		List<VarValue> wrongVars = new ArrayList<>();
		
		for(VarValue mutatedReadVar: afterdNode.getReadVariables()){
			List<VarValue> mutatedVarList = findCorrespondingVarWithDifferentValue(mutatedReadVar, 
					beforeNode.getReadVariables(), afterdNode.getReadVariables(), mutatedTrace, Variable.READ);
			if(!mutatedVarList.isEmpty()){
				for(VarValue value: mutatedVarList){
					wrongVars.add(value);
				}
//				return wrongVars;
			}
		}
		
		return wrongVars;
	}

	/**
	 * Normally, it should return a list of a single variable value. 
	 * 
	 * However, it is possible that it returns a list
	 * of two variable values. Such a case happen when the mutated variable and original variable are synonymous reference 
	 * variable, e.g., obj. They are different because some of their attributes or elements are different, e.g., obj.x. In 
	 * this case, the simulated user does not know whether the reference variable itself (e.g., obj) is wrong, or its 
	 * attribute or element (e.g., obj.x) is wrong. Therefore, we return both case for the simulated debugging. In above case,
	 * the first element in the list is the reference variable value, and the second one is the wrong attribute variable value.
	 * 
	 * @param mutatedVar
	 * @param originalList
	 * @param mutatedList
	 * @return
	 */
	private List<VarValue> findCorrespondingVarWithDifferentValue(VarValue mutatedVar, List<VarValue> originalList, 
			List<VarValue> mutatedList, Trace mutatedTrace, String RW) {
		List<VarValue> differentVarValueList = new ArrayList<>();
		
		if(mutatedVar instanceof VirtualValue){
			for(VarValue originalVar: originalList){
				if(originalVar instanceof VirtualValue && mutatedVar.getType().equals(originalVar.getType())){
					/**
					 * currently, the mutation will not change the order of virtual variable.
					 */
					int mutatedIndex = mutatedList.indexOf(mutatedVar);
					int originalIndex = originalList.indexOf(originalVar);
					
					if(mutatedIndex==originalIndex){
						if(((VirtualValue) mutatedVar).isOfPrimitiveType() || mutatedVar.isDefinedToStringMethod()){
							if(!mutatedVar.getStringValue().equals(originalVar.getStringValue())){
								differentVarValueList.add(mutatedVar);
							}
							
							String mutatedVarContent = mutatedVar.getStringValue();
							String originalVarContent = originalVar.getStringValue();
							
							if((mutatedVarContent!=null && originalVarContent==null) ||
									(mutatedVarContent==null && originalVarContent!=null)){
								differentVarValueList.add(mutatedVar);
							}
							else if(mutatedVarContent!=null && originalVarContent!=null){
								if(!mutatedVarContent.equals(originalVarContent)){
									differentVarValueList.add(mutatedVar);
								}
							}
						}
					}
				}
			}
		}
		else if(mutatedVar instanceof PrimitiveValue){
			for(VarValue originalVar:originalList){
				if(originalVar instanceof PrimitiveValue){
					if(mutatedVar.getVarName().equals(originalVar.getVarName())){
						
						String mutatedVarContent = mutatedVar.getStringValue();
						String originalVarContent = originalVar.getStringValue();
						
						if((mutatedVarContent!=null && originalVarContent==null) ||
								(mutatedVarContent==null && originalVarContent!=null)){
							differentVarValueList.add(mutatedVar);
						}
						else if(mutatedVarContent!=null && originalVarContent!=null){
							if(!mutatedVarContent.equals(originalVarContent)){
								differentVarValueList.add(mutatedVar);
							}
						}
					}
				}
			}
		}
		else if(mutatedVar instanceof ReferenceValue){
			for(VarValue originalVar: originalList){
				if(originalVar instanceof ReferenceValue){
					if(mutatedVar.getVarName().equals(originalVar.getVarName())){
						ReferenceValue mutatedRefVar = (ReferenceValue)mutatedVar;
						setChildren(mutatedRefVar, afterdNode, RW);
						ReferenceValue originalRefVar = (ReferenceValue)originalVar;
						setChildren(originalRefVar, beforeNode, RW);
						
						if(mutatedRefVar.getChildren() != null && originalRefVar.getChildren() != null){
							HierarchyGraphDiffer differ = new HierarchyGraphDiffer();
							SortedGraphMatcher sortedMatcher = new SortedGraphMatcher(new Comparator<GraphNode>() {
								@Override
								public int compare(GraphNode o1, GraphNode o2) {
									if(o1 instanceof VarValue && o2 instanceof VarValue){
										return ((VarValue)o1).getVarName().compareTo(((VarValue)o2).getVarName());									
									}
									return 0;
								}
							});
							differ.diff(mutatedRefVar, originalRefVar, true, sortedMatcher, /*EvaluationSettings.variableComparisonDepth*/-1);
							
							if(!differ.getDiffs().isEmpty()){
								differentVarValueList.add(mutatedVar);
							}
							
							System.currentTimeMillis();
							
							for(GraphDiff diff: differ.getDiffs()){
								if(diff.getDiffType().equals(GraphDiff.UPDATE)){
									VarValue mutatedSubVarValue = (VarValue) diff.getNodeBefore();
									
									if(!mutatedSubVarValue.equals(mutatedVar)){
										String varID = mutatedSubVarValue.getVarID();
										if(!varID.contains(":") && !varID.contains(VirtualVar.VIRTUAL_PREFIX)){
											
											TraceNode producer = mutatedTrace.findProducer(mutatedSubVarValue, afterdNode);
											
											String order = (producer == null) ? "0": String.valueOf(producer.getOrder());
											
//											String order = mutatedTrace.findDefiningNodeOrder(Variable.READ, afterdNode, 
//													varID, mutatedSubVarValue.getAliasVarID());
											varID = varID + ":" + order;
										}
										mutatedSubVarValue.setVarID(varID);
										
										if(!differentVarValueList.contains(mutatedSubVarValue)){
											differentVarValueList.add(mutatedSubVarValue);										
										}
										
										/** one wrong attribute is enough for debugging. */
										break;
										
									}
								}
							}
							
//							return differentVarValueList;	
						}
						else if((mutatedRefVar.getChildren() == null && originalRefVar.getChildren() != null) ||
								(mutatedRefVar.getChildren() != null && originalRefVar.getChildren() == null)){
							differentVarValueList.add(mutatedVar);
//							return differentVarValueList;
						}
					}
				}
			}
		}
		
		return differentVarValueList;
	}
	
	private void setChildren(ReferenceValue refVar, TraceNode node, String RW){
		if(refVar.getChildren()==null){
			if(node.getProgramState() != null){
				
				String varID = refVar.getVarID();
//				varID = varID.substring(0, varID.indexOf(":"));
				varID = Variable.truncateSimpleID(varID);
				
				BreakPointValue programState = node.getProgramState();
				if(RW.equals(Variable.WRITTEN)){
					programState = node.getAfterState();
				}
				else if(RW.equals(Variable.READ)){
					programState = node.getProgramState();
				}
				
				VarValue vv = programState.findVarValue(varID);
				if(vv != null){
					List<VarValue> retrievedChildren = vv.getAllDescedentChildren();
					MicroBatUtil.assignWrittenIdentifier(retrievedChildren, node);
					
					refVar.setChildren(vv.getChildren());
				}				
			}
		}
	}
}
