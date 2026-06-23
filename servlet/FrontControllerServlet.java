package servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import mg.itu.miantra.annotation.Url;
import util.Utilitaire;

import java.io.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class FrontControllerServlet extends HttpServlet {

    List<Method> methods = new ArrayList<>();

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

        try {

            methods = util.recupererClasseController(packageName,
                    mg.itu.miantra.annotation.Controller.class, mg.itu.miantra.annotation.Url.class);

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

        boolean found = false;

        for (Method m : methods) {
            Url annotation = m.getAnnotation(Url.class);
            String urll = annotation.value();
            if (urll.equals(lastUrl)) {
                found = true;
                Class<?> clazz = m.getDeclaringClass();
                String nomClass = clazz.getName();
                out.println("Classe : " + nomClass);
                out.println("Method : " + m.getName());
                out.println("Url :" + urll);
                break;

            }

        }

        if (!found) {
            out.println("Aucune correspondance trouvée.");
            out.println("Liste des URLs disponibles :");
            out.println(" ");

            for (Method mm : methods) {
                Url annotationn = mm.getAnnotation(Url.class);
                String urlll = annotationn.value();
                Class<?> clazz = mm.getDeclaringClass();
                String nomClass = clazz.getName();
                out.println("Classe : " + nomClass);
                out.println("Method : " + mm.getName());
                out.println("Url :" + urlll);
                out.println("-------------");

            }
        }

    }

}
