package tregression.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

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
import microbat.model.variable.ArrayElementVar;
import microbat.model.variable.Variable;
import microbat.model.variable.VirtualVar;
import microbat.util.MicroBatUtil;
import tregression.StepChangeTypeChecker;

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

	public boolean isExactSame() {
		return this.isExactlySame;
	}

	@Override
	public String toString() {
		return "TraceNodePair [originalNode=" + beforeNode + ", mutatedNode=" + afterdNode + ", isExactlySame="
				+ isExactlySame + "]";
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
	/**
	 * We should shift this methods into PairList, since both TraceNodePair and PairList has a circular dependency due to the pairList argument.
	 */
	public List<VarValue> findSingleWrongWrittenVarID(Trace trace, PairList pairlist) {
		List<VarValue> wrongVars = new ArrayList<>();

		for (VarValue mutatedWrittenVar : beforeNode.getWrittenVariables()) {
			List<VarValue> mutatedVarList = findCorrespondingVarWithDifferentValue(mutatedWrittenVar,
					afterdNode.getWrittenVariables(), beforeNode.getWrittenVariables(), trace, Variable.WRITTEN, pairlist);
			if (!mutatedVarList.isEmpty()) {
				for (VarValue value : mutatedVarList) {
					wrongVars.add(value);
				}
			}
		}

		return wrongVars;
	}

	/**
	 * We should shift this methods into PairList, since both TraceNodePair and PairList has a circular dependency.
	 */
	public List<VarValue> findSingleWrongReadVar(Trace mutatedTrace, PairList pairlist) {

		List<VarValue> wrongVars = new ArrayList<>();

		for (VarValue mutatedReadVar : beforeNode.getReadVariables()) {
			List<VarValue> mutatedVarList = findCorrespondingVarWithDifferentValue(mutatedReadVar,
					afterdNode.getReadVariables(), beforeNode.getReadVariables(), mutatedTrace, Variable.READ, pairlist);
			if (!mutatedVarList.isEmpty()) {
				for (VarValue value : mutatedVarList) {
					wrongVars.add(value);
				}
			}
		}

		return wrongVars;
	}

	/**
	 * Normally, it should return a list of a single variable value.
	 * 
	 * However, it is possible that it returns a list of two variable values. Such a
	 * case happen when the mutated variable and original variable are synonymous
	 * reference variable, e.g., obj. They are different because some of their
	 * attributes or elements are different, e.g., obj.x. In this case, the
	 * simulated user does not know whether the reference variable itself (e.g.,
	 * obj) is wrong, or its attribute or element (e.g., obj.x) is wrong. Therefore,
	 * we return both case for the simulated debugging. In above case, the first
	 * element in the list is the reference variable value, and the second one is
	 * the wrong attribute variable value.
	 * 
	 * @param mutatedVarVal
	 * @param originalList
	 * @param mutatedList
	 * @return
	 */
	private List<VarValue> findCorrespondingVarWithDifferentValue(VarValue mutatedVarVal, List<VarValue> originalList,
			List<VarValue> mutatedList, Trace mutatedTrace, String RW, PairList pairList) {
		List<VarValue> differentVarValueList = new ArrayList<>();

		if (mutatedVarVal instanceof VirtualValue) {
			for (VarValue originalVar : originalList) {
				if (originalVar instanceof VirtualValue && mutatedVarVal.getType().equals(originalVar.getType())) {
					/**
					 * currently, the mutation will not change the order of virtual variable.
					 */
					int mutatedIndex = mutatedList.indexOf(mutatedVarVal);
					int originalIndex = originalList.indexOf(originalVar);

					if (mutatedIndex == originalIndex) {
						if (((VirtualValue) mutatedVarVal).isOfPrimitiveType()
								|| mutatedVarVal.isDefinedToStringMethod()) {
							if (!mutatedVarVal.getStringValue().equals(originalVar.getStringValue())) {
								differentVarValueList.add(mutatedVarVal);
							}

							String mutatedVarContent = mutatedVarVal.getStringValue();
							String originalVarContent = originalVar.getStringValue();

							if ((mutatedVarContent != null && originalVarContent == null)
									|| (mutatedVarContent == null && originalVarContent != null)) {
								differentVarValueList.add(mutatedVarVal);
							} else if (mutatedVarContent != null && originalVarContent != null) {
								if (!mutatedVarContent.equals(originalVarContent)) {
									differentVarValueList.add(mutatedVarVal);
								}
							}
						}
					}
				}
			}
		} else if (mutatedVarVal instanceof PrimitiveValue) {
			for (VarValue originalVar : originalList) {
				if (!(originalVar instanceof PrimitiveValue)) {
					continue;
				}
				handlePrimitiveValuesCase(mutatedVarVal, mutatedTrace, RW, originalVar,
						differentVarValueList,  pairList);
			}
		} else if (mutatedVarVal instanceof ReferenceValue) {
			for (VarValue originalVar : originalList) {
				if (originalVar instanceof ReferenceValue) {
					if (mutatedVarVal.getVarName().equals(originalVar.getVarName())) {
						ReferenceValue mutatedRefVar = (ReferenceValue) mutatedVarVal;
						setChildren(mutatedRefVar, beforeNode, RW);
						ReferenceValue originalRefVar = (ReferenceValue) originalVar;
						setChildren(originalRefVar, afterdNode, RW);

						if (mutatedRefVar.getChildren() != null && originalRefVar.getChildren() != null) {
							HierarchyGraphDiffer differ = new HierarchyGraphDiffer();
							SortedGraphMatcher sortedMatcher = new SortedGraphMatcher(new Comparator<GraphNode>() {
								@Override
								public int compare(GraphNode o1, GraphNode o2) {
									if (o1 instanceof VarValue && o2 instanceof VarValue) {
										return ((VarValue) o1).getVarName().compareTo(((VarValue) o2).getVarName());
									}
									return 0;
								}
							});
							differ.diff(mutatedRefVar, originalRefVar, true, sortedMatcher,
									/* EvaluationSettings.variableComparisonDepth */-1);

							if (!differ.getDiffs().isEmpty()) {
								differentVarValueList.add(mutatedVarVal);
							}

							System.currentTimeMillis();

							for (GraphDiff diff : differ.getDiffs()) {
								if (diff.getDiffType().equals(GraphDiff.UPDATE)) {
									VarValue mutatedSubVarValue = (VarValue) diff.getNodeBefore();

									if (!mutatedSubVarValue.equals(mutatedVarVal)) {
										String varID = mutatedSubVarValue.getVarID();
										if (!varID.contains(":") && !varID.contains(VirtualVar.VIRTUAL_PREFIX)) {

											TraceNode producer = mutatedTrace.findProducer(mutatedSubVarValue,
													beforeNode);

											String order = (producer == null) ? "0"
													: String.valueOf(producer.getOrder());

//											String order = mutatedTrace.findDefiningNodeOrder(Variable.READ, afterdNode, 
//													varID, mutatedSubVarValue.getAliasVarID());
											varID = varID + ":" + order;
										}
										mutatedSubVarValue.setVarID(varID);

										if (!differentVarValueList.contains(mutatedSubVarValue)) {
											differentVarValueList.add(mutatedSubVarValue);
										}

										/** one wrong attribute is enough for debugging. */
										break;

									}
								}
							}

//							return differentVarValueList;	
						} else if ((mutatedRefVar.getChildren() == null && originalRefVar.getChildren() != null)
								|| (mutatedRefVar.getChildren() != null && originalRefVar.getChildren() == null)) {
							differentVarValueList.add(mutatedVarVal);
//							return differentVarValueList;
						}
					}
				}
			}
		}

		return differentVarValueList;
	}

	private void setChildren(ReferenceValue refVar, TraceNode node, String RW) {
		if (refVar.getChildren() == null) {
			if (node.getProgramState() != null) {

				String varID = refVar.getVarID();
//				varID = varID.substring(0, varID.indexOf(":"));
				varID = Variable.truncateSimpleID(varID);

				BreakPointValue programState = node.getProgramState();
				if (RW.equals(Variable.WRITTEN)) {
					programState = node.getAfterState();
				} else if (RW.equals(Variable.READ)) {
					programState = node.getProgramState();
				}

				VarValue vv = programState.findVarValue(varID);
				if (vv != null) {
					List<VarValue> retrievedChildren = vv.getAllDescedentChildren();
					MicroBatUtil.assignWrittenIdentifier(retrievedChildren, node);

					refVar.setChildren(vv.getChildren());
				}
			}
		}
	}

	private void handlePrimitiveValuesCase(VarValue mutatedVarVal, Trace mutatedTrace, String RW, VarValue originalVar,
			List<VarValue> differentVarValueList, PairList pairList) {
		Variable mutatedVariable = mutatedVarVal.getVariable();
		if (!bothHaveSameClass(mutatedVariable, originalVar.getVariable())) {
			return;
		}
		if (mutatedVariable instanceof ArrayElementVar) {
			Optional<VarValue> correspondingVarValOptional = getCorrespondingVarValue(mutatedVarVal,
					Variable.READ.equals(RW), pairList);
			if (correspondingVarValOptional.isPresent() && !mutatedVarVal.getStringValue().equals(correspondingVarValOptional.get().getStringValue())) {
				differentVarValueList.add(mutatedVarVal);
			}
		} else if (mutatedVarVal.getVarName().equals(originalVar.getVarName())) {
			String mutatedVarContent = mutatedVarVal.getStringValue();
			String originalVarContent = originalVar.getStringValue();

			if ((mutatedVarContent != null && originalVarContent == null)
					|| (mutatedVarContent == null && originalVarContent != null)) {
				differentVarValueList.add(mutatedVarVal);
			} else if (mutatedVarContent != null && originalVarContent != null) {
				if (!mutatedVarContent.equals(originalVarContent)) {
					differentVarValueList.add(mutatedVarVal);
				}
			}
		}
	}

	private Optional<VarValue> getCorrespondingVarValue(VarValue thisVar, boolean isRead, PairList pairList) {
		StepChangeTypeChecker stepChangeTypeChecker = new StepChangeTypeChecker(beforeNode.getTrace(),
				afterdNode.getTrace());
		List<VarValue> sameVarValues;
		if (isRead) {
			sameVarValues = stepChangeTypeChecker.findSynonymousVarList(beforeNode, afterdNode, thisVar, true,  pairList);
		} else {
			sameVarValues = stepChangeTypeChecker.findSymWrittenVaribles(beforeNode, afterdNode, thisVar, true,  pairList);
		}
		if (sameVarValues.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(sameVarValues.get(0));
	}

	private boolean bothHaveSameClass(Object obj, Object obj1) {
		return obj.getClass().equals(obj1.getClass());
	}
}
