package listener;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import mg.itu.miantra.annotation.Url;
import util.HttpMethod;
import util.Mapping;
import util.UrlMethod;
import util.Utilitaire;

// @WebListener
public class Listener implements ServletContextListener {

    private static final String SPRING_ROOT_CONTEXT_ATTRIBUTE = "org.springframework.web.context.WebApplicationContext.ROOT";

    @Override
    public void contextInitialized(ServletContextEvent sce) {

        ServletContext context = sce.getServletContext();

        String packageName = context.getInitParameter("controller-package");

        HashMap<UrlMethod, Mapping> map = new HashMap<>();

        Utilitaire util = new Utilitaire();

        try {

            List<Class<?>> controllers = util.recupererClasseController(packageName,
                    mg.itu.miantra.annotation.Controller.class);

            for (Class<?> c : controllers) {

                List<Method> methods = util.methodWithAnnotation(c, Url.class);

                for (Method m : methods) {

                    Url annotation = m.getAnnotation(Url.class);

                    UrlMethod urlMethod = new UrlMethod(
                            annotation.value(),
                            HttpMethod.valueOf(annotation.method()));

                    Mapping mapping = new Mapping(c, m);

                    if (map.containsKey(urlMethod)) {
                        throw new RuntimeException(
                                "Mapping déjà existant : "
                                        + urlMethod.getUrl()
                                        + " "
                                        + urlMethod.getHttpMethod());
                    }

                    map.put(urlMethod, mapping);
                }
            }
            Object conteneurSpring = context.getAttribute(SPRING_ROOT_CONTEXT_ATTRIBUTE);
            if (conteneurSpring == null) {
                throw new RuntimeException("Le conteneur spring n'a pas pu etre recuperer au demarrage"
                        + "verifier l'ordre des listners dans le web.xml");
            }

            context.setAttribute("springContext", conteneurSpring);
            context.setAttribute("mapping", map);

        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {

    }
}
