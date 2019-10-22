package nlp3code.tester;

import org.junit.runner.notification.Failure;

/** Class TestResult
 * 	Object for storing result information of a JUnit test run.
 */
public class UnitTestResult{
	private UnitTest test;
	private boolean passed = false;
	private boolean timedOut = false;
	private Integer rep;
	private String exceptionType = "";
    private String exceptionMessage = "";
    private String expectedValue = "";
    private String actualValue = "";
    private long executionTime = 0;
    private long cpuTime = 0;
	
    /**
     * Constructor using the previously run test.
     */
	public UnitTestResult(UnitTest test) {
		this.test = test;
	}
	
	/**
	 * Handle failure and record failure information.
	 */
	public void addFailure(Failure failure) {
		this.passed = false;
		
		Throwable rootCause = failure.getException();
		//get the root cause
        while(rootCause.getCause() != null &&  rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }
        //System.out.println(rootCause.getLocalizedMessage());
        
        this.exceptionType = rootCause.getClass().getName();
        this.exceptionMessage = rootCause.getMessage();
	}
	
	
	/*Getters*/
	
    public UnitTest getTest() {
        return test;
    }
    
    public Integer getRep() {
    	return rep;
    }
    
    public boolean getPassed() {
        return passed;
    }

    public boolean getTimedOut() {
        return timedOut;
    }

    public String getExceptionType() {
        return exceptionType;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public String getAssertionExpectedValue() {
        return expectedValue;
    }

    public String getAssertionActualValue() {
        return actualValue;
    }

    public long getExecutionTime() {
        return executionTime;
    }

    public long getCPUTime() {
        return cpuTime;
    }
	
	public void setExceptionType(String exceptionType) {
        this.exceptionType = exceptionType;
    }

    public void setExceptionMessage(String exceptionMessage) {
        this.exceptionMessage = exceptionMessage;
    }
    
    /*Setters*/
    
    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public void setTimedOut(boolean timedOut) {
        this.timedOut = timedOut;
    }

    public void setExpectedValue(String expectedValue) {
        this.expectedValue = expectedValue;
    }

    public void setActualValue(String actualValue) {
        this.actualValue = actualValue;
    }
    
    public void setExecutionTime(long testExecutionTime)  {
        this.executionTime = testExecutionTime;
    }

    public void setCPUTime(long testCPUTime)  {
        this.cpuTime = testCPUTime;
    }
    
	
}