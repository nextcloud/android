package com.owncloud.android.test.ui.groups;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 *
 * Modified version of ClasspathClassesFinder from:
 * http://linsolas.free.fr/wordpress/index.php/2011/02/how-to-categorize-junit-tests-with-maven/
 *
 * The difference is, that it does not search for annotated classes but for classes with a certain
 * class name prefix and suffix.
 */
public final class PatternClasspathClassesFinder {

 /**
  * Get the list of classes of a given package name, and that are annotated
  * by a given annotation.
  *
  * @param packageName
  *            The package name of the classes.
  * @param classPrefix
  *            The prefix of the class name.
  * @param classSuffix
  *            The suffix of the class name.
  * @param methodAnnotation
  *            Only return classes containing methods annotated with methodAnnotation.
  * @return The List of classes that matches the requirements.
  */
 public static Class<?>[] getSuiteClasses(String packageName,
   String classPrefix, String classSuffix,
   Class<? extends Annotation> methodAnnotation) {
  try {
   return getClasses(packageName, classPrefix, classSuffix, methodAnnotation);
  } catch (Exception e) {
   e.printStackTrace();
  }
  return null;
 }

 /**
  * Get the list of classes of a given package name, and that are annotated
  * by a given annotation.
  *
  * @param packageName
  *            The package name of the classes.
  * @param classPrefix
  *            The prefix of the class name.
  * @param classSuffix
  *            The suffix of the class name.
  * @param methodAnnotation
  *            Only return classes containing methods annotated with methodAnnotation.
  * @return The List of classes that matches the requirements.
  * @throws ClassNotFoundException
  *             If something goes wrong...
  * @throws IOException
  *             If something goes wrong...
  */
 private static Class<?>[] getClasses(String packageName,
   String classPrefix, String classSuffix,
   Class<? extends Annotation> methodAnnotation)
   throws ClassNotFoundException, IOException {
  ClassLoader classLoader = Thread.currentThread()
    .getContextClassLoader();
  String path = packageName.replace('.', '/');
  // Get classpath
  Enumeration<URL> resources = classLoader.getResources(path);
  List<File> dirs = new ArrayList<File>();
  while (resources.hasMoreElements()) {
   URL resource = resources.nextElement();
   dirs.add(new File(resource.getFile()));
  }
  // For each classpath, get the classes.
  ArrayList<Class<?>> classes = new ArrayList<Class<?>>();
  for (File directory : dirs) {
   classes.addAll(findClasses(directory, packageName, classPrefix, classSuffix, methodAnnotation));
  }
  return classes.toArray(new Class[classes.size()]);
 }

 /**
  * Find classes, in a given directory (recursively), for a given package
  * name, that are annotated by a given annotation.
  *
  * @param directory
  *            The directory where to look for.
  * @param packageName
  *            The package name of the classes.
  * @param classPrefix
  *            The prefix of the class name.
  * @param classSuffix
  *            The suffix of the class name.
  * @param methodAnnotation
  *            Only return classes containing methods annotated with methodAnnotation.
  * @return The List of classes that matches the requirements.
  * @throws ClassNotFoundException
  *             If something goes wrong...
  */
 private static List<Class<?>> findClasses(File directory,
   String packageName, String classPrefix, String classSuffix,
   Class<? extends Annotation> methodAnnotation)
   throws ClassNotFoundException {
  List<Class<?>> classes = new ArrayList<Class<?>>();
  if (!directory.exists()) {
   return classes;
  }
  File[] files = directory.listFiles();
  for (File file : files) {
   if (file.isDirectory()) {
    classes.addAll(findClasses(file,
      packageName + "." + file.getName(), classPrefix, classSuffix, methodAnnotation));
   } else if (file.getName().startsWith(classPrefix) && file.getName().endsWith(classSuffix + ".class")) {
    // We remove the .class at the end of the filename to get the
    // class name...
    Class<?> clazz = Class.forName(packageName
      + '.'
      + file.getName().substring(0,
        file.getName().length() - 6));

    // Check, if class contains test methods (prevent "No runnable methods" exception):
    boolean classHasTest = false;
    for (Method method : clazz.getMethods()) {
     if (method.getAnnotation(methodAnnotation) != null) {
      classHasTest = true;
      break;
     }
    }
    if (classHasTest) {
     classes.add(clazz);
    }
   }
  }
  return classes;
 }
}