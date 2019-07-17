package nlp2code;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.tools.Diagnostic;
import javax.tools.JavaFileManager;

/* Class Tester
 * Handles Testing of code snippets through public function test
 */

class Tester{
	private static String before;
	private static String after;
	private static String snippet;
	private static String className;
	private static String functionName;
	private static IMCompiler compiler;
	
	/*	Function to test a snippet
	 *  Returns the number of passed tests. */
	public static Integer test(String s, String b, String a, List<String> argumentTypes, String returnType) {
		//set code fragments
		snippet = s;
		before = b;
		after = a;
		
		//construct a test function
		String test = constructFunction(returnType, argumentTypes);
		//cannot construct function
		if(test == null) return 0;
		
		System.out.print(test);
		
		//construct file
		String code = constructFile(test);
		//cannot construct file
		if(code == null) return 0;
		
		//try to compile our test
		compiler = new IMCompiler(false, false, false);
		compiler.evaluating = true;
		Integer errors = compiler.compile(code);
		compiler.evaluating = false;
		
		
//		System.out.println(errors);
//		for (Diagnostic diagnostic : compiler.diagnostics.getDiagnostics()) {
//			System.out.print(diagnostic.getMessage(null));
//		}
		
		//if compilation fails, we assume some type mismatch with test and say no passes
		if(errors != 0) {
			return 0;
		}
		
		//attempt to run tests
		return run();
		
	}
	
