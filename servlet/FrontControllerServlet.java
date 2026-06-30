package servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import mg.itu.miantra.annotation.Url;
import util.Mapping;
import util.Utilitaire;

import java.io.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FrontControllerServlet extends HttpServlet {

    HashMap<String, Mapping> map = new HashMap<>();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    public void init() throws ServletException {

        String packageName = getServletConfig().getInitParameter("controller-package");

        Utilitaire util = new Utilitaire();
        List<Class<?>> controller = new ArrayList<>();

        try {

            controller = util.recupererClasseController(packageName,
                    mg.itu.miantra.annotation.Controller.class);
            for (Class<?> c : controller) {
                List<Method> methods = util.methodWithAnnotation(c, mg.itu.miantra.annotation.Url.class);
                for (Method m : methods) {
                    Url annotation = m.getAnnotation(Url.class);
                    String url = annotation.value();
                    Mapping mapping = new Mapping(c, m);
                    map.put(url, mapping);
                }
            }

        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();
        String uri = request.getRequestURI();
        String context = request.getContextPath();
        String lastUrl = uri.substring(context.length());

        if (lastUrl == null) {
            out.println("Erreur : request.getPathInfo() retourne null. Verifie le mapping du servlet.");
            return;
        }
        if (map.containsKey(lastUrl)) {
            Mapping mapping = map.get(lastUrl);
            out.println("Url : " + lastUrl);
            out.println("Class : " + mapping.getClazz().getName());
            out.println("Methode : " + mapping.getMethod().getName());
            out.println("-------------------------------");

        } else {
            for (Map.Entry<String, Mapping> entry : map.entrySet()) {
                String url = entry.getKey();
                Mapping mp = entry.getValue();
                out.println("Url : " + url);
                out.println("Class : " + mp.getClazz().getName());
                out.println("Methode : " + mp.getMethod().getName());
                out.println("-------------------------------");

            }

        }

    }

}
