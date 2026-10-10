package util;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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

    public boolean estTypeSimple(Class<?> type) {

        return type == String.class
                || type == int.class
                || type == Integer.class
                || type == double.class
                || type == Double.class
                || type == float.class
                || type == Float.class
                || type == long.class
                || type == Long.class
                || type == byte.class
                || type == Byte.class
                || type == boolean.class
                || type == Boolean.class
                || type == char.class
                || type == Character.class
                || type == LocalDate.class;
    }

    public Object convertObject(Class<?> type, String valeur) {

        if (valeur == null) {
            return null;
        }

        if (type == String.class) {
            return valeur;
        }

        if (type == int.class || type == Integer.class) {
            return Integer.parseInt(valeur);
        }

        if (type == double.class || type == Double.class) {
            return Double.parseDouble(valeur);
        }

        if (type == float.class || type == Float.class) {
            return Float.parseFloat(valeur);
        }

        if (type == long.class || type == Long.class) {
            return Long.parseLong(valeur);
        }
        if (type == byte.class || type == Byte.class) {
            return Byte.parseByte(valeur);
        }

        if (type == boolean.class || type == Boolean.class) {
            return Boolean.parseBoolean(valeur);
        }

        if (type == char.class || type == Character.class) {
            if (valeur.isEmpty()) {
                throw new IllegalArgumentException(
                        "La valeur ne peut pas être vide pour un caractère.");
            }

            return valeur.charAt(0);
        }

        if (type == LocalDate.class) {
            return LocalDate.parse(valeur);
        }

        return null;
    }

    public List<Object> construireListeObjets(
            Class<?> typeElement,
            String prefixe,
            Map<String, String[]> parametrePage) throws Exception {

        List<Object> liste = new ArrayList<>();
        Set<Integer> indices = new TreeSet<>();

        for (String nom : parametrePage.keySet()) {

            if (nom.startsWith(prefixe + "[")) {

                int debut = prefixe.length() + 1;
                int fin = nom.indexOf("]", debut);

                if (fin != -1) {

                    String texteIndice = nom.substring(debut, fin);

                    try {
                        int indice = Integer.parseInt(texteIndice);
                        indices.add(indice);

                    } catch (NumberFormatException e) {

                    }
                }
            }
        }
        for (int indice : indices) {

            String prefixeObjet = prefixe + "[" + indice + "]";

            Object objet = construireObjet(
                    typeElement,
                    prefixeObjet,
                    parametrePage);

            liste.add(objet);
        }
        return liste;
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
            Class<?> typeChamp = field.getType();

            if (estTypeSimple(typeChamp)) {

                String[] valeurObjet = parametrePage.get(nomParametre);

                if (valeurObjet != null) {

                    String valeur = valeurObjet[0];
                    Object valeurConvertie = convertObject(field.getType(), valeur);

                    field.setAccessible(true);
                    field.set(object, valeurConvertie);
                }

            } else if (List.class.isAssignableFrom(typeChamp)) {

                Type typeGenerique = field.getGenericType();

                if (typeGenerique instanceof ParameterizedType) {

                    ParameterizedType typeParametre = (ParameterizedType) typeGenerique;

                    Type typeElement = typeParametre.getActualTypeArguments()[0];

                    if (typeElement instanceof Class<?>) {

                        Class<?> classeElement = (Class<?>) typeElement;
                        if (classeElement == String.class) {

                            String[] valeurs = parametrePage.get(nomParametre);

                            if (valeurs != null) {

                                List<String> liste = new ArrayList<>();

                                for (String valeur : valeurs) {
                                    liste.add(valeur);
                                }

                                field.setAccessible(true);
                                field.set(object, liste);
                            }
                        } else {

                            List<Object> liste = construireListeObjets(
                                    classeElement,
                                    nomParametre,
                                    parametrePage);

                            field.setAccessible(true);
                            field.set(object, liste);
                        }
                    }
                }
            } else {

                String prefixeEnfant = nomParametre + ".";
                boolean enfantPresent = false;

                for (String nom : parametrePage.keySet()) {
                    if (nom.startsWith(prefixeEnfant)) {
                        enfantPresent = true;
                        break;
                    }
                }
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
