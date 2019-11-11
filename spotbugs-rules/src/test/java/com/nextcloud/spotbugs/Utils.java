package com.nextcloud.spotbugs;

import edu.umd.cs.findbugs.test.SpotBugsRule;

import java.nio.file.Path;
import java.nio.file.Paths;

public interface Utils {

    static void addAux(SpotBugsRule spotbugs) {
        for (String classPath : System.getProperty("java.class.path").split(":")) {
            Path p = Path.of(classPath);
            spotbugs.addAuxClasspathEntry(p);
        }
    }

    static Path get(Class c) {
        String p = c.getName().replace(".", "/") + ".class";
        return Paths.get("target/test-classes", p);
    }
}
