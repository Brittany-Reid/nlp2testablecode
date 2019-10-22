package nlp3code.code;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;


import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.BlockStmt;

import nlp3code.DocHandler;

/**
 * A snippet object represents a piece of code to be inserted into the user's editor.
 * A snippet is separated into a list of Fragment objects, when a snippet contains
 * segments that need to be integrated at different locations. By default, import
 * statements will be separated into their own fragment during creation. The first
 * fragment in the list always represents the import block, even if a snippet has
 * no associated imports.
 * 
 * The previous implementation simply had a separate list for imports, but hopefully
 * this new representation is more easily extended.
 * 
 * The fragment list should be in order of position: imports -> fields -> snippets -> methods -> classes.
 * 
 * There should only ever be one snippet type fragment.
 * 
 * It's super confusing but I honestly don't know what else to do.
 */
public class Snippet implements Comparable<Snippet>{
	//Code, as a list of fragments
	private List<Fragment> code = null;
	//Thread ID
	private int ID;
	//Cached size
	private int size = -1;
	//cached LOC
	private int LOC = -1;
	//cached errors
	private int errors = -1;
	//cached tests
	private int passedTests = -1;
	//cached diagnostics
	private List<Diagnostic<? extends JavaFileObject>> diagnostics = null;
	//cached AST
	private Node ast = null;
	List<String> arguments = null;
	String returnType = null;
	
	/**
	 * Constructs a snippet object from a String of code;
	 * @param code The code to store in this snippet.
	 * @param ID The snippet's thread ID.
	 */
	public Snippet(String code, int ID){
		this.ID = ID;
		constructFragments(code);
	}
	
	/**
	 * Copy constructor.
	 */
	public Snippet(Snippet that) {
		this.ast = that.ast;
		this.diagnostics = that.diagnostics;
		this.passedTests = that.passedTests;
		this.errors = that.errors;
		this.LOC = that.LOC;
		this.size = that.size;
		this.ID = that.ID;
		this.code = new ArrayList<>();
		for(Fragment f : that.code) {
			this.code.add(new Fragment(f));
		}
	}
	
	/**
	 * Clears cached data on modification.
	 */
	private void changed() {
		size = -1;
		LOC = -1;
		errors = -1;
		passedTests = -1;
		diagnostics = null;
		ast = null;
	}
	
	@Override
	public int compareTo(Snippet b) {
		//empty snippets vs non-empty compiling
		if(b.getLOC() == 0 && LOC != 0) return -1;
		if(LOC == 0 && b.getLOC() != 0) return 1;
		
		//handle negative error value
		if(b.getErrors() == -1 && errors != -1) return -1;
		if(b.getErrors() != -1 && errors == -1) return 1;
		
		//if error value is 0, look at passed tests
		if(b.getErrors() == 0 && errors == 0) {
			//handle any negatives
			if(b.getPassedTests() == -1 && passedTests != -1) return -1;
			if(b.getPassedTests() != -1 && passedTests == -1) return 1;
			
			//otherwise, compare passed, largets at top
			return Integer.compare(b.getPassedTests(), passedTests);
		}
		
		//compare error value
		return Integer.compare(errors, b.getErrors());
	}
	
	/**
	 * Update error information.
	 * @param errors The error value to use.
	 */
	public void updateErrors(Integer errors, List<Diagnostic<? extends JavaFileObject>> diagnostics) {
		this.errors = errors;
		this.diagnostics = diagnostics;
	}
	
