package util;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Utilitaire {

    public List<Class<?>> recupererClasseController(String packageName,
            Class<? extends Annotation> controller)
            throws Exception {

        List<Class<?>> ctrll = new ArrayList<>();

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
                    ctrll.add(clazz);
                }
            }
        }
        return ctrll;
    }

    public List<Method> methodWithAnnotation(Class<?> clazz, Class<? extends Annotation> url) {
        List<Method> methods = new ArrayList<>();
        Method[] meths = clazz.getDeclaredMethods();
        for (Method m : meths) {
            if (m.isAnnotationPresent(url)) {
                methods.add(m);
            }
        }
        return methods;

    }

}
