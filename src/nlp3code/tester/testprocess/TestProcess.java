package nlp3code.tester.testprocess;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import javax.tools.ToolProvider;

import java.util.List;

import nlp3code.compiler.IMClassLoader;
import nlp3code.compiler.IMCompiledCode;
import nlp3code.compiler.IMCompiler;
import nlp3code.tester.ProcessClassLoader;

/**
 * We spawn a new process running the main function of this class when we want to test code.
 * This is because we can't stop threads safely when they run untrusted code.
 * The code from SO will not be written to respect thread interrupts.
 * We want to run that code in a separate JVM.
 * http://whiley.org/2011/04/15/one-thing-i-really-hate-about-the-jvm/
 */
public class TestProcess{
	public static final String ISOLATED_TEST_RUNNER_METHOD_NAME = "runTests";
	
	/**
	 * The function that is run. Debugging tip: if you get an Invalid Stream Header, check the
	 * hex values. 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException{
		ObjectInputStream ois = new ObjectInputStream(System.in);
		ObjectOutputStream oos = new ObjectOutputStream(System.out);
		//Don't do any output before System.out is redirected...
		
		try {
			System.setOut(System.err); // to divert any output from the child code
			String methodName = (String) ois.readObject();
			String className = (String) ois.readObject();
			String classpath = (String) ois.readObject();
			String code = (String) ois.readObject();
			
			//use system compiler as we dont need to analyse errors, just so we dont need to deal with the patch jar
			List<String> options = new ArrayList<String>();
			options.add("-cp");
			options.add(classpath);
			IMCompiler compiler = new IMCompiler(ToolProvider.getSystemJavaCompiler(), options, null);
			compiler.addSource(className, code);
			compiler.compileAll();
			
			//if this didn't compile, bail out
			if(compiler.getErrors() != 0) {
				oos.writeInt(0);
			}
			
			IMClassLoader classLoader = (IMClassLoader) compiler.fileManager.getClassLoader(null);
			
			IMCompiledCode classFile = classLoader.getCompiled(className);
			
			ProcessClassLoader urlClassLoader = new ProcessClassLoader(classpath);
			urlClassLoader.addCompiledCode(className, classFile);
			
			UnitTest test = new UnitTest(className, methodName);
			UnitTestResult result = runTest(test, urlClassLoader);
			
			if(result == null) {
				Exception e = new Exception("NULL RESULT!");
				throw e;
			}
			else {
				if(result.getPassed() == true) {
					oos.writeInt(1);
				}
				else {
					oos.writeInt(0);
				}
			}
		} catch (Exception e) {
			oos.writeInt(-1);
			oos.writeObject(e.toString());
		}
		oos.flush();
	}

	private static UnitTestResult runTest(UnitTest test, ProcessClassLoader classLoader) {
		UnitTestResult result = new UnitTestResult(test);
		String className = test.getFullClassName();
	    String methodName = test.getMethodName();
	    
	    //get the junit runner class
	    Class<?> runnerClass = null;
        try {
            runnerClass = classLoader.loadClass(JUnitBridge.class.getName());
        } catch (ClassNotFoundException e) {
            return null;
        }
        
		
		//make an object of this class
		Object runner = null;
        try {
            runner = runnerClass.newInstance();
        } catch (InstantiationException e) {
           System.out.println("Could not instantiate isolated test runner: " + e);
           return null;

        } catch (IllegalAccessException e) {
        	System.out.println("Could not instantiate isolated test runner: " + e);
            return null;
        }
		
        //get the run tests method
		Method method = null;
        try {
        	//why can gintool find method with cache.unittest by searching with system.unittest?
            method = runner.getClass().getMethod(JUnitBridge.BRIDGE_METHOD_NAME, String.class, String.class);
        } catch (NoSuchMethodException e) {
            System.out.println("Could not run isolated tests runner, can't find method: " + ISOLATED_TEST_RUNNER_METHOD_NAME);
            return null;
        } catch(NoClassDefFoundError e) {
        	System.out.println("Missing a dependancy of JUnitBridge.class.");
        	e.printStackTrace();
        	return null;
        }
        
        try {
            result = (UnitTestResult) method.invoke(runner, test.getMethodName(), test.getFullClassName());
        } catch (IllegalAccessException e) {
        	e.printStackTrace();
        } catch (InvocationTargetException e) {
        	e.printStackTrace();
        }
        
        return result;
	}
}