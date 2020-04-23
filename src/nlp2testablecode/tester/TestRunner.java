package nlp2testablecode.tester;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.StringUtils;

import nlp2testablecode.compiler.IMCompiledCode;
import nlp2testablecode.tester.testprocess.JUnitBridge;
import nlp2testablecode.tester.testprocess.UnitTest;
import nlp2testablecode.tester.testprocess.UnitTestResult;
import nlp2testablecode.tester.testprocess.UnitTestResultSet;

public class TestRunner {
	public static final String ISOLATED_TEST_RUNNER_METHOD_NAME = "runTests";
	private String packageName;
	private String className;
	private String classPath;
	private List<UnitTest> tests;
	
	/**
	 * Constructor
	 */
	public TestRunner(String fullyQualifiedClassName, String classPath, List<UnitTest> unitTests) {
		this.className = fullyQualifiedClassName;
		this.classPath = classPath;
		if (className.contains(".")) {
			this.packageName = StringUtils.substringBeforeLast(className, ".");
	    } else {
	    	this.packageName = "";
	    }
		
		//get unittests
		if(unitTests == null) {
			this.tests = null;
		}
		else {
			this.tests = unitTests;
		}
	}

	public UnitTestResultSet runTests(IMCompiledCode code) {
		List<UnitTestResult> results = null;
		CacheClassLoader classLoader = new CacheClassLoader(this.getClassPath());
		
		//add code to classloader
		classLoader.addCompiledCode(code.getClassName(), code);
		
		//run tests
		results = runTests(classLoader);
		//if running tests failed in some way
		if(results == null) return null;
		
		return new UnitTestResultSet(results);
	}
	
	private LinkedList<UnitTestResult> runTests(CacheClassLoader classLoader) {
		tests = testsForClass(classLoader);
		LinkedList<UnitTestResult> results = new LinkedList<>();
        for (UnitTest test: this.getTests()) {
        	UnitTestResult result = runSingleTest(test, classLoader);
            if(result == null) {
            	//System.err.println("Error: Testing failed.");
            	return null;
            }
            results.add(result);
        }
        
        return results;
	}
	
	private UnitTestResult runSingleTest(UnitTest test, CacheClassLoader classLoader) {
	        Class<?> runnerClass = null;
	        
	        try {
	            runnerClass = classLoader.loadClass(JUnitBridge.class.getName());
	        } catch (ClassNotFoundException e) {
	            System.out.println("Could not load isolated test runner - class not found.");
	            return null;
	        }
	        
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
	        
	        AtomicReference<Object> resultAt = new AtomicReference<>();
	        Object result = null;
	        
        	final Method fMethod = method;
        	final Object fRunner = runner;
        	final UnitTest fTest = test;
        	
        	Runnable runnable = new Runnable() {
                @Override
                public void run() {
                	try {
						resultAt.set(fMethod.invoke(fRunner, test.getMethodName(), fTest.getFullClassName()));
					} catch (IllegalAccessException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IllegalArgumentException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                }
            };

            Thread thread = new Thread(runnable);
            thread.start();
            boolean stop = false;
            int timepool = 10000;
            while(stop == false && thread.isAlive()) {
            	try {
            		//sleep for half a second then check
					Thread.sleep(500);
					timepool-=500;
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            	//this is evil... but does it work?
            	//because we cant stop our current implementation of testing
            	//through regular means, there remain hanging threads
            	//this may actually be better for now....
            	//i have no idea if the issues with stop even apply here
            	if(timepool <= 0) {
            		System.out.println("Timed out");
	            	thread.stop();
	            	stop =true;
            	}
            }
	        
	        
//	        try {
//	            result = method.invoke(runner, test.getMethodName(), test.getFullClassName());
//	        } catch (IllegalAccessException e) {
//	        	e.printStackTrace();
//	        } catch (InvocationTargetException e) {
//	        	e.printStackTrace();
//	        }
	        result = resultAt.get();
	        UnitTestResult res = (UnitTestResult) result;
	        return res;
	    }
	
	/**
	 * Constructs test set from class
	 */
	public List<UnitTest> testsForClass(CacheClassLoader classLoader) {
		List<UnitTest> tests = new LinkedList<UnitTest>();
		
		//for now set this manually
		UnitTest test = new UnitTest(className, "junittest");
		tests.add(test);
		
		//im unsure how gintool is able to get annotated methods
		//without the same issue popping up
		//i wonder if ensuring cacheclassloader always loads test from the plugin/system classloader
		//solves this issue anyway but i havent tried since adding the eclipse specific code

		return tests;
	}
	
	/* Getters */
	
	public List<UnitTest> getTests(){
		return tests;
	}
	
	public String getClassName() {
		return className;
	}
	
	public String getClassPath() {
		return classPath;
	}
	
	public String getPackageName() {
		return packageName;
	}
	
}
