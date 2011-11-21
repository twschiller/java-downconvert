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
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;

/**
 * <p>Convert {@link EnhancedForStatement}s into {@link WhileStatement}s using
 * {@link java.util.Iterator} with the proper type parameter.</p>
 * 
 * <p>Currently the new iterators are names <code>it0</code>, <code>it1</code>, ...
 * which may introduce naming conflicts if pre-existing variables in scope 
 * use the same naming scheme.</p>
 * 
 * @author Todd Schiller
 */
public class ConvertEnhancedFor extends ASTVisitor{

	private final CompilationUnit unit;
	private final AST ast;

	private int count = 0;
	
	protected ConvertEnhancedFor(CompilationUnit unit) {
		this.unit = unit;
		this.ast = unit.getAST();
	}

	public static void convert(CompilationUnit unit){
		ConvertEnhancedFor c = new ConvertEnhancedFor(unit);
		c.unit.accept(c);
	}

	/**
	 * get the name for the iterator variable for the enhanced for loop that is
	 * currently being transformed
	 * @return the name for the iterator variable
	 */
	private SimpleName iteratorVar(){
		return ast.newSimpleName("it" + count);
	}
	
	@Override
	public void endVisit(CompilationUnit node){
		// if a loop was converted, add an import for java.util.Iterator if it
		// does not already exist.
		
		if (count > 0){
			for (Object i : node.imports()){
				ImportDeclaration d = (ImportDeclaration) i;
				if (d.getName().getFullyQualifiedName().equals(ast.newName("java.util.Iterator").getFullyQualifiedName())){
					return;
				}
			}
			ImportDeclaration i = ast.newImportDeclaration();
			i.setName(ast.newName("java.util.Iterator"));
			node.imports().add(i);
		}
	}
	
	@Override
	public boolean visit(EnhancedForStatement node){
		Block block = ast.newBlock();
		
		// (1) DECLARE THE ITERATOR
		MethodInvocation iterator = ast.newMethodInvocation();
		iterator.setExpression(copy(node.getExpression()));
		iterator.setName(ast.newSimpleName("iterator"));
	
		VariableDeclarationFragment it = ast.newVariableDeclarationFragment();
		it.setName(iteratorVar());
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
		hasNext.setExpression(iteratorVar());
		hasNext.setName(ast.newSimpleName("hasNext"));
		loop.setExpression(hasNext);
		
		// (2.2) DECLARE THE NEW LOOP BODY
		Block body = ast.newBlock();
				
		// (2.3) CALL NEXT
		VariableDeclarationFragment value = ast.newVariableDeclarationFragment();
		value.setName(copy(node.getParameter().getName()));
		
		MethodInvocation next = ast.newMethodInvocation();
		next.setExpression(iteratorVar());
		next.setName(ast.newSimpleName("next"));
		
		CastExpression cast = ast.newCastExpression();
		cast.setType(copy(node.getParameter().getType()));
		cast.setExpression(next);
		
		value.setInitializer(cast);
		
		VariableDeclarationStatement valueWithType = ast.newVariableDeclarationStatement(value);
		valueWithType.setType(copy(node.getParameter().getType()));
		
		body.statements().add(valueWithType);
		
		// (2.4) COPY THE ORIGINAL FOREACH LOOP BODY
		Statement placeholder = ast.newEmptyStatement();
		body.statements().add(placeholder);
		ASTUtil.replaceInBlock(placeholder, copy(node.getBody()));
		
		// (2.5) SET THE NEW BODY BLOCK
		loop.setBody(body);
		
		// (3) ADD THE WHILE LOOP (PREVIOUSLY JUST CONTAINED LOOP ITERATOR DEFINITION)
		block.statements().add(loop);
		
		// (4) REPLACE THE CODE
		if (node.getParent() instanceof Block){
			ASTUtil.replaceInBlock(node, block);
		}else{
			throw new RuntimeException("Expecting a parent block, got :" + node.getParent().getClass().toString());
		}
		
		// (4) DELETE THE OLD NODES
		node.delete();
		
		count++;
		
		return true;
	}
	
}
