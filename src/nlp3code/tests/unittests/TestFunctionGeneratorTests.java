package nlp3code.tests.unittests;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import nlp3code.tester.TestFunctionGenerator;

/**
 *	Unit tests for the {@link TestFunctionGenerator} class.
 *
 */
public class TestFunctionGeneratorTests {

	/**
	 * Test a single argument test case generation.
	 */
	@Test
	public void testSingleTestCaseGeneration() {
		//arg: string, return: int
		List<String> types = new ArrayList<>();
		types.add("int");
		types.add("String");
		
		String test = TestFunctionGenerator.generateTestCase(types, "function").toString();
		assertEquals("// Format: assertEquals(int, function(String));\r\nassertEquals(0, function(\"empty\"));", test);
	}
	
	/**
	 * Test a multiple argument test case generation.
	 */
	@Test
	public void testMultiTestCaseGeneration() {
		//arg: String, String, return: int
		List<String> types = new ArrayList<>();
		types.add("int");
		types.add("String");
		types.add("String");
		
		String test = TestFunctionGenerator.generateTestCase(types, "function").toString();
		assertEquals("// Format: assertEquals(int, function(String, String));\r\nassertEquals(0, function(\"empty\", \"empty\"));", test);
	}

	/**
	 * Test function generation.
	 */
	@Test
	public void testFunctionGen() {
		//arg: string, return: int
		List<String> types = new ArrayList<>();
		types.add("int");
		types.add("String");
		
		String function = TestFunctionGenerator.generateTestFunction(types, "function").toString();
		assertEquals("@Test()\r\npublic void nlp3code_test() {\r\n    // Format: assertEquals(int, function(String));\r\n    assertEquals(0, function(\"empty\"));\r\n}", function);
	}
}
