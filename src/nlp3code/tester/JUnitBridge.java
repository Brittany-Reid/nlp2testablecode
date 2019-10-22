package nlp3code.tester;

import java.lang.IllegalAccessException;
import java.lang.NoSuchFieldException;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;

/** 
 * Class for running Junit tests from invocation within same JVM.
 * We are currently not using the plugin classpath, so any dependencies
 * need to be manually handled.
 */
public class JUnitBridge {
	public static final String BRIDGE_METHOD_NAME = "runTests";
	
	/**
	 * The function that will be invoked. Runs JUnit tests.
	 */
	public UnitTestResult runTests(String methodName, String className) {
		UnitTest test = new UnitTest(className, methodName);
		UnitTestResult result = new UnitTestResult(test);
		Request request = null;
		
		try {
			request = buildRequest(test);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		JUnitCore jUnitCore = new JUnitCore();
		jUnitCore.addListener(new TestRunListener(result));
		jUnitCore.run(request);
		
		return result;
	}
	
	/**
	 * Function to build a JUnit Request method using a UnitTest object.
	 */
	public Request buildRequest(UnitTest test) throws ClassNotFoundException, NoSuchMethodException, NoSuchFieldException, IllegalAccessException {
		Class<?> clazz = null;
		
		String testClassname = test.getFullClassName();
		ClassLoader loader = this.getClass().getClassLoader();
		clazz = loader.loadClass(testClassname);
		String methodName = test.getMethodName();
		
		return Request.method(clazz, methodName);
	}
}