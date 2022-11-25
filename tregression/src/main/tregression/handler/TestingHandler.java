package tregression.handler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import microbat.bytecode.ByteCode;
import microbat.bytecode.ByteCodeList;
import microbat.bytecode.OpcodeType;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.util.JavaUtil;
import sav.common.core.Pair;
import tregression.StepChangeType;
import tregression.StepChangeTypeChecker;
import tregression.empiricalstudy.MatchStepFinder;
import tregression.model.PairList;
import tregression.separatesnapshots.DiffMatcher;
import tregression.views.BuggyTraceView;
import tregression.views.CorrectTraceView;

public class TestingHandler extends AbstractHandler {

	private BuggyTraceView buggyView;
	private CorrectTraceView correctView;
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		JavaUtil.sourceFile2CUMap.clear();
		Job job = new Job("Testing Tregression") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				// Access the buggy view and correct view
				setup();
				
				final Trace buggyTrace = buggyView.getTrace();
				final Trace correctTrace = correctView.getTrace();
				
				final PairList pairList = buggyView.getPairList();
				final DiffMatcher matcher = buggyView.getDiffMatcher();
				
				final StepChangeTypeChecker checker = new StepChangeTypeChecker(buggyTrace, correctTrace);
				
				Set<String> forEachLoopLocations = new HashSet<>();
				
				for (TraceNode node : buggyTrace.getExecutionList()) {
					
					setAllPropability(node.getReadVariables(), -1.0);
					setAllPropability(node.getWrittenVariables(), -1.0);
					
					final String nodeLocation = encodeNodeLocation(node);
					if (forEachLoopLocations.contains(nodeLocation)) {
						continue;
					}
					
					if (node.getWrittenVariables().isEmpty() || node.getReadVariables().isEmpty()) {
						continue;
					}
					
					if (isForEachLoop(node)) {
						System.out.println("TraceNode: " + node.getOrder() + " is for each loop");
						forEachLoopLocations.add(nodeLocation);
						continue;
					}
					
					TraceNode matchedNode = MatchStepFinder.findMatchedStep(true, node, pairList);
					if (matchedNode == null) {
						continue;
					}
					
					List<Pair<VarValue, VarValue>> wrongPairs = checker.getWrongWrittenVariableList(true, node, matchedNode, pairList, matcher);
					List<VarValue> wrongVarList = new ArrayList<>();
					for (Pair<VarValue, VarValue> wrongPair : wrongPairs) {
						wrongVarList.add(wrongPair.first());
					}
					
					StepChangeType changeType = checker.getType(node, true, pairList, matcher);
					if (changeType.getType() == StepChangeType.SRC) {
						setAllPropability(node.getReadVariables(), 1.0);
						for (VarValue writtenVar : node.getWrittenVariables()) {
							writtenVar.setProbability(wrongVarList.contains(writtenVar) ? 0.0 : 1.0);
						}
					} else if (changeType.getType() == StepChangeType.CTL) {
						// Do nothing
					} else if (changeType.getType() == StepChangeType.IDT) {
						setAllPropability(node.getReadVariables(), 1.0);
						setAllPropability(node.getWrittenVariables(), 1.0);
					} else {
						List<Pair<VarValue, VarValue>> wrongReadVarPair = changeType.getWrongVariableList();
						List<VarValue> wrongReadVarList = new ArrayList<>();
						for (Pair<VarValue, VarValue> pair : wrongReadVarPair) {
							wrongReadVarList.add(pair.first());
						}
					
						for (VarValue readVar : node.getReadVariables()) {
							readVar.setProbability(wrongReadVarList.contains(readVar)?0.0:1.0);
						}
						
						for (VarValue writtenVar : node.getWrittenVariables()) {
							writtenVar.setProbability(wrongVarList.contains(writtenVar)?0.0:1.0);
						}
					}
				}
				
				System.out.println("Finish assigning");
				
