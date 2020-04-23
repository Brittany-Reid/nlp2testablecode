package nlp2testablecode.tester.testprocess;

import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

/**
 * Class TestRunListener
 * 	Listens for test events, and record information to the specified UnitTestResult
 */
public class TestRunListener extends RunListener {
	private UnitTestResult unitTestResult;
	
	/**
	 * Constructor that takes a UnitTestResult to store our test information in.
	 */
	public TestRunListener(UnitTestResult unitTestResult) {
        this.unitTestResult = unitTestResult;
    }

	/**When our test is finished, we make sure to set passed state.*/
    public void testRunFinished(Result result) throws Exception {
        if (result.wasSuccessful()) {
            unitTestResult.setPassed(true);
        }
    }
	
	
	/*Failures*/
	
	public void testAssumptionFailure(Failure failure) {;
        unitTestResult.addFailure(failure);
    }
	
	public void testFailure(Failure failure) throws Exception {
		unitTestResult.addFailure(failure);
	}	
	
}
