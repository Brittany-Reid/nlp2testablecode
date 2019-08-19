package nlp2code;

/**
 * A snippet is an object that contains a code snippet and related information.
 */
public class Snippet {
	private String code;
	private int id;
	private boolean compiled = false;
	private int errors = -1;
	
	/**
	 * Constructs a snippet object from a String of code;
	 * @param code The code to store in this snippet.
	 */
	Snippet(String code, int id){
		this.code = code;
		this.id = id;
	}
	
	/**
	 * Stores the given code, overwriting previous code.
	 * @param code The code to store.
	 */
	public void setCode(String code) {
		this.code = code;
		changed();
	}
	
	/**
	 * Update error information.
	 * @param errors The error value to use.
	 */
	public void updateErrors(Integer errors) {
		this.errors = errors;
		compiled = true;
	}
	
	/**
	 * Function to return the stored code.
	 * @return A String code.
	 */
	public String getCode() {
		return code;
	}
	
	/**
	 * Returns the code string with SO source appended.
	 */
	public String getSourcedCode() {
		return "//https://stackoverflow.com/questions" + id + "\n" + code;
	}
	
	/**
	 * Function to return the question ID this snippet belongs to.
	 * @return The Integer ID.
	 */
	public int getID() {
		return id;
	}
	
	/**
	 * Private function to reset values on change.
	 */
	private void changed() {
		compiled = false;
		errors = -1;
	}
	
}
