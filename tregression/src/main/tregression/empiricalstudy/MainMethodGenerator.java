package tregression.empiricalstudy;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import microbat.util.JavaUtil;
import microbat.util.Settings;
import tregression.empiricalstudy.TrialGenerator.TestCase;

public class MainMethodGenerator {
	public void generateMainMethod(String fullJavaPath, TestCase tc) {
		JavaUtil.sourceFile2CUMap.clear();
		CompilationUnit cu = JavaUtil.findCompiltionUnitBySourcePath(fullJavaPath, tc.testClass);

		TypeDeclaration typeDel = (TypeDeclaration) cu.types().get(0);
		boolean hasMainMethod = checkMethod(typeDel, "main");
		
		if(hasMainMethod) {
			return;
		}
		
		boolean hasSetUpMethod = checkMethod(typeDel, "setUp");
		boolean hasTearDownMethod = checkMethod(typeDel, "tearDown");
		try {
			generateMainMethod(fullJavaPath, typeDel, hasSetUpMethod, hasTearDownMethod, tc.testClass, tc.testMethod);
		} catch (MalformedTreeException e) {
			e.printStackTrace();
		} catch (CoreException e) {
			e.printStackTrace();
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}

	private void generateMainMethod(String fullJavaPath, TypeDeclaration type, boolean hasSetUpMethod,
			boolean hasTearDownMethod, String testType, String testMethod)
			throws CoreException, MalformedTreeException, BadLocationException {

		AST ast = type.getAST();
		ASTRewrite astRewrite = ASTRewrite.create(ast);
		testType = testType.substring(testType.lastIndexOf(".") + 1, testType.length());
		MethodDeclaration md = createMainMethod(testType, testMethod, ast, hasSetUpMethod, hasTearDownMethod);
		if (md == null) {
			return;
		}

		// CompilationUnit cu = (CompilationUnit) type.getRoot();
		String codeContent = retrieveCodeContent(fullJavaPath);
		Document document = new Document(codeContent);

		ListRewrite statementsListRewrite = astRewrite.getListRewrite(type, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
		statementsListRewrite.insertLast(md, null);
		TextEdit edit = astRewrite.rewriteAST(document, null);
		edit.apply(document);

		
		String newCode = document.get();
		try {
			File file = new File(fullJavaPath);
			FileUtils.writeStringToFile(file, newCode); 
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.currentTimeMillis();
	}

	private String retrieveCodeContent(String fullJavaPath) {
		byte[] encoded;
		try {
			encoded = Files.readAllBytes(Paths.get(fullJavaPath));
			return new String(encoded, StandardCharsets.ISO_8859_1);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	private MethodDeclaration createMainMethod(String testType, String testMethod, AST ast, boolean hasSetUpMethod,
			boolean hasTearDownMethod) {
		MethodDeclaration method = ast.newMethodDeclaration();
		// modifier
		Modifier pubMod = ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD);
		Modifier staticMod = ast.newModifier(ModifierKeyword.STATIC_KEYWORD);
		method.modifiers().add(pubMod);
		method.modifiers().add(staticMod);

		// return type
		PrimitiveType returnType = ast.newPrimitiveType(PrimitiveType.VOID);
		method.setReturnType2(returnType);

		// methodName
		SimpleName methodName = ast.newSimpleName("main");
		method.setName(methodName);

		// parameter
		SingleVariableDeclaration svd = ast.newSingleVariableDeclaration();
		SimpleName paramName = ast.newSimpleName("args");
		svd.setName(paramName);
		SimpleName paramTypeName = ast.newSimpleName("String");
		SimpleType pType = ast.newSimpleType(paramTypeName);
		ArrayType paramType = ast.newArrayType(pType);

		svd.setType(paramType);
		method.parameters().add(svd);

		// body
		Block body = ast.newBlock();

		// fragment: test = new XXTest();
		VariableDeclarationFragment fragment = ast.newVariableDeclarationFragment();
		SimpleName varName = ast.newSimpleName("test");
		fragment.setName(varName);
		ClassInstanceCreation creation = ast.newClassInstanceCreation();
		SimpleName cName = ast.newSimpleName(testType);
		SimpleType cType = ast.newSimpleType(cName);
		creation.setType(cType);
		fragment.setInitializer(creation);

		// vStatement: XXTest test = new XXTest();
		VariableDeclarationStatement vStatement = ast.newVariableDeclarationStatement(fragment);
		SimpleName tName = ast.newSimpleName(testType);
		SimpleType sType = ast.newSimpleType(tName);
		vStatement.setType(sType);

		body.statements().add(vStatement);

		// try{}
		TryStatement tryStatement = ast.newTryStatement();
		Block tryBody = ast.newBlock();
		tryStatement.setBody(tryBody);

		if (hasSetUpMethod) {
			addSetUpMethodInvocation(ast, tryBody);
		}

		addTestMethodInvocation(ast, tryBody, testMethod);

		if (hasTearDownMethod) {
			addTearDownMethodInvocation(ast, tryBody);
		}

		CatchClause clause = ast.newCatchClause();
		SingleVariableDeclaration exceptionSVD = ast.newSingleVariableDeclaration();
		SimpleName exVarName = ast.newSimpleName("e");
		SimpleName exTypeName = ast.newSimpleName("Exception");
		SimpleType exType = ast.newSimpleType(exTypeName);
		exceptionSVD.setType(exType);
		exceptionSVD.setName(exVarName);
		clause.setException(exceptionSVD);
		tryStatement.catchClauses().add(clause);

		body.statements().add(tryStatement);

		method.setBody(body);
		return method;
	}

	@SuppressWarnings("unchecked")
	private void addTearDownMethodInvocation(AST ast, Block tryBody) {
		MethodInvocation invocation = ast.newMethodInvocation();
		SimpleName sName = ast.newSimpleName("test");
		invocation.setExpression(sName);
		SimpleName mName = ast.newSimpleName("tearDown");
		invocation.setName(mName);
		ExpressionStatement statement = ast.newExpressionStatement(invocation);
		tryBody.statements().add(statement);
	}

	@SuppressWarnings("unchecked")
	private void addTestMethodInvocation(AST ast, Block tryBody, String testMethod) {
		MethodInvocation invocation = ast.newMethodInvocation();
		SimpleName sName = ast.newSimpleName("test");
		invocation.setExpression(sName);
		SimpleName mName = ast.newSimpleName(testMethod);
		invocation.setName(mName);
		ExpressionStatement statement = ast.newExpressionStatement(invocation);
		tryBody.statements().add(statement);
	}

	@SuppressWarnings("unchecked")
	private void addSetUpMethodInvocation(AST ast, Block tryBody) {
		MethodInvocation invocation = ast.newMethodInvocation();
		SimpleName sName = ast.newSimpleName("test");
		invocation.setExpression(sName);
		SimpleName mName = ast.newSimpleName("setUp");
		invocation.setName(mName);
		ExpressionStatement statement = ast.newExpressionStatement(invocation);
		tryBody.statements().add(statement);
	}

	private boolean checkMethod(TypeDeclaration type, String method) {
		for (MethodDeclaration md : type.getMethods()) {
			String methodName = md.getName().getIdentifier();
			if (methodName.equals(method)) {
				return true;
			}
		}

		return false;
	}

}
