package nlp3code.tester.testprocess;

import java.util.List;

/**
 * Class UnitTestResultSet
 *   Set of results for unit tests. Includes functions for querying test result information.
 */
public class UnitTestResultSet {
	private List<UnitTestResult> results;
	
	/**
	 * Constructor from a List of UnitTestResults.
	 */
	public UnitTestResultSet(List<UnitTestResult> results) {
		this.results = results;
	}
	
	/**
	 * Returns the list of UnitTestResults
	 */
	public List<UnitTestResult> getResults() {
		return results;
    }
	
	/**
	 * Returns the number of successful (passed) tests within the result set.
	 */
	public Integer getSuccessful() {
		Integer passed = 0;
        for (UnitTestResult testResult : results) {
            if (testResult.getPassed()) {
                passed+=1;
            }
        }
        return passed;
	}
	
	/**
	 * Returns if all tests were successful.
	 */
	public boolean allTestsSuccessful() {
        for (UnitTestResult testResult : results) {
            if (!testResult.getPassed()) {
                return false;
            }
        }
        return true;
    }
}