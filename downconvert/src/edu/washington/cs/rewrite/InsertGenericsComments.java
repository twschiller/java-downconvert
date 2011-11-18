package edu.washington.cs.rewrite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.Region;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MultiTextEdit;

public class InsertGenericsComments {

	public static MultiTextEdit commentGenerics(Document source, CompilationUnit unit){
		String content = source.get();
		
		MultiTextEdit edits = new MultiTextEdit();
		
		ArrayList<Region> sorted = new ArrayList<Region>(GenericLocations.getGenericLocations(unit));
		
		//sort in reverse order, so we can make the modifications in reverse
		Collections.sort(sorted, new Comparator<Region>(){
			@Override
			public int compare(Region o1, Region o2) {
				return -Integer.valueOf(o1.getOffset()).compareTo(o2.getOffset());
			}
		});
		
		for (Region r : sorted){
			String sub = content.substring(r.getOffset(), r.getOffset() + r.getLength());
			edits.addChild(new InsertEdit(r.getOffset() + sub.indexOf('<'),"/*" + getGeneric(sub) + "*/"));
		}
		
		return edits;
	}
	
	private static String getGeneric(String x){
		int open = 0;
		
		StringBuilder b = new StringBuilder();
	
		for (int i = 0; i < x.length(); i++){
			
			if (x.charAt(i) == '<'){
				open++;
			}
			
			if (open > 0){
				b.append(x.charAt(i));
			}
			
			if (x.charAt(i) == '>'){
				open--;
				if (open == 0){
					return b.toString();
				}
			}
		}
		
		throw new IllegalArgumentException("template expresion is unbalanced");
	}
	
	protected static class GenericLocations extends ASTVisitor{

		private Set<Region> regions = new HashSet<Region>(); 

		CompilationUnit unit;

		public static Set<Region> getGenericLocations(CompilationUnit unit){
			GenericLocations c = new GenericLocations();
			c.unit = unit;
			c.unit.accept(c);
			return c.regions;
		}

		@Override
		public boolean visit(ParameterizedType node){
			regions.add(new Region(node.getStartPosition(), node.getLength()));

			// don't continue. we only want the outer-level <>'s
			return false;
		}

		@Override
		public boolean visit(ClassInstanceCreation node){
			if (!node.typeArguments().isEmpty()){
				regions.add(new Region(node.getStartPosition(), node.getLength()));
			}
			return true;
		}

		@Override
		public boolean visit(MethodInvocation node){
			if (!node.typeArguments().isEmpty()){
				regions.add(new Region(node.getStartPosition(), node.getLength()));
			}
			return true;
		}
	}
}
