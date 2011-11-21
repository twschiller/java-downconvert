package edu.washington.cs.downconvert.rewrite;

import static edu.washington.cs.downconvert.rewrite.ASTUtil.copy;
import static edu.washington.cs.downconvert.rewrite.ASTUtil.replace;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;

/**
 * Insert sufficient casts so that Generics can be removed.
 * 
 * This is done by looking for invocations of methods that that
 * are declared with a type variable return type, and adding a cast
 * to the type that Eclipse has resolved.
 * 
 * @author Todd Schiller
 */
public class InsertCastsForGenerics extends ASTVisitor{

	CompilationUnit unit;
	AST ast;

	public static void convert(CompilationUnit unit){
		InsertCastsForGenerics c = new InsertCastsForGenerics();
		c.unit = unit;
		c.ast = unit.getAST();
		c.unit.accept(c);
		
	}
	
	@Override 
	public boolean visit(MethodInvocation node){
		IMethodBinding method = node.resolveMethodBinding();
		IMethodBinding declaration = method.getMethodDeclaration();
		
		if (declaration.getReturnType().isTypeVariable()){
			ITypeBinding rt = method.getReturnType();
			
			// TODO make this more robust (it probably doesn't work for arrays, etc).
			CastExpression cast = ast.newCastExpression();	
			cast.setType(ast.newSimpleType(ast.newName(rt.getQualifiedName())));
			cast.setExpression(copy(node));
			
			replace(node, cast);
			
			node.delete();
		}
		return true;
	}

}
