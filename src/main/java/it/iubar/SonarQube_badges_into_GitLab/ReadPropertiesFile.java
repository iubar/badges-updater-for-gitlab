package it.iubar.SonarQube_badges_into_GitLab;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

public class ReadPropertiesFile {

    /* Create basic object */
 
    Properties commonProperties = new Properties();
    
    public ReadPropertiesFile() {
        super();
    }
    
    /**
     * 
     * @see https://stackoverflow.com/questions/7615040/difference-between-classloader-getsystemresourceasstream-and-getclass-getresou
     * 
     * @param propertiesFilename nome del file di configurazione
     * @param key la chiave del parametro da leggere
     * @return String il valore della corrispondente chiave
     */
    public  String readKey(String propertiesFilename, String key){    	
    	
        // Simple validation
        if (propertiesFilename != null && !propertiesFilename.trim().isEmpty()
                && key != null && !key.trim().isEmpty()) {

            FileInputStream objFileInputStream = null;
            InputStream objInputStream = null;
           
            try {
                // Metodo 0
            	// String fileName = getClass().getClassLoader().getResource(propertiesFilename).getFile();
                // objFileInputStream = new FileInputStream(fileName);
                
                // Metodo 1 (OK - Dalla cartella resource)
                // ClassLoader loader = Thread.currentThread().getContextClassLoader();
                // objInputStream = loader.getResourceAsStream(propertiesFilename);
        
                // Metodo 2 (OK - Dalla cartella resource)
                // objFileInputStream = new FileInputStream(propertiesFilename);
             
                // Metodo 3 (OK - Dalla cartella resource)
                // objInputStream = ClassLoader.getSystemResourceAsStream(propertiesFilename);

                // Metodo 4
                // URL url = ClassLoader.getSystemResource("/" . propertiesFilename);
                // objInputStream = url.openStream());

                // Metodo 5 (leggo dalla cartella corrente, ovverto "target" a runtime)
                // File propertiesFile = new File(propertiesFilename);
                // if (!propertiesFile.exists()) {
                //    System.out.println("ERROR: properties file " + propertiesFilename + " was not found !");
                // } else {
                //     objFileInputStream = new FileInputStream(propertiesFile);
                // }
                
                // Load file into commonProperties
                commonProperties.load(objInputStream);
                
                System.out.print(commonProperties.toString());
                System.exit(0);
                
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
