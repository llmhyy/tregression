package tregression.handler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import sav.strategies.dto.AppJavaClassPath;
import tregression.StepChangeType;
import tregression.StepChangeTypeChecker;
import tregression.empiricalstudy.MatchStepFinder;
import tregression.empiricalstudy.TestCase;
import tregression.empiricalstudy.config.ConfigFactory;
import tregression.empiricalstudy.config.ProjectConfig;
import tregression.model.PairList;
import tregression.separatesnapshots.AppClassPathInitializer;
import tregression.separatesnapshots.DiffMatcher;
import tregression.tracematch.ControlPathBasedTraceMatcher;
import tregression.views.BuggyTraceView;
import tregression.views.CorrectTraceView;

//import dataset.BugDataset;
//import dataset.BugDataset.BugData;
//import dataset.bug.minimize.ProjectMinimizer;

import jmutation.utils.TraceHelper;

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
				
		        final String scrFolderPath = "src\\main\\java";
		        final String testFolderPath = "src\\test\\java";
		        final String mutatedProjPath = "C:\\Users\\arkwa\\Documents\\NUS\\Debug_Simulation\\Mutation_BugDataset\\1\\bug";
		        final String originProjPath = "C:\\Users\\arkwa\\Documents\\NUS\\Debug_Simulation\\Mutation_BugDataset\\fix";
		        
				
//				final Trace buggyTrace = buggyView.getTrace();
//				final Trace correctTrace = correctView.getTrace();
				
//		        final Trace buggyTrace = data.getBuggyTrace();
//		        final Trace correctTrace = data.getWorkingTrace();
//		        
//		        dataset.TestCase testCase = data.getTestCase();
//		        final String projName = data.getProjectName();
//		        final String regressionID = testCase.toString();
//		        
//		        ProjectConfig config = ConfigFactory.createConfig(projName, regressionID, mutatedProjPath, originProjPath);
//				tregression.empiricalstudy.TestCase tc = new tregression.empiricalstudy.TestCase(testCase.testClassName(), testCase.testMethodName());
//				AppJavaClassPath buggyApp = AppClassPathInitializer.initialize(mutatedProjPath, tc, config);
//				AppJavaClassPath correctApp = AppClassPathInitializer.initialize(originProjPath, tc, config);
//				
//				buggyTrace.setAppJavaClassPath(buggyApp);
//				correctTrace.setAppJavaClassPath(correctApp);
//				
//		        DiffMatcher matcher = new DiffMatcher(scrFolderPath, testFolderPath, mutatedProjPath, originProjPath);
//				matcher.matchCode();
//				
//				ControlPathBasedTraceMatcher traceMatcher = new ControlPathBasedTraceMatcher();
//				PairList pairList = traceMatcher.matchTraceNodePair(buggyTrace, correctTrace, matcher);
//				
//				updateView(buggyTrace, correctTrace, pairList, matcher);
				
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
	private void updateView(final Trace buggyTrace, final Trace correctTrace, final tregression.model.PairList pairListTregression, final DiffMatcher matcher) {
		if (this.buggyView != null && this.correctView != null) {
			Display.getDefault().syncExec(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					buggyView.setMainTrace(buggyTrace);
					buggyView.updateData();
					buggyView.setPairList(pairListTregression);
					buggyView.setDiffMatcher(matcher);
					
					correctView.setMainTrace(correctTrace);
					correctView.updateData();
					correctView.setPairList(pairListTregression);
					correctView.setDiffMatcher(matcher);
				}
			});
		} else {
			System.out.println("buggyView or correctView is null");
		}
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
