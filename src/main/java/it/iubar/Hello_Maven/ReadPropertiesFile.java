package it.iubar.Hello_Maven;


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;


public class ReadPropertiesFile {

	    /* Create basic object */
	    ClassLoader objClassLoader = null;
	    Properties commonProperties = new Properties();
	    
	    public ReadPropertiesFile() {
	        /* Initialize 'objClassLoader' once so same object used for multiple files. */
	        objClassLoader = getClass().getClassLoader();
	    }
	    
	    public String readKey(String propertiesFilename, String key){
	        if (propertiesFilename != null && !propertiesFilename.trim().isEmpty()
	                && key != null && !key.trim().isEmpty()) {
	            /* try-with-resource in JDK 1.7 or above */
	            try(
	                    FileInputStream objFileInputStream = new FileInputStream(objClassLoader.getResource(propertiesFilename).getFile());
	               ){
	                /* Load file into commonProperties */
	                commonProperties.load(objFileInputStream);
	                /* Get the value of key */
	                return String.valueOf(commonProperties.get(key));
	            } catch (FileNotFoundException ex) {
	                ex.printStackTrace();
	            } catch (IOException ex) {
	                ex.printStackTrace();
	            }
	        }
	        return null;
	    }
}
