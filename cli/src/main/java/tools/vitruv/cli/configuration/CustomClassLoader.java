package tools.vitruv.cli.configuration;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

/**
 * The CustomClassLoader class is used to load classes within a JAR file from a custom classpath. 
 */
public class CustomClassLoader extends URLClassLoader {
  private static final Logger logger = Logger.getLogger(CustomClassLoader.class.getName());

  /**
   * The constructor of the CustomClassLoader class.
   *
   * @param urls The URLs of the classpath.
   * @param parent The parent class loader.
   */
  public CustomClassLoader(URL[] urls, ClassLoader parent) {
    super(urls, parent);
  }

  /**
   * Adds a JAR to a class path.
   *
   * @param jarPath The path of the JAR file that should be added to the class path.
   */
  public void addJarToClassPath(String jarPath) {
    try {
      URL jarUrl = new URL("file:///" + jarPath);
      this.addURL(jarUrl);
    } catch (Exception e) {
      e.printStackTrace();
    }

    try {
      // Open the JAR file
      JarFile jarFile = new JarFile(new File(jarPath));

      // Get the entries in the JAR file
      Enumeration<JarEntry> entries = jarFile.entries();

      // Iterate through the entries
      while (entries.hasMoreElements()) {
        JarEntry entry = entries.nextElement();

        // Check if the entry is a class file
        if (entry.getName().endsWith(".class")) {
          // Print the class name
          String className = entry.getName().replace("/", ".").replace(".class", "");
          logger.info(className);
        }
      }

      // Close the JAR file
      jarFile.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
