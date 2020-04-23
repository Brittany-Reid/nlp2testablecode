package nlp2testablecode.compiler;

import java.security.SecureClassLoader;
import java.util.HashMap;
import java.util.Map;

/**
 * Class IMClassLoader
 * 	 ClassLoader for our in memory classes.
 */
public class IMClassLoader extends SecureClassLoader{
	private Map<String, IMCompiledCode> classes = new HashMap<String, IMCompiledCode>();
	
	/**
	 * Default constructor, uses SystemClassLoader as parent.
	 */
	IMClassLoader(){
		super(ClassLoader.getSystemClassLoader());
	}
	
	/**
	 * Constructor, using the specified parent.
	 */
	IMClassLoader(ClassLoader parent){
		super(parent);
	}
	
	/**
	 * Internal: Adds compiled code to the class map for lookup.
	 */
	public void addCode(IMCompiledCode code) {
		classes.put(code.getClassName(), code);
	}
	
	/**
	 * Internal: Overrides the findClass function of classLoader.
	 */
	@Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
		IMCompiledCode code = classes.get(name);
		if(code == null) {
			return super.findClass(name);
		}
		byte[] byteCode = code.getBytes();
		return defineClass(name, byteCode, 0, byteCode.length);
	}
	
	/**
	 * Get the raw class file. 
	 */
	public IMCompiledCode getCompiled(String name) {
		IMCompiledCode code = classes.get(name);
		return code;
	}
	
}