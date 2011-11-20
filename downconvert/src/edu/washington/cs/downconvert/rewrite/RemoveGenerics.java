package edu.washington.cs.downconvert.rewrite;

import static edu.washington.cs.downconvert.rewrite.ASTUtil.copy;
import static edu.washington.cs.downconvert.rewrite.ASTUtil.replace;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ParameterizedType;

/**
 * Remove generics from a file. This must be run <i>after</i> enhanced
 * for loops have been removed from a program, and casts have been inserted.
 * @author Todd Schiller
 *
 */
public class RemoveGenerics extends ASTVisitor{

	CompilationUnit unit;
	AST ast;

	public static void convert(CompilationUnit unit){
		RemoveGenerics c = new RemoveGenerics();
		c.unit = unit;
		c.ast = unit.getAST();
		c.unit.accept(c);
		
	}
	
	@Override
	public boolean visit(ParameterizedType node){
		replace(node, copy(node.getType()));
		return true;
	}
	
	@Override
	public boolean visit(ClassInstanceCreation node){
		node.typeArguments().clear();
		return true;
	}
	
	@Override
	public boolean visit(MethodInvocation node){
		node.typeArguments().clear();		
		return true;
	}
}
