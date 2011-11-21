package edu.washington.cs.downconvert.tests;

import java.util.ArrayList;
import java.util.List;

/**
 * A test for generic classes with multiple type parameters; mind
 * your P's and Q's.
 * @author Todd Schiller
 * @param <P> a unbounded type
 * @param <Q> a bounded type
 */
public class GenericClassTest<P,Q extends Number>{

	/**
	 * Should be converted to Object
	 */
	P myP;
	
	/**
	 * Should be converted to type List
	 */
	List<Q> myQs;
	
	/**
	 * Argument should be converted to type Object
	 * @param p
	 */
	public GenericClassTest(P p){
		this.myP = p;
		this.myQs = new ArrayList<Q>();
	}
	
	/**
	 * Return value should be converted to type Object
	 * @return
	 */
	public P getP(){
		return myP;
	}
	
	/**
	 * Return value should be converted to type Number
	 * @return
	 */
	public Q firstQ(){
		return myQs.get(0);
	}
	
	/**
	 * Argument should be converted to type Q.
	 * @param q
	 * @return
	 */
	public boolean containsQ(Q q){
		return myQs.contains(q);
	}
}
