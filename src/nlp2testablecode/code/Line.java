package nlp2testablecode.code;

/**
 * A Line is a representation of a single line of code, as well as state information.
 */
public class Line{
	//The string contents of the code line
	private String contents = null;
	//If the line has been deleted
	private boolean deleted = false;
	
	/**
	 * Constructs a new Line from a string.
	 * @param contents
	 */
	Line(String contents){
		this.contents = contents;
	}
	
	/**
	 * Copy Constructor.
	 */
	Line(Line that){
		this.contents = that.contents;
		this.deleted = that.deleted;
	}
	
	public int length() {
		return contents.length();
	}
	
	public void delete() {
		deleted = true;
	}
	
	public String get() {
		return contents;
	}
	
	public boolean isDeleted() {
		return deleted;
	}
}
