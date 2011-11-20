package edu.washington.cs.downconvert.rewrite;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;

/**
 * Utility functions for rewriting Eclipse ASTs
 * @author Todd Schiller
 */
public abstract class ASTUtil {

	@SuppressWarnings("unchecked")
	public static <T extends ASTNode> T copy(T node){
		return (T) ASTNode.copySubtree(node.getAST(), node);
	}
	
	public static void replace(ASTNode before, ASTNode after){
		StructuralPropertyDescriptor location = before.getLocationInParent();
		before.getParent().setStructuralProperty(location, after);
	}
	
	public static void replaceInBlock(Statement node, ASTNode replacement){
		if (replacement instanceof Block){
			replaceInBlock(node, (Block) replacement);
		}else if (replacement instanceof Statement){
			replaceInBlock(node, (Statement) replacement);
		}else{
			throw new IllegalArgumentException("replacement has type " + replacement.getClass().getName());
		}
	}
	
	@SuppressWarnings("unchecked")
	public static void replaceInBlock(Statement node, Block replacement){
		replaceInBlock(node, (List<Statement>) replacement.statements());
	}
	
	public static void replaceInBlock(Statement node, Statement replacement){
		List<Statement> ss = new ArrayList<Statement>();
		ss.add(replacement);
		replaceInBlock(node, ss);
	}
	
	@SuppressWarnings("unchecked")
	public static void replaceInBlock(Statement node, List<Statement> replacements){
		if (!(node.getParent() instanceof Block)){
			throw new IllegalArgumentException("Original statement must be a member of a block");
		}
		
		Block block = (Block) node.getParent();
		
		for (int i = 0; i < block.statements().size(); i++){
			if (block.statements().get(i) == node){
				block.statements().remove(i);
				
				for (int j = replacements.size() - 1; j >= 0; j--){
					Statement replacement = replacements.get(j);
					if (replacement.getAST() != node.getAST()){
						throw new IllegalArgumentException("One or more replacements is not from the same AST as the node");
					}
					
					block.statements().add(i, ASTNode.copySubtree(node.getAST(), replacement));
				}
				
			}
		}		
	}
}
