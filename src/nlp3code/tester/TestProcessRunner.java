package nlp3code.tester;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.concurrent.TimeoutException;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;

import nlp3code.Evaluator;


/**
 * This class constructs the new process in TestProcess.
 */
public class TestProcessRunner {
	private static final int GRANULARITY = 50; //ms
	public static String JAVA_CMD = System.getenv("JAVA_CMD");
	public static String CLASSPATH = System.getenv("CLASSPATH");
	
	
	/**
	 * Running this function will create a new process to test code inside, by running TestProcess.main.
	 * TODO: Make sure we can report any errors correctly :)
	 */
	public static int exec(int timeout, String methodName, String className, String code) throws Throwable {
		String javaCommand = JAVA_CMD;
		if(javaCommand == null) javaCommand = "java";
		String classpath = Tester.getClassPath();
		ProcessBuilder builder = new ProcessBuilder();
		builder.directory(Paths.get(FileLocator.resolve(FileLocator.find(Platform.getBundle("nlp3code"), new Path("bin/"), null)).toURI()).toFile());
		builder.command(javaCommand, "-cp", "\".;" + classpath + "\"", "nlp3code.tester.testprocess.TestProcess");
		//System.out.println(classpath);
		Process child = builder.start();
		
		try {
			// first, send over the method
			OutputStream output = child.getOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(output);
			oos.writeObject(methodName);
			oos.writeObject(className);
			oos.writeObject(classpath);
			oos.writeObject(code);
			oos.flush();
			
			// second, read the result whilst checking for a timeout
			InputStream input = child.getInputStream();
			ObjectInputStream ois = new ObjectInputStream(input);
			int total = 0;
			while(total < timeout && input.available() == 0) {
				try {
					Thread.sleep(GRANULARITY);
				} catch(InterruptedException e) {				
				}
				total = total + GRANULARITY;
			}

			//return result
			if(input.available() != 0) {
				int result = ois.readInt();
				//System.out.println(result);
				
				//handle error, get string report
				if(result == -1) {
					System.out.println((String)ois.readObject());
					return 0;
				}
				
				//otherwise, return the result
				return result;						
			} else {				
//				throw new TimeoutException("timed out after " + timeout + "ms");
				System.out.println("Timeout!");
				return 0;
			}
		}
		finally {
			child.destroy();
		}
	}
}
