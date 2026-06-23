package util;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Utilitaire {

    public List<Method> recupererClasseController(String packageName,
            Class<? extends Annotation> controller,
            Class<? extends Annotation> url)
            throws Exception {

        List<Method> met = new ArrayList<>();

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String path = packageName.replace('.', '/');
        URL ressources = classLoader.getResource(path);

        if (ressources == null) {
            throw new Exception("Package introuvable : " + packageName);
        }
        File directory = new File(ressources.toURI());
        File[] files = directory.listFiles();

        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".class")) {

                String className = packageName + "." + file.getName().replace(".class", "");

                Class<?> clazz = Class.forName(className);

                if (clazz.isAnnotationPresent(controller)) {
                    Method[] methods = clazz.getDeclaredMethods();
                    for (Method m : methods) {
                        if (m.isAnnotationPresent(url)) {
                            met.add(m);
                        }
                    }
                }
            }
        }

        return met;
    }

}