	/**
	 * Formats a String of code into our fragment representation.
	 * @param code The code to process.
	 */
	private void constructFragments(String code) {
		BufferedReader bReader = new BufferedReader(new StringReader(code));
		
		//all snippets by default have these sections, even if there are no imports
		Fragment imports = new Fragment(Fragment.IMPORTS);
		Fragment body = new Fragment(Fragment.SNIPPET);
	
		String line;
		try {
			//read string
			while ( (line = bReader.readLine()) != null) {
				if(line.trim().startsWith("import")) {
					//add imports to the import block
					imports.addLine(line);
				}else {
					//by default add to main body of snippet
					body.addLine(line);
				}
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		//add to fragment list
		this.code = new ArrayList<Fragment>();
		this.code.add(imports);
		this.code.add(body);
	}

	
	/**
	 * Deletes the nth line from the snippet, starting at 1.
	 */
	public void deleteLine(int n) {
		int i = 0;
		
		//process each fragment
		for(Fragment fragment : code) {
			
			for(int j = 0; j < fragment.size(); j++) {
				//when we find the position, delete
				if(i == n-1) {
					fragment.deleteLine(j);
				}
				i++;
			}
		}
		
		changed();
	}
	
	/**
	 * Checks if a given line is deleted, starting at 1.
	 * @param n
	 * @return
	 */
	public boolean isDeleted(int n) {
		int i = 0;
		
		//process each fragment
		for(Fragment fragment : code) {
			for(int j = 0; j < fragment.size(); j++) {
				//when we find the position
				if(i == n-1) {
					return fragment.isDeleted(j);
				}
				i++;
			}
		}
		
		return false;
	}
	
	/**
	 * Returns the nth line, starting at 1. Includes deleted lines.
	 */
	public String getLine(int n) {
		int i = 0;
		
		//process each fragment
		for(Fragment fragment : code) {
			
			for(int j = 0; j < fragment.size(); j++) {
				//when we find the position
				if(i == n-1) {
					return fragment.getLine(j);
				}
				i++;
			}
		}
		
		return "";
	}
	
	/**
	 * Returns the number of errors.
	 */
	public int getErrors() {
		return errors;
	}
	
	/**
	 * Returns the number of passed tests.
	 */
	public int getPassedTests() {
		return passedTests;
	}
	
	/**
	 * Return the size of the snippet as number of lines, including deleted lines.
	 */
	public int size() {
		if(size != -1) return size;
		
		size = 0;
		for(Fragment fragment : code) {
			size += fragment.size();
		}
		return size;
	}
	
	/**
	 * Return the lines of code, excluding deleted lines.
	 */
	public int getLOC() {
		if(LOC != -1) return LOC;
		
		LOC = 0;
		for(Fragment fragment : code) {
			LOC += fragment.getLOC();
		}
		return LOC;
	}
	
	public int getNumFragments() {
		return code.size();
	}
	
	public Fragment getFragment(int n) {
		return code.get(n);
	}
	
	/**
	 * Returns the snippet code for compat with old functions.
	 * @return
	 */
	public String getCode() {
		for(Fragment fragment : code) {
			if(fragment.getType() == Fragment.SNIPPET) {
				return fragment.getCode();
			}
		}
		return null;
	}
	
	/**
	 * Compat set code
	 */
	public void setCode(String replacement) {
		for(int i = 0; i<code.size(); i++) {
			if(code.get(i).getType() == Fragment.SNIPPET) {
				code.set(i, new Fragment(replacement));
			}
		}
		changed();
	}
	
	/**
	 * Inserts an import fragment.
	 * @return
	 */
	private static String insertImports(Fragment importFragment, String surrounding) {
		int offset = DocHandler.getImportOffset(surrounding);
		if(DocHandler.imports != null && DocHandler.imports.size() > 0) {
			for(String i : DocHandler.imports) {
				importFragment.deleteLineContaining(i.trim());
			}
		}
		String importBlock = importFragment.getCode();
		surrounding = surrounding.substring(0, offset) + importBlock + surrounding.substring(offset, surrounding.length());
		return surrounding;
	}
	
	/**
	 * Static function to insert a snippet within a String at the given offset.
	 */
	public static String insert(Snippet snippet, String surrounding, int offset) {
		//process fragments in reverse order
		for(int i=snippet.code.size()-1; i>=0; i--) {
			Fragment fragment = snippet.code.get(i);
			String fragmentString = fragment.getCode();
			
			//add snippet at insert point
			if(fragment.getType() == Fragment.SNIPPET) {
				surrounding = surrounding.substring(0, offset) + fragmentString + surrounding.substring(offset, surrounding.length());
			}
			
			//add imports
			if(fragment.getType() == Fragment.IMPORTS) {
				surrounding = insertImports(fragment, surrounding);
			}
		}
		return surrounding;
	}
	
	
	/**
	 * Static function to insert a snippet within a String at the given offset with formatting information.
	 */
	public static String insertFormatted(Snippet snippet, String surrounding, int offset) {
		for(int i=snippet.code.size()-1; i>=0; i--) {
			Fragment fragment = snippet.code.get(i);
			String fragmentString = fragment.getFormattedCode();
			
			//add snippet at insert point
			if(fragment.getType() == Fragment.SNIPPET) {
				String source = "//https://stackoverflow.com/questions/" + snippet.ID + "\n";
				fragmentString = source + fragmentString;
				surrounding = surrounding.substring(0, offset) + fragmentString + surrounding.substring(offset, surrounding.length());
			}
			
			//add imports
			if(fragment.getType() == Fragment.IMPORTS) {
				surrounding = insertImports(fragment, surrounding);
			}
		}
		return surrounding;
	}

	public List<Diagnostic<? extends JavaFileObject>> getDiagnostics() {
		return diagnostics;
	}

	public List<String> getImportList() {
		List<String> importList = new ArrayList<>();
		Fragment imports = code.get(0);
		for(int i=0; i<imports.getLOC(); i++) {
			importList.add(imports.getLine(i));
		}
		return importList;
	}

	public void addImportStatement(String string) {
		Fragment imports = code.get(0);
		imports.addLine(string);
		changed();
	}

	public void setPassedTests(int i) {
		passedTests = i;
	}

	public void setArguments(List<String> arguments) {
		this.arguments = arguments;
	}
	
	public void setReturn(String returnType) {
		this.returnType = returnType;
	}

	public List<String> getArgumentTypes() {
		return arguments;
	}
	
	public String getReturn() {
		return returnType;
	}

	public void setAST(Node node) {
		ast = node;
	}
	
}
