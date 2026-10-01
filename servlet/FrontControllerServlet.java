package servlet;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import mg.itu.miantra.annotation.WebApi;
import util.HttpMethod;
import util.Mapping;
import util.UrlMethod;
import util.ModelAndView;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.lang.reflect.Parameter;

public class FrontControllerServlet extends HttpServlet {

    HashMap<UrlMethod, Mapping> map = new HashMap<>();
    String prefix;
    String suffix;
    Object springContext;

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
        springContext = getServletContext().getAttribute("springContext");

        this.prefix = getServletContext().getInitParameter("prefix");
        this.suffix = getServletContext().getInitParameter("suffix");
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
                Object result = null;
                boolean parametreValide = false;

                if (met.getParameterCount() == 0) {
                    result = met.invoke(controller);

                } else if (met.getParameterCount() == 1) {
                    Class<?> paramType = met.getParameterTypes()[0];
                    if (paramType == Object.class) {
                        result = met.invoke(controller, springContext);
                    } else {
                        parametreValide = true;
                    }
                } else {
                    parametreValide = true;
                }

                if (parametreValide) {
                    Parameter[] parametres = met.getParameters();
                    Map<String, String[]> parametrePage = request.getParameterMap();
                    Object[] valeurs = new Object[parametres.length];

                    for (Map.Entry<String, String[]> entry : parametrePage.entrySet()) {

                        for (int i = 0; i < parametres.length; i++) {

                            Parameter p = parametres[i];

                            if (p.getName().equals(entry.getKey())) {
                                String valeur = entry.getValue()[0];

                                if (p.getType() == String.class) {
                                    valeurs[i] = valeur;
                                } else if (p.getType() == int.class) {
                                    valeurs[i] = Integer.parseInt(valeur);
                                }
                            }
                        }
                    }
                    result = met.invoke(controller, valeurs);

                }
                boolean isWebAPI = met.isAnnotationPresent(WebApi.class);

                if (isWebAPI) {
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    PrintWriter pw = response.getWriter();

                    if (result instanceof String str) {
                        response.setContentType("text/plain");
                        response.setCharacterEncoding("UTF-8");
                        pw.print(str);
                    } else {
                        ObjectMapper obMap = new ObjectMapper();
                        String json = obMap.writeValueAsString(result);
                        pw.println(json);
                    }
                    return;
                }

                if (result instanceof ModelAndView mv) {

                    for (Map.Entry<String, Object> entry : mv.getModel().entrySet()) {
                        request.setAttribute(entry.getKey(), entry.getValue());
                    }

                    String view = prefix + "/" + mv.getView() + suffix;

                    request.getRequestDispatcher(view).forward(request, response);
                    return;
                }

                if (result instanceof String str) {
                    response.getWriter().println(str);
                    return;
                }

            } catch (Exception e) {
                e.printStackTrace();
                throw new ServletException(e);
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
