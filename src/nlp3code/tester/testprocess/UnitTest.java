package nlp3code.tester.testprocess;

/** Class UnitTest
 * 	A test to be run.
 */
public class UnitTest{
	
	private String className;
	private String methodName;
	
	public UnitTest(String className, String methodName) {
        this.className = className;
        this.methodName = methodName;
    }
	
	public String getFullClassName() {
		return className;
	}
	
	public String getMethodName() {
		return methodName;
	}
	
	public String getTestName() {
        return className + "." + methodName;
    }

//    public String getTopClassName() {
//        return StringUtils.substringBefore(className, "$");
//    }
//
//    public String getInnerClassName() {
//        return StringUtils.substringAfter(className, "$");
//    }
}
