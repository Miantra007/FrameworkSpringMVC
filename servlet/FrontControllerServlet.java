package servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import java.io.*;

public class FrontControllerServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        recupererUrl(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        recupererUrl(request, response);
    }

    private void recupererUrl(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        StringBuffer url = request.getRequestURL();
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();
        out.println(url);
    }

}