				return Status.OK_STATUS;
			}
		};
		job.schedule();
		return null;
	}
	
	private void setAllPropability(List<VarValue> vars, double prob) {
		for (VarValue var : vars) {
			var.setProbability(prob);
		}
	}
	private String encodeNodeLocation(TraceNode node) {
		return node.getBreakPoint().getFullJavaFilePath() + "_" + node.getLineNumber();
	}
	
	private boolean isForEachLoop(TraceNode node) {
		String code = node.getCodeStatement();
		code = code.replaceAll("\\s+", "");
		if (!code.startsWith("for(")) {
			return false;
		}
		
		int count = 0;
		for (int i = 0; i < code.length(); i++) {
		    if (code.charAt(i) == ':') {
		        count++;
		    }
		}
		
		return count == 1;
//		ByteCodeList byteCodeList = new ByteCodeList(node.getBytecode());
//		return this.isCollectionForEachLoop(byteCodeList) || this.isArrayListForEachLoop(byteCodeList);
	}
	
	private boolean isCollectionForEachLoop(ByteCodeList byteCodeList) {
		if (byteCodeList.size() != 10) {
			return false;
		}
		int[] opCodeList = {-1,185,-1,167,-1,185,-1,-1,185,154};
		for (int i=0; i<10; i++) {
			ByteCode byteCode = byteCodeList.getByteCode(i);
			int targetOpcode = opCodeList[i];
			if (targetOpcode == -1) {
				continue;
			}
			if (byteCode.getOpcode() != targetOpcode) {
				return false;
			}
		}
		
		ByteCode byteCode_0 = byteCodeList.getByteCode(0);
		if(byteCode_0.getOpcodeType() != OpcodeType.LOAD_VARIABLE) {
			return false;
		}
		
		ByteCode byteCode_2 = byteCodeList.getByteCode(2);
		if (byteCode_2.getOpcodeType() != OpcodeType.STORE_VARIABLE) {
			return false;
		}
		
		ByteCode byteCode_4 = byteCodeList.getByteCode(4);
		if (byteCode_4.getOpcodeType() != OpcodeType.LOAD_VARIABLE) {
			return false;
		}
		
		ByteCode byteCode_6 = byteCodeList.getByteCode(6);
		if (byteCode_6.getOpcodeType() != OpcodeType.STORE_VARIABLE) {
			return false;
		}
		
		ByteCode byteCode_7 = byteCodeList.getByteCode(7);
		if (byteCode_7.getOpcodeType() != OpcodeType.LOAD_VARIABLE) {
			return false;
		}
		
		return true;
	}
	
	private boolean isArrayListForEachLoop(ByteCodeList byteCodeList) {
		if (byteCodeList.size() != 16) {
			return false;
		}
		
		int[] opCodeList = {-1,89,58,190,54,3,-1,167,25,-1,-1,-1,132,-1,21,161};
		for (int i=0; i<16; i++) {
			ByteCode byteCode = byteCodeList.getByteCode(i);
			int targetOpcode = opCodeList[i];
			if (targetOpcode == -1) {
				continue;
			}
			if (byteCode.getOpcode() != targetOpcode) {
				return false;
			}
		}
		
		ByteCode byteCode_1 = byteCodeList.getByteCode(0);
		if(byteCode_1.getOpcodeType() != OpcodeType.LOAD_VARIABLE) {
			return false;
		}
		
		ByteCode byteCode_6 = byteCodeList.getByteCode(6);
		if (byteCode_6.getOpcodeType() != OpcodeType.STORE_VARIABLE) {
			return false;
		}
		
		ByteCode byteCode_9 = byteCodeList.getByteCode(9);
		if (byteCode_9.getOpcodeType() != OpcodeType.LOAD_VARIABLE) {
			return false;
		}
		
		ByteCode byteCode_10 = byteCodeList.getByteCode(10);
		if (byteCode_10.getOpcodeType() != OpcodeType.LOAD_FROM_ARRAY) {
			return false;
		}
		
		ByteCode byteCode_11 = byteCodeList.getByteCode(11);
		if (byteCode_11.getOpcodeType() != OpcodeType.STORE_VARIABLE) {
			return false;
		}
		
		ByteCode byteCode_13 = byteCodeList.getByteCode(13);
		if (byteCode_13.getOpcodeType() != OpcodeType.LOAD_VARIABLE) {
			return false;
		}
		
		return true;
	}
	
	private void setup() {
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				try {
					buggyView = (BuggyTraceView)PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(BuggyTraceView.ID);
					correctView = (CorrectTraceView)PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(CorrectTraceView.ID);
				} catch (PartInitException e) {
					buggyView = null;
					correctView = null;
					System.out.println("Fail to get the view");
				}
			}
		});
	}
	
	private void jumpToNode(final TraceNode targetNode) {
		Display.getDefault().asyncExec(new Runnable() {
		    @Override
		    public void run() {
				Trace buggyTrace = buggyView.getTrace();
				buggyView.jumpToNode(buggyTrace, targetNode.getOrder(), true);
		    }
		});
	}
}
