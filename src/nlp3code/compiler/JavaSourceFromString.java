package nlp3code.compiler;

import java.net.URI;
import javax.tools.SimpleJavaFileObject;

public class JavaSourceFromString extends SimpleJavaFileObject {
	  final String code;
	  private String className;

	  public JavaSourceFromString(String className, String code) {
		  super(URI.create("file:///" + className + Kind.SOURCE.extension),Kind.SOURCE);
		  this.code = code;
		  this.className = className;
	  }
	  
	  public String getClassName() {
		  return className;
	  }

	  @Override
	  public CharSequence getCharContent(boolean ignoreEncodingErrors) {
	    return code;
	  }
}