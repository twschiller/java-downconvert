package edu.washington.cs.downconvert.rewrite;

import static edu.washington.cs.downconvert.rewrite.ASTUtil.copy;
import static edu.washington.cs.downconvert.rewrite.ASTUtil.replace;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

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
			
			if ((node.getParent() instanceof Expression || node.getParent() instanceof VariableDeclarationFragment || node.getParent() instanceof ReturnStatement)
				&& !(node.getParent() instanceof CastExpression)){
				
				ITypeBinding rt = method.getReturnType();
				
				ParenthesizedExpression paren = ast.newParenthesizedExpression();
				
				// TODO make this more robust (it probably doesn't work for arrays, etc).
				CastExpression cast = ast.newCastExpression();	
				
				if (rt.isCapture()){
					if (rt.getWildcard() != null && rt.getBound() != null){
						throw new RuntimeException("Lowerbounded wildcard bindings not supported for capture");
					}
					
					if (rt.getTypeBounds().length == 1){
						cast.setType(ASTUtil.convert(ast, rt.getTypeBounds()[0]));
					}else if (rt.getTypeBounds().length > 0){
						throw new RuntimeException("Multiple type bounds not supported for capture binding");
					}else{
						cast.setType(ast.newSimpleType(ast.newName("Object")));
					}
					
				}else if (rt.isWildcardType()){
					if (rt.getBound() == null){
						cast.setType(ast.newSimpleType(ast.newName("Object")));
					}else{
						if (rt.isUpperbound()){
							cast.setType(ASTUtil.convert(ast, rt.getBound()));
						}else{
							throw new RuntimeException("Lower bounds not supported when inserting casts");
						}
					}
				}else{
					try{
						cast.setType(ASTUtil.convert(ast, rt));
					}catch(Exception e){
						throw new RuntimeException(e);
					}
				}
				
				cast.setExpression(copy(node));
				
				paren.setExpression(cast);
				
				replace(node, paren);
				
				node.delete();
			}
		}
		return true;
	}

}
