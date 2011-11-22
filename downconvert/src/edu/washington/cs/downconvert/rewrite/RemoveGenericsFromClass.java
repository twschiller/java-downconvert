package edu.washington.cs.downconvert.rewrite;

import static edu.washington.cs.downconvert.rewrite.ASTUtil.copy;
import static edu.washington.cs.downconvert.rewrite.ASTUtil.replace;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

/**
 * Remove generics from a class with type parameters. All type variables
 * are converted to Object, unless they are bounded by a <i>single</i> type, in which
 * case they converted to that type.
 * @author Todd Schiller
 */
public class RemoveGenericsFromClass extends ASTVisitor{

	CompilationUnit unit;
	AST ast;

	public static void convert(CompilationUnit unit){
		RemoveGenericsFromClass c = new RemoveGenericsFromClass();
		c.unit = unit;
		c.ast = unit.getAST();
		c.unit.accept(c);
	}
	
	@Override
	public boolean visit(TypeDeclaration node){
		//remove type parameters for the class
		node.typeParameters().clear();
		
		return true;
	}
	
	@Override
	public boolean visit(ParameterizedType node){
		replace(node, copy(node.getType()));
		return true;
	}
	

	
	@Override
	public boolean visit(FieldDeclaration node){
		node.setType(resolveTypeVariable(node.getType()));
		return true;
	}
	
	@Override
	public boolean visit(SingleVariableDeclaration node){
		node.setType(resolveTypeVariable(node.getType()));
		return true;
	}
	
	@Override
	public boolean visit(VariableDeclarationStatement node){
		node.setType(resolveTypeVariable(node.getType()));
		return true;
	}

	@Override
	public boolean visit(MethodDeclaration node){
		if (node.getReturnType2() != null){
			node.setReturnType2(resolveTypeVariable(node.getReturnType2()));
		}
		
		return true;
	}
	
	@Override
	public boolean visit(CastExpression node){
		node.setType(resolveTypeVariable(node.getType()));
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
	
	private Type resolveTypeVariable(Type type){
		ITypeBinding binding = type.resolveBinding();
		if (binding.isTypeVariable()){
			if (binding.getTypeBounds().length == 1){
				return ast.newSimpleType(ast.newName(binding.getTypeBounds()[0].getQualifiedName()));
			}else{
				return ast.newSimpleType(ast.newSimpleName("Object"));
			}
		}else{
			return copy(type);
		}
	}
}
