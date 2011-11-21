package edu.washington.cs.downconvert.rewrite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.Region;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MultiTextEdit;

/**
 * Insert JML contracts for generics
 * @author Todd Schiller
 */
public class InsertGenericsJml extends ASTVisitor{ 

	private List<TypeRecord> records = new ArrayList<TypeRecord>();
	AST ast;
	
	/**
	 * true iff <code>type</code> implements / extend java.lang.Collection
	 * @param type the type
	 * @return true iff <code>type</code> implements / extend java.lang.Collection
	 */
	private boolean isCollectionType(ITypeBinding type){
		// there is no canonical binding for java.lang.Collection, unfortunately
		
		if (type.getErasure().getQualifiedName().equals("java.util.Collection")){
			return true;
		}else if(!type.equals(ast.resolveWellKnownType("java.lang.Object")) && !type.isInterface()
				&& isCollectionType(type.getSuperclass())){
			return true;
		}else{
			for (ITypeBinding i : type.getInterfaces()){
				if (isCollectionType(i)){
					return true;
				}
			}
			return false;
		}
	}
	
	/**
	 * true iff <code>type</code> implements / extend java.lang.Collection
	 * @param type the type
	 * @return true iff <code>type</code> implements / extend java.lang.Collection
	 */
	private boolean isCollectionType(Type type){
		return isCollectionType(type.resolveBinding());
	}
	
	private static String getLeadingWhitespace(String str){
		return str.substring(0, str.indexOf(str.trim().charAt(0)));
	}
	
	public static MultiTextEdit commentGenerics(Document source, CompilationUnit unit){
		MultiTextEdit edits = new MultiTextEdit();
		
		ArrayList<TypeRecord> sorted = new ArrayList<TypeRecord>(getTypeRecords(unit));
		
		//sort in reverse order, so we can make the modifications in reverse
		Collections.sort(sorted, new Comparator<TypeRecord>(){
			@Override
			public int compare(TypeRecord o1, TypeRecord o2) {
				return -Integer.valueOf(o1.region.getOffset()).compareTo(o2.region.getOffset());
			}
		});
		
		for (TypeRecord r : sorted){
			try{
				int lastLine = source.getLineOfOffset(r.region.getOffset() + r.region.getLength());
				int nextLineOffset = source.getLineOffset(lastLine + 1);

				String line = source.get(source.getLineOffset(lastLine), source.getLineLength(lastLine));
				
				String content = null;
				switch (r.recordType){
				case Field:
					 content = "//@invariant " + r.name.getFullyQualifiedName() + ".elementType = \\type(" + r.type.toString() + ");";
					 break;
				case LocalVariable:
					 content = "//@set " + r.name.getFullyQualifiedName() + ".elementType = \\type(" + r.type.toString() + ");";
					 break;
				}
				
				edits.addChild(new InsertEdit(nextLineOffset, 
						getLeadingWhitespace(line) + content + source.getLineDelimiter(lastLine)));

			}catch (BadLocationException ex){
				throw new RuntimeException("Error inserting JML @set statement", ex);
			}
		}
		
		return edits;
	}
	
	
	public static List<TypeRecord> getTypeRecords(CompilationUnit unit){
		InsertGenericsJml c = new InsertGenericsJml();
		c.ast = unit.getAST();
		unit.accept(c);
		
		return c.records;
	}
	
	protected static class TypeRecord{
		private enum RecordType { LocalVariable, Field };
		
		private final SimpleName name;
		private final Type type;
		private final Region region;
		private final RecordType recordType;
		
		protected TypeRecord(SimpleName name, Type type, Region region, RecordType recordType) {
			super();
			this.name = name;
			this.type = type;
			this.region = region;
			this.recordType = recordType;
		}
	}
	
	@Override
	public boolean visit(VariableDeclarationStatement node){
		if (node.getType().isParameterizedType() && isCollectionType(node.getType())){
			ParameterizedType type = (ParameterizedType) node.getType();
			
			if (type.typeArguments().size() == 1){
				for (Object f : node.fragments()){
					VariableDeclarationFragment fragment = (VariableDeclarationFragment) f;
					records.add(new TypeRecord(
							fragment.getName(), 
							(Type) type.typeArguments().get(0), 
							new Region(node.getStartPosition(), node.getLength()),
							TypeRecord.RecordType.LocalVariable
							));
				}
				
			}
		}
		return true;
	}
	
	public boolean visit(FieldDeclaration node){
		if (node.getType().isParameterizedType() && isCollectionType(node.getType())){
			ParameterizedType type = (ParameterizedType) node.getType();
			
			if (type.typeArguments().size() == 1){
				for (Object f : node.fragments()){
					VariableDeclarationFragment fragment = (VariableDeclarationFragment) f;
					records.add(new TypeRecord(
							fragment.getName(), 
							(Type) type.typeArguments().get(0), 
							new Region(node.getStartPosition(), node.getLength()),
							TypeRecord.RecordType.Field
							));
				}
				
			}
		}
		
		return true;
	}
	
}
