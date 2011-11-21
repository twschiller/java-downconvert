package edu.washington.cs.downconvert.tests;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Test inserting JML elementType annotations for Generics
 * @author Todd Schiller
 */
public class JmlCollectionsTest {

	List<String> fieldDeclaration = new ArrayList<String>();
	Set<Number> fieldDeclaration2;
	
	public JmlCollectionsTest(HashSet<Number> parameter){
		this.fieldDeclaration2 = parameter;
	}
	
	public void StatementTest(){
		List<Integer> variableDeclaration = new ArrayList<Integer>();
	}
	
}

