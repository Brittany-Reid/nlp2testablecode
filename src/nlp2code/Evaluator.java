package nlp2code;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import org.apache.logging.log4j.Logger;

/* Evaluator Class
 * Handles evaluation of snippets and evaluation pipeline.
 */
class Evaluator{
	static Logger logger = Activator.getLogger();
	static private IMCompiler compiler;
	static public Integer errorFree = 0;
	static String testOutput;
	static List<String> testInput;
	static Integer passed;
	static Integer compiled;
	static Integer retrieved;
	
	/* Returns an ordered vector of snippets
	 * Based on evaluation metrics. */
	public static Vector<String> evaluate(Vector<String> snippets, String before, String after){
		HashMap<String, Integer> compilerErrors;
		HashMap<String, Integer> passedTests = new HashMap<String, Integer>();
		Vector<String> ordered;
		List<String> sorted;
		
		retrieved = snippets.size();
		
		compiler = new IMCompiler(false, false, false);
		
		//compile all snippets and get their errors, modifications are made in this step
		compilerErrors = getCompilerErrors(snippets, before, after);
		
		//get passing test cases
		if(QueryDocListener.testInput != null && QueryDocListener.testInput.size() >= 4) {
			if(InputHandler.previous_query.equals("convert string to integer")) {
				passedTests = getPassedTests(compilerErrors);
			}
		}
		else {
			//System.out.println("Cannot generate test cases for this task.");
		}
		
		//we need a final for our anon class
		final HashMap<String, Integer> passedTestsFinal = passedTests;
		
		//sort
		sorted = new ArrayList<String>(compilerErrors.keySet());
		Collections.sort(sorted, new Comparator<String>() {
		    public int compare(String left, String right) {
		    	Integer compare = Integer.compare(compilerErrors.get(left), compilerErrors.get(right));
		    	//if tied, look at the passed tests
		    	if(compare == 0 && !passedTestsFinal.isEmpty()) {
		    		compare = Integer.compare(passedTestsFinal.get(right), passedTestsFinal.get(left));
		    	}
		    	return compare;
		    }
		});
		
		ordered = new Vector<String>(sorted);
		
		
		return ordered;
	}
	
	/*Returns a hashmap of code snippets to compiler errors*/
	private static HashMap<String, Integer> getCompilerErrors(Vector<String> snippets, String b, String a){
		HashMap<String, Integer> compilerErrors = new HashMap<String, Integer>();
		
		//set the before and after
		compiler.before = b;
		compiler.after = a;

		for(String s : snippets) {
			
			//count compiler errors
			Integer errorCount = compiler.compile(b+s+a);
			
			//if we have zero errors
			if(errorCount == 0) {
				errorFree++;
			}
			else {
				//this should be moved out of compiler
				//s = compiler.modify(s);
			}
			
			//add snippet to hashmap
			compilerErrors.put(s, errorCount);
		}
		
		
		return compilerErrors;
	}
	
	private static HashMap<String, Integer> getPassedTests(HashMap<String, Integer> snippets){
		HashMap<String, Integer> passedMap = new HashMap<String, Integer>();
		List<String> argumentTypes = new ArrayList<String>();
		testInput = new ArrayList<String>();
		String returnType;
		
		//get arguments
		for(int i=0; i<QueryDocListener.testInput.size()-2; i++) {
			if(i % 2 == 0) {
				argumentTypes.add(QueryDocListener.testInput.get(i));
			}
			else {
				testInput.add(QueryDocListener.testInput.get(i));
			}
		}
		
		//get return type
		returnType = QueryDocListener.testInput.get(QueryDocListener.testInput.size()-2);
		testOutput = QueryDocListener.testInput.get(QueryDocListener.testInput.size()-1);
		
		compiled = 0;
		passed = 0;
		
		for(String s : snippets.keySet()) {
			Integer passCount = 0;
			
			//if we had no compiler error, try testing
			if(snippets.get(s) == 0) {
				compiled++;
				passCount = Tester.test(s, compiler.before, compiler.after, argumentTypes, returnType);
				System.out.println("Passed: " + passCount);
				if(passCount > 0) {
					passed++;
				}
			}
			
			//add to pass hashmap
			passedMap.put(s, passCount);
		}
		
		return passedMap;
	}
	
}