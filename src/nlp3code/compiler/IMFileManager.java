package nlp3code.compiler;

import java.io.IOException;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;

public class IMFileManager extends ForwardingJavaFileManager<JavaFileManager> {
	//private List<IMCompiledCode> classes = new ArrayList<IMCompiledCode>();
    private IMClassLoader classLoader;
    
    public IMFileManager(StandardJavaFileManager standardManager, IMClassLoader classLoader) {
        super(standardManager);
        this.classLoader = classLoader;
    }

    public IMFileManager(StandardJavaFileManager standardManager) {
        super(standardManager);
        classLoader = new IMClassLoader();
    }

    @Override
    public ClassLoader getClassLoader(Location location) {
        return classLoader;
    }
    
    public void setClassLoader(IMClassLoader classLoader) {
    	this.classLoader = classLoader;
    }

    public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) throws IOException {
        IMCompiledCode code = new IMCompiledCode(className, kind);
        //classes.add(code);
        classLoader.addCode(code);
        return code;
    }
}