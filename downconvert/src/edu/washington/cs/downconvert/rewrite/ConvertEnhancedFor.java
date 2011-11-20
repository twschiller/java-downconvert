package edu.washington.cs.downconvert.rewrite;


import static edu.washington.cs.downconvert.rewrite.ASTUtil.copy;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;

/**
 * Convert {@link EnhancedForStatement}s into {@link WhileStatement}s using
 * {@link java.util.Iterator} with the proper type parameter
 * @author Todd Schiller
 */
public class ConvertEnhancedFor extends ASTVisitor{

	private final CompilationUnit unit;
	private final AST ast;

	protected ConvertEnhancedFor(CompilationUnit unit) {
		this.unit = unit;
		this.ast = unit.getAST();
	}

	public static void convert(CompilationUnit unit){
		ConvertEnhancedFor c = new ConvertEnhancedFor(unit);
		c.unit.accept(c);
	}
		
	@Override
	public boolean visit(CompilationUnit xxx){
		ImportDeclaration i = ast.newImportDeclaration();
		i.setName(ast.newName("java.util.Iterator"));
		xxx.imports().add(i);
		return true;
	}
	
	@Override
	public boolean visit(EnhancedForStatement node){
		
		Block block = ast.newBlock();
		
		// (1) DECLARE THE ITERATOR
		MethodInvocation iterator = ast.newMethodInvocation();
		iterator.setExpression(copy(node.getExpression()));
		iterator.setName(ast.newSimpleName("iterator"));
	
		VariableDeclarationFragment it = ast.newVariableDeclarationFragment();
		it.setName(ast.newSimpleName("it"));
		it.setInitializer(iterator);
		
		VariableDeclarationStatement itWithType = ast.newVariableDeclarationStatement(it);
		ParameterizedType paramType = ast.newParameterizedType(ast.newSimpleType(ast.newName("Iterator")));
		paramType.typeArguments().add(copy(node.getParameter().getType()));
		
		itWithType.setType(paramType);
		
		block.statements().add(itWithType);
		
		// (2) DECLARE THE WHILE LOOP
		WhileStatement loop = ast.newWhileStatement();
		
		// (2.1) DECLARE THE CONDITION
		MethodInvocation hasNext = ast.newMethodInvocation();
		hasNext.setExpression(ast.newSimpleName("it"));
		hasNext.setName(ast.newSimpleName("hasNext"));
		loop.setExpression(hasNext);
		
		// (2.2) DECLARE THE NEW LOOP BODY
		Block body = ast.newBlock();
				
		// (2.3) CALL NEXT
		VariableDeclarationFragment value = ast.newVariableDeclarationFragment();
		value.setName(copy(node.getParameter().getName()));
		
		MethodInvocation next = ast.newMethodInvocation();
		next.setExpression(ast.newSimpleName("it"));
		next.setName(ast.newSimpleName("next"));
		
		CastExpression cast = ast.newCastExpression();
		cast.setType(copy(node.getParameter().getType()));
		cast.setExpression(next);
		
		value.setInitializer(cast);
		
		VariableDeclarationStatement valueWithType = ast.newVariableDeclarationStatement(value);
		valueWithType.setType(copy(node.getParameter().getType()));
		
		body.statements().add(valueWithType);
		
		// (2.4) COPY THE ORIGINAL BODY
		Statement placeholder = ast.newEmptyStatement();
		body.statements().add(placeholder);
		ASTUtil.replaceInBlock(placeholder, copy(node.getBody()));
		
		
		// (2.5) ADD THE BODY BLOCK
		loop.setBody(body);
		
		// (3) ADD THE LOOP
		block.statements().add(loop);
		
		// (4) REPLACE THE CODE
		if (node.getParent() instanceof Block){
			ASTUtil.replaceInBlock(node, block);
		}else{
			throw new RuntimeException(node.getParent().getClass().toString());
		}
		
		// (4) DELETE THE OLD NODES
		node.delete();
		
		return true;
	}
	
}