	private static Integer run() {
		Integer passed = 0;
		Class[] cArgs = new Class[1];
		cArgs[0] = String.class;
		
		//get method
		JavaFileManager fm = compiler.fileManager;
		try {
			Method method = fm.getClassLoader(null).loadClass(compiler.fullName).getDeclaredMethod(functionName, cArgs);
			method.setAccessible(true);
			Integer result = (Integer) method.invoke(null, "1");
			System.out.print(result);
			if(result == 1) {
				return 1;
			}
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return passed;
	}
	
	/* Constructs a function for testing */
	private static String constructFunction(String returnType, List<String> argumentTypes) {
		List<String> arguments;
		String test = "";
		
		//get arguments
		arguments = getArguments(argumentTypes);
		//if we couldn't find all arguments, fail
		if(arguments == null) return null;
		
		//construct signature
		//for now use test but to avoid conflicts check if free
		functionName = "test";
		test += "public static " + returnType + " " + functionName + "(";
		for(int i=0; i<arguments.size(); i++) {
			if(i != 0) test += ", ";
			test += argumentTypes.get(i);
			test += " ";
			test += arguments.get(i);
		}
		test += "){\n";
		
		//construct contents
		addReturn(returnType);
		//if we couldnt add a return, fail
		if(snippet == null) return null;
		
		test += snippet;
		
		test += "}\n";
		
		return test;
	}

	
	/* Builds the class file from before, after and the test function */
	private static String constructFile(String test) {
		String code = "";
		
		//get the current file
		code = before + after;
		
		String[] lines = code.split("\n");
		Integer insertPos = findClass(lines);
		if(insertPos == -1) return null;
		
		//reconstruct
		code = "";
		for(int i=0; i<lines.length; i++) {
			if(i == insertPos) {
				code += test;
			}
			code += lines[i] + "\n";
		}
		
		return code;
	}
	
	/*Finds class in source code, returns number of line after*/
	private static Integer findClass(String[] lines) {
		Integer pos = -1;
		Boolean classStart = false;
		
		for(int i=0; i<lines.length; i++) {
			String line = lines[i].trim();
			if(line.startsWith("class ")) {
				classStart = true;
				className = line.split(" ")[1];
				if(line.endsWith("{")) {
					classStart = false;
					pos = i+1;
					return pos;
				}
			}
			else if(classStart = true) {
				if(line.endsWith("{")) {
					pos = i+1;
					return pos;
				}
			}
		}
		
		return pos;
	}
	
	/*Given a list of argument types, find arguments from snippet
	 * Return null if fails */
	private static List<String> getArguments(List<String> argumentTypes) {
		List<String> arguments = new ArrayList<String>();
		String[] lines = snippet.split("\n");
		
		//for each argument type
		for(String t : argumentTypes) {
			//run through snippet 
			for(int i=0; i<lines.length; i++) {
				String name = parseName(lines[i], t);
				//if returns a name, valid
				if(name != null) {
					//set line to null so no repeats
					lines[i] = null;
					arguments.add(name);
					//stop searching
					break;
				}
			}
		}
		
		//reconstruct snippet without parameters
		snippet = "";
		for(String l : lines) {
			if(l != null) {
				snippet += l + "\n";
			}
		}
		
		//if we couldnt find all arguments, fail
		if(arguments.size() != argumentTypes.size()) return null;
		
		return arguments;
	}
	
	/*Parses a line to find name of variable if declaration of type type*/
	private static String parseName(String line, String type) {
		String name = null;
		Integer start, end;
		String currentName;
		
		//handle comments later!!!!
		
		//must start with type and space
		if(line.startsWith(type + " ")) {
			//name must come after
			start = 0 + (type + " ").length();
			currentName = line.substring(start);
			//if contains an equals
			if(currentName.contains("=")) {
				//chop off anything at this point
				currentName = currentName.substring(0, currentName.indexOf("="));
			}
			//remove any spaces or semicolons
			currentName = currentName.replace(" ", "");
			currentName = currentName.replace(";", "");
			name = currentName;
		}
		
		return name;
	}
	
	private static Boolean isDeclaration(String line, String type) {
		char current = ' ';
		char previous;
		String token = "";
		Integer num = 0;
		
		//states
		Boolean quotes = false;
		Boolean dQuotes = false;
		Boolean comment = false;
		Boolean trailingComment = false;
		
		for(int i=0; i<line.length(); i++) {
			previous = current;
			current = line.charAt(i);
			
			//if non-canceled quote, toggle state
			if(current == '\'' && previous != '\\') {
				if(quotes == false) quotes = true;
				else if(quotes == true) quotes = false;
			}
			
			//if non-canceled double quote, toggle state
			if(current == '\"' && previous != '\\') {
				if(dQuotes == false) dQuotes = true;
				else if(dQuotes == true) dQuotes = false;
			}
			
			//if comment toggle state
			if(quotes == false && dQuotes == false) {
				if(current == '*' && previous == '/') {
					comment = true;
				}
				
				if(current == '/' && previous == '*') {
					comment = false;
				}
				
				//if trailing comment, no toggle
				if(comment == false) {
					if(current == '/' && previous == '/') {
						trailingComment = true;
						//no more important info here
						break;
					}
				}
			}
			
			//if outside these states
			if(quotes == false && comment == false && dQuotes == false) {
				//add to token until we find a whitespace
				if(!Character.isWhitespace(current) && current != ';' && current != '=') {
					token += current;
				}
				//found first whitespace
				else if(Character.isWhitespace(current) && !Character.isWhitespace(previous)) {
					//if we have a token
					if(token != "") {
						//if first token, check type
						if(num == 0) {
							if(token == type) {
								return true;
							}
						}
						
						
						//reset token
						token = "";
						num++;
					}
				}
			}
			
			
			
			
		}
		
		return false;
	}
	
	/* Adds a return statement to the snippet
	 * Bottom-up search for first valid candidate 
	 * Return type for tests is specified but if snippet ends with function
	 * not assignment, we cannot verify the function return type.
	 * Until compile? Could use an error message to inform the return type changes
	 * Quickfix could help? */
	private static void addReturn(String type) {
		String[] lines = snippet.split("\n");
		Boolean found = false;
		
		//bottom up search
		for(int i=lines.length-1; i>=0; i--) {
			String current = lines[i];
			//check if line is valid for return
			String returnLine = parseReturn(current, type);
			if(returnLine != null) {
				//replace line
				lines[i] = returnLine;
				found = true;
				break;
			}
		}
		
		//if we never found a valid return, set snippet to null and end
		if(found == false) {
			snippet = null;
			return;
		}
		
		//reconstruct snippet with changes
		snippet = "";
		for(String l : lines) {
			if(l != null) {
				snippet += l + "\n";
			}
		}
	}
	
	/* Parses a line to determine if it could be a valid return
	 * Returns the line formatted as a return if true.
	 * If false, returns null. */
	private static String parseReturn(String line, String type) {
		String returnLine = null;
		String verify;
		String current = "";
		String name;
		String whitespace = "";
		char previous;
		char c = ' ';
		
		//trim whitespace
		line.trim();
		
		
		
		//find leading whitespace
		for(int i=0; i<line.length(); i++) {
			previous = c;
			c = line.charAt(i);
			
			//check for non-whitespace
			if(c != ' ' && c != '\t') {
				break;
			}
			else {
				whitespace += c;
			}
		}
		
		//chop off
		current = line.substring(whitespace.length(), line.length());
		
		//check if // comment
		if(current.startsWith("//")) return null;
		
		//if a declaration
		name = parseName(current, type);
		if(name != null) {
			returnLine = line + "\n";
			returnLine += "return " + name + ";";
			return returnLine;
		}
		
		if(current.contains("=")) {
			//get the name
			name = current.substring(0, current.indexOf("="));
			returnLine = line + "\n";
			returnLine += "return " + name;
			return returnLine;
		}
		
		//if function
		if(current.contains("(") && current.contains(")")) {
			returnLine = whitespace + "return " + current;
			return returnLine;
		}
		
		return returnLine;
	}
	
	
}