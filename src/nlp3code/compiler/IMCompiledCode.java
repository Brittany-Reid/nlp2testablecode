package nlp3code.compiler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

import javax.tools.SimpleJavaFileObject;

/**
 *   In memory compiled code file.
 */
public class IMCompiledCode extends SimpleJavaFileObject implements java.io.Serializable {
	
	private static final long serialVersionUID = 1805236520776498033L;
	protected final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    private String className;

    public IMCompiledCode(String className, Kind kind) {
        super(URI.create("string:///" + className.replace('.', '/') + kind.extension), kind);
        this.className = className;
    }
    
    public String getClassName() {
    	return className;
    }

    public byte[] getBytes() {
        return bos.toByteArray();
    }

    @Override
    public OutputStream openOutputStream() throws IOException {
        return bos;
    }
}