package edu.washington.cs.downconvert.tests;

import java.util.ArrayList;
import java.util.List;

public class EnhancedForTest {

	public static void single(){
		List<Integer> xs = new ArrayList<Integer>();
		for(Integer x : xs){
			System.out.println(x);
		}
	}
	
	public static void nested(){
		List<List<Integer>> xs = new ArrayList<List<Integer>>();
		
		for (List<Integer> ys : xs){
			for (Integer y : ys){
				System.out.println(y);
			}
		}
	}
	
}
