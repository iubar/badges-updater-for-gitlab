package it.iubar.SonarQube_badges_into_GitLab;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

public class ReadPropertiesFile {

    /* Create basic object */
    ClassLoader objClassLoader = null;
    Properties commonProperties = new Properties();
    
    public ReadPropertiesFile() {
        /* Initialize 'objClassLoader' once so same object used for multiple files. */
        objClassLoader = getClass().getClassLoader();
    }
    
    public  String readKey(String propertiesFilename, String key){    	
    	
        // Simple validation
        if (propertiesFilename != null && !propertiesFilename.trim().isEmpty()
                && key != null && !key.trim().isEmpty()) {

            FileInputStream objFileInputStream = null;
            
           
            try {
                // Read file from resources folder 
            	String fileName = objClassLoader.getResource(propertiesFilename).getFile();
                objFileInputStream = new FileInputStream(fileName);
                
                //Metodo 1 (OK - Dalla cartella resource)
//                ClassLoader loader = Thread.currentThread().getContextClassLoader();
//                InputStream objInputStream = loader.getResourceAsStream(propertiesFilename);
        
                //Metodo 2 (OK - Dalla cartella resource)
//                objFileInputStream = new FileInputStream("src/main/resources/config.properties");
             
                //Metodo 3 (OK - Dalla cartella resource)
                InputStream objInputStream = ClassLoader.getSystemResourceAsStream(propertiesFilename);

                // Load file into commonProperties
                commonProperties.load(objInputStream);
                
                //System.out.print(commonProperties.toString());
                //System.exit(0);
                
                // Get the value of key
                return String.valueOf(commonProperties.get(key));
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            } catch (IOException ex) {
                ex.printStackTrace();
            }finally{
                // Close the resource
                if(objFileInputStream != null){
                    try {
                        objFileInputStream.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        } 
        
        return null; 
    }
    
    public static void main(String[] args) {
       
    }
}