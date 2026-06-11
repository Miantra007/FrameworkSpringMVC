package servlet;

import jakarta.servlet.http.*;
import java.io.*;
import java.rmi.ServerException;

public class FrontControllerServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServerException, IOException {
        recupererUrl(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServerException, IOException {
        recupererUrl(request, response);
    }

    private void recupererUrl(HttpServletRequest request, HttpServletResponse response)
            throws ServerException, IOException {

        StringBuffer url = request.getRequestURL();
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();
        out.println(url);
    }

}
