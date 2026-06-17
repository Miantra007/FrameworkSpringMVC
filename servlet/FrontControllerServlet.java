package servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import util.Utilitaire;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FrontControllerServlet extends HttpServlet {

    List<String> listController = new ArrayList<>();

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

    public void init() {
        String packageName = getServletConfig().getInitParameter("controller-package");

        Utilitaire util = new Utilitaire();
        List<Class<?>> controller = util.recupererClasseController(packageName,
                mg.itu.miantra.annotation.Controller.class);
        for (Class<?> c : controller) {
            listController.add(c.getName());
        }
    }

    private void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        StringBuffer url = request.getRequestURL();
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();
        out.println(url);
        out.println("Classe controller");
        for (String controller : listController) {
            out.println(controller);
        }
    }

}
