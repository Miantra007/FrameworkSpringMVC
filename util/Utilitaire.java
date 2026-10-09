package util;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    public Object convertObject(Class<?> type, String v) {

        if (type == String.class) {
            return v;

        } else if (type == int.class) {
            return Integer.parseInt(v);

        } else if (type == Boolean.class) {
            return Boolean.parseBoolean(v);

        } else if (type == Double.class) {
            return Double.parseDouble(v);
        }
        return null;
    }

    public Object construireObjet(
            Class<?> type,
            String prefixe,
            Map<String, String[]> parametrePage) throws Exception {

        Object object = type.getDeclaredConstructor().newInstance();

        Field[] fields = type.getDeclaredFields();

        for (int i = 0; i < fields.length; i++) {

            Field field = fields[i];

            String nomChamp = field.getName();

            String nomParametre = prefixe + "." + nomChamp;

            if (field.getType() == String.class ||
                    field.getType() == int.class ||
                    field.getType() == boolean.class ||
                    field.getType() == double.class ||
                    field.getType() == Boolean.class ||
                    field.getType() == Double.class) {

                String[] valeurObjet = parametrePage.get(nomParametre);

                if (valeurObjet != null) {

                    String valeur = valeurObjet[0];

                    Object valeurConvertie = convertObject(field.getType(), valeur);

                    field.setAccessible(true);

                    field.set(object, valeurConvertie);
                }

            } else {

                // Vérifier si des paramètres existent pour l'objet enfant
                String prefixeEnfant = nomParametre + ".";

                boolean enfantPresent = false;

                for (String nom : parametrePage.keySet()) {
                    if (nom.startsWith(prefixeEnfant)) {
                        enfantPresent = true;
                        break;
                    }
                }

                // Construire l'enfant uniquement s'il est présent
                if (enfantPresent) {
                    Object objetEnfant = construireObjet(
                            field.getType(),
                            nomParametre,
                            parametrePage);

                    field.setAccessible(true);
                    field.set(object, objetEnfant);
                }
            }
        }

        return object;
    }

}
