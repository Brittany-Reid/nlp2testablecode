package nlp2code.fixer;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import nlp2code.Snippet;

/**
 * This class contains code fixes for parsing errors.
 */
public class ParsingFixes {
	
	/**
	 * Handles missing semi-colon fix. Returns null on error.
	 * @param snippet The snippet to fix.
	 * @param diagnostic The diagnostic containing error information.
	 * @param offset The offset value calculated from surrounding code.
	 * @return A fixed snippet, or on error, null.
	 */
	public static Snippet missingSemiColon(Snippet snippet, Diagnostic<? extends JavaFileObject> diagnostic, int offset) {
		String code = Fixer.insertAt(snippet.getCode(), ";", diagnostic.getStartPosition(), diagnostic.getEndPosition(), offset);
		if(code == null) return null;
		
		snippet.setCode(code);
		
		return snippet;
	}
}
