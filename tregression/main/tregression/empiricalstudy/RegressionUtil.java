package tregression.empiricalstudy;

import java.util.ArrayList;
import java.util.List;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.LocalVariable;
import org.apache.bcel.classfile.LocalVariableTable;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ArrayInstruction;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.INVOKEINTERFACE;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.LocalVariableInstruction;
import org.apache.bcel.generic.Type;

import microbat.codeanalysis.bytecode.ByteCodeParser;
import microbat.codeanalysis.bytecode.MethodFinderByLine;
import microbat.codeanalysis.bytecode.MethodFinderBySignature;
import microbat.codeanalysis.runtime.PreCheckInformation;
import microbat.model.BreakPoint;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.util.PrimitiveUtils;
import sav.common.core.utils.SignatureUtils;
import sav.strategies.dto.AppJavaClassPath;

public class RegressionUtil {
	public static List<String> identifyIncludedClassNames(List<TraceNode> stopSteps,
			PreCheckInformation precheckInfo, List<TraceNode> visitedSteps) {
		Repository.clearCache();
		
		List<BreakPoint> parsedBreakPoints = new ArrayList<>();
		List<String> classes = new ArrayList<>();
		
		for(TraceNode stopStep: stopSteps){
			AppJavaClassPath appClassPath = stopStep.getTrace().getAppJavaClassPath();
			
			List<TraceNode> range = identifyEnhanceRange(stopStep, visitedSteps);
			range.add(stopStep);
			
			for(TraceNode rangeStep: range) {
				BreakPoint point = rangeStep.getBreakPoint();
				if(parsedBreakPoints.contains(point)){
					continue;
				}
				parsedBreakPoints.add(point);
				
				String clazz = point.getClassCanonicalName();
				
				MethodFinderByLine finder = new MethodFinderByLine(point);
				ByteCodeParser.parse(clazz, finder, appClassPath);
				Method method = finder.getMethod();
				List<InstructionHandle> insList = finder.getHandles();
				
				List<String> visitedLibClasses = findInvokedLibClasses(rangeStep, insList, finder.getJavaClass(), method, precheckInfo);
				for(String str: visitedLibClasses){
					if(!classes.contains(str)){
						classes.add(str);
					}
				}
			}
		}
		
		return classes;
	}

	private static TraceNode findClosestStep(TraceNode stopStep, List<TraceNode> visitedSteps) {
		TraceNode closestStep = null;
		int distance = -1;
		for(TraceNode step: visitedSteps) {
			if(step.getOrder()>stopStep.getOrder()) {
				if(closestStep==null) {
					closestStep = step;
					distance = step.getOrder() - stopStep.getOrder();
				}
				else {
					int newDis = step.getOrder() - stopStep.getOrder();
					if(newDis < distance) {
						closestStep = step;
						distance = newDis;
					}
				}
			}
		}
		
		return closestStep;
	}
	
	private static List<TraceNode> identifyEnhanceRange(TraceNode stopStep, List<TraceNode> visitedSteps){
		TraceNode closetStep = findClosestStep(stopStep, visitedSteps);
		List<TraceNode> list = new ArrayList<>();
		
		if(closetStep==null){
			return list;
		}
		
		Trace trace = stopStep.getTrace();
		for(int i=closetStep.getOrder(); i>stopStep.getOrder(); i--) {
			TraceNode step = trace.getTraceNode(i);
			list.add(step);
		}
		
		return list;
	}
	
	
	/**
	 * step is the trace step being analyzed
	 * insList is the list of instructions corresponding to the step
	 * method is the method where the step resides in
	 * precheckInfo contains the information of all the loaded classes in the runtime.
	 * 
	 * @param step
	 * @param insList
	 * @param method
	 * @param precheckInfo
	 * @return
	 */
	private static List<String> findInvokedLibClasses(TraceNode step, List<InstructionHandle> insList, JavaClass clazz, Method method,
			PreCheckInformation precheckInfo) {
		List<String> collectedIncludedClasses = new ArrayList<>();
		if(step.getInvocationChildren().isEmpty()){
			TraceNode stepOver = step.getStepOverPrevious();
			
			String ignoreMethod = null;
			if(stepOver!=null && stepOver.getBreakPoint().equals(step.getBreakPoint())){
				TraceNode invocationChild = stepOver.getInvocationChildren().get(0);
				ignoreMethod = invocationChild.getMethodSign();
			}
			
			AppJavaClassPath appPath = step.getTrace().getAppJavaClassPath();
			analyzeIncludedClasses(collectedIncludedClasses, clazz, method, appPath, precheckInfo, 3, insList, ignoreMethod);
			
		}
		
		return collectedIncludedClasses;
	}
	
	/**
	 * 
	 * ignoreMethod is used to identify library classes, which can be set as null.
	 * 
	 * @param collectedIncludedClasses
	 * @param method
	 * @param insList
	 * @param appPath
	 * @param ignoreMethod
	 * @param precheckInfo
	 * @return
	 */
	private static void analyzeIncludedClasses(List<String> collectedIncludedClasses, JavaClass clazz, Method method,  
			AppJavaClassPath appPath, PreCheckInformation precheckInfo, int cascadeLimit,
			List<InstructionHandle> insList, String ignoreMethod){
		
		assert(cascadeLimit>=1);
		cascadeLimit--;
		if(cascadeLimit==0){
			return;
		}
		
		ConstantPoolGen cGen = new ConstantPoolGen(method.getConstantPool());
		
		if(insList==null){
			InstructionList iList = new InstructionList(method.getCode().getCode());
			insList = new ArrayList<>();
			for(InstructionHandle handle: iList){
				insList.add(handle);
			}
		}
		
		for(InstructionHandle handle: insList){
			Instruction ins = handle.getInstruction();
			
			if(isForReadWriteVariable(ins)){
				String className = parseClassName(ins, method, cGen);				
				if(className != null && !PrimitiveUtils.isPrimitiveType(className)){
					className = className.replace("[]", "");
					if(className.equals("java.lang.Object") || className.equals("java.lang.String")){
						continue;
					}
					
					if(SignatureUtils.isSignature(className)){
						className = SignatureUtils.signatureToName(className);
						className = className.replace("[]", "");
					}
					
					if(PrimitiveUtils.isPrimitiveType(className)){
						continue;
					}
					
					appendSuperClass(className, appPath, collectedIncludedClasses);
					
					if(!collectedIncludedClasses.contains(className)){
						collectedIncludedClasses.add(className);
					}	
					
				}
			}
			else if(ins instanceof InvokeInstruction){
				InvokeInstruction iIns = (InvokeInstruction)ins;
				
				String invokedMethodName = iIns.getMethodName(cGen);
				
				if(ignoreMethod!=null && ignoreMethod.contains(invokedMethodName)){
					continue;
				}
				
				String className = iIns.getClassName(cGen);
				if(className.equals("java.lang.Object") || className.equals("java.lang.String")){
					continue;
				}
				
				if(invokedMethodName.equals(method.getName()) 
						&& iIns.getSignature(cGen).equals(method.getSignature()) 
						&& className.equals(clazz.getClassName())){
					continue;
				}
				
				if(SignatureUtils.isSignature(className)){
					className = SignatureUtils.signatureToName(className);
					className = className.replace("[]", "");
				}
				
				if(PrimitiveUtils.isPrimitiveType(className)){
					continue;
				}
				
				appendSuperClass(className, appPath, collectedIncludedClasses);
				
				if(!collectedIncludedClasses.contains(className)){
					collectedIncludedClasses.add(className);
				}	
				
				//add implementation class
				if(ins instanceof INVOKEINTERFACE) {
					List<String> loadedClassStrings = precheckInfo.getLoadedClasses();
					List<String> implementations = findImplementation(className, 
							loadedClassStrings, appPath);
					
					for(String implementation: implementations) {
						collectedIncludedClasses.add(implementation);
						appendSuperClass(className, appPath, collectedIncludedClasses);
					}
				}
				
				
				MethodFinderBySignature finder = findInvokedMethod(className, invokedMethodName, iIns.getSignature(cGen), appPath);
				Method invokedMethod = finder.getMethod();
				JavaClass invokedClass = finder.getJavaClass();
				if(invokedMethod!=null && !(ins instanceof INVOKEINTERFACE) && !invokedMethod.isAbstract()
						&& !invokedMethod.isNative()){
					analyzeIncludedClasses(collectedIncludedClasses, invokedClass, invokedMethod, appPath, 
							precheckInfo, cascadeLimit, null, null);					
				}	
			}
		}
	}
	
	private static MethodFinderBySignature findInvokedMethod(String className, String invokedMethodName, String signature,
			AppJavaClassPath appPath) {
		MethodFinderBySignature finder = new MethodFinderBySignature(invokedMethodName + signature);
		ByteCodeParser.parse(className, finder, appPath);
		
		return finder;
	}

	@SuppressWarnings("deprecation")
	private static String parseClassName(Instruction ins, Method method, ConstantPoolGen cGen) {
		if(ins instanceof LocalVariableInstruction){
			LocalVariableTable table = method.getLocalVariableTable();
			if(table != null){
				LocalVariableInstruction lIns = (LocalVariableInstruction) ins;
				LocalVariable localVar = table.getLocalVariable(lIns.getIndex());
				if(localVar!=null){
					String classSig = localVar.getSignature();
					if(classSig.length()!=1){
						String className = SignatureUtils.signatureToName(classSig);
						return className;				
					}
				}
			}
		}
		else if(ins instanceof FieldInstruction){
			FieldInstruction fIns = (FieldInstruction)ins;
			Type type = fIns.getFieldType(cGen);
			String classSig = type.getSignature();
			if(classSig.length()!=1){
				String className = SignatureUtils.signatureToName(classSig);
				return className;				
			}
		}
		else if(ins instanceof ArrayInstruction){
			ArrayInstruction aIns = (ArrayInstruction)ins;
			Type type = aIns.getType(cGen);
			String classSig = type.getSignature();
			if(classSig.length()!=1){
				String className = SignatureUtils.signatureToName(classSig);
				return className;				
			}
		}
		
		return null;
	}

	private static boolean isForReadWriteVariable(Instruction ins) {
		return ins instanceof FieldInstruction || 
				ins instanceof ArrayInstruction ||
				ins instanceof LocalVariableInstruction;
	}

	private static void appendSuperClass(String className, AppJavaClassPath appPath, List<String> includedClasses){
		JavaClass javaClazz = ByteCodeParser.parse(className, appPath);
		if(javaClazz==null){
			return;
		}
		
		try {
			for(JavaClass superClass: javaClazz.getSuperClasses()){
				if(!superClass.getClassName().equals("java.lang.Object")){
					if(!includedClasses.contains(superClass.getClassName())){
						includedClasses.add(superClass.getClassName());
					}	
				}
			}
		} catch (ClassNotFoundException e) {
		}
	}

	private static List<String> findImplementation(String className, List<String> loadedClassStrings,
			AppJavaClassPath appClassPath) {
		List<String> list = new ArrayList<>();
		for(String loadedClassString: loadedClassStrings) {
			if(loadedClassString.contains("microbat") || loadedClassString.contains("sav.common")
					|| loadedClassString.contains("sun.reflect")
					|| loadedClassString.contains("com.sun")
					|| loadedClassString.contains("sun.misc")
					|| loadedClassString.contains("java.lang.ClassLoader")
					|| loadedClassString.contains("java.lang.ref")
					|| loadedClassString.contains("java.security")
					|| loadedClassString.contains("sun.")
					|| loadedClassString.contains("java.io")
					|| loadedClassString.contains("java.nio")
					|| loadedClassString.contains("java.util.zip")
					|| loadedClassString.contains("java.util.concurrent")
					|| loadedClassString.contains("junit")
					|| loadedClassString.contains("java.lang.ThreadLocal")
					|| loadedClassString.contains("java.lang.Terminator")) {
				continue;
			}
			
			JavaClass javaClass = ByteCodeParser.parse(loadedClassString, appClassPath);
			if(javaClass!=null) {
				try {
					for(JavaClass interfaze: javaClass.getAllInterfaces()) {
						if(interfaze.getClassName().equals(className)) {
							list.add(loadedClassString);
							break;
						}
					}
				} catch (ClassNotFoundException e) {
//					e.printStackTrace();
				}
				
			}
		}
		return list;
	}
}
