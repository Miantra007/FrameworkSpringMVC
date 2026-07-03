package servlet;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import util.HttpMethod;
import util.Mapping;
import util.UrlMethod;

import java.io.*;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class FrontControllerServlet extends HttpServlet {

    HashMap<UrlMethod, Mapping> map = new HashMap<>();

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
    @SuppressWarnings("unchecked")
    public void init() throws ServletException {

        ServletContext context = getServletContext();
        map = (HashMap<UrlMethod, Mapping>) context.getAttribute("mapping");
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
        HttpMethod httpMethod = HttpMethod.valueOf(request.getMethod().toUpperCase());
        UrlMethod urlMethod = new UrlMethod(lastUrl, httpMethod);

        if (map.containsKey(urlMethod)) {
            Mapping mapping = map.get(urlMethod);
            try {
                Method met = mapping.getMethod();
                Object controller = mapping.getClazz().getDeclaredConstructor().newInstance();
                Object result = met.invoke(controller);

            } catch (Exception e) {
                e.printStackTrace();
            }
            out.println("Url : " + urlMethod.getUrl() + " - " + urlMethod.getHttpMethod());
            out.println("Class : " + mapping.getClazz().getName());
            out.println("Methode : " + mapping.getMethod().getName());
            out.println("-------------------------------");

        } else {
            for (Map.Entry<UrlMethod, Mapping> entry : map.entrySet()) {
                UrlMethod url = entry.getKey();
                Mapping mp = entry.getValue();
                out.println("Url : " + url.getUrl() + " - " + url.getHttpMethod());
                out.println("Class : " + mp.getClazz().getName());
                out.println("Methode : " + mp.getMethod().getName());
                out.println("-------------------------------");

            }

        }

    }

}
