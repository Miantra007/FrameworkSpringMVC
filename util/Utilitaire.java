package util;

import java.io.File;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Utilitaire {

    public List<Class<?>> recupererClasseController(String packageName, Class<? extends Annotation> annotation)
            throws Exception {
        List<Class<?>> resultats = new ArrayList<>();

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String path = packageName.replace('.', '/');
        URL ressources = classLoader.getResource(path);

        if (ressources == null) {
            throw new Exception("Package introuvable : " + packageName);
        }
        File directory = new File(ressources.toURI());
        File[] files = directory.listFiles();

        if (files == null) {
            return resultats;
        }
        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".class")) {

                String className = packageName + "."
                        + file.getName().replace(".class", "");

                Class<?> clazz = Class.forName(className);

                if (clazz.isAnnotationPresent(annotation)) {
                    resultats.add(clazz);
                }
            }
        }

        return resultats;
    }

}
