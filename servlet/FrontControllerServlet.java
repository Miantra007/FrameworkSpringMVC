package servlet;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import mg.itu.miantra.annotation.WebApi;
import util.HttpMethod;
import util.Mapping;
import util.UrlMethod;
import util.Utilitaire;
import util.ModelAndView;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.springframework.context.ApplicationContext;

import java.lang.reflect.Parameter;

public class FrontControllerServlet extends HttpServlet {

    HashMap<UrlMethod, Mapping> map = new HashMap<>();
    String prefix;
    String suffix;
    ApplicationContext springContext;
    Utilitaire util = new Utilitaire();

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
        springContext = (ApplicationContext) getServletContext().getAttribute("springContext");

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

                if (met.getParameterCount() == 0) {
                    result = met.invoke(controller);
                } else {

                    Parameter[] parametres = met.getParameters();
                    Map<String, String[]> parametrePage = request.getParameterMap();
                    Object[] valeurs = new Object[parametres.length];

                    for (int i = 0; i < parametres.length; i++) {
                        Parameter p = parametres[i];
                        Class<?> type = p.getType();

                        if (type == ApplicationContext.class) {
                            valeurs[i] = springContext;

                        } else if (type == String.class ||
                                type == int.class ||
                                type == Boolean.class ||
                                type == Double.class) {

                            String[] valeurParametre = parametrePage.get(p.getName());

                            if (valeurParametre != null) {

                                String valeur = valeurParametre[0];
                                valeurs[i] = util.convertObject(p.getType(), valeur);
                            }
                        } else {
                            String prefix = type.getSimpleName().toLowerCase();
                            valeurs[i] = util.construireObjet(type, prefix, parametrePage);
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

        } else

        {
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
