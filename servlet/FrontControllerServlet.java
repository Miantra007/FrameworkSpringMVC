package servlet;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.*;
import mg.itu.miantra.annotation.Auth;
import mg.itu.miantra.annotation.WebApi;
import util.HttpMethod;
import util.MapFile;
import util.Mapping;
import util.UrlMethod;
import util.Utilitaire;
import util.ModelAndView;
import util.Session;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationContext;

import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

@MultipartConfig
public class FrontControllerServlet extends HttpServlet {

    HashMap<UrlMethod, Mapping> map = new HashMap<>();
    String prefix;
    String suffix;
    ApplicationContext springContext;
    Utilitaire util = new Utilitaire();
    String authSession;
    String authLogin;
    String role;

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

        this.authSession = getInitParameter("auth-session");
        this.authLogin = getInitParameter("auth-login");
        this.role = getInitParameter("role-session");

        context.setAttribute("authSession", authSession);
        context.setAttribute("authLogin", authLogin);
        context.setAttribute("role", role);

    }

    @SuppressWarnings("unchecked")
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
                Class<?> clazz = mapping.getClazz();
                Map<Class<?>, Object> controllers = (Map<Class<?>, Object>) getServletContext()
                        .getAttribute("controllerInstance");

                Object controller = controllers.computeIfAbsent(clazz, c -> {
                    try {
                        System.out.println("Création du contrôleur : " + c.getName());
                        return c.getDeclaredConstructor().newInstance();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
                System.out.println("Contrôleur utilisé : " + clazz.getName());
                System.out.println("Identité de l'instance : "
                        + System.identityHashCode(controller));

                if (met.isAnnotationPresent(Auth.class)) {

                    String nomAuth = (String) getServletContext().getAttribute("authSession");

                    String urlLogin = (String) getServletContext().getAttribute("authLogin");
                    String rl = (String) getServletContext().getAttribute("role");

                    HttpSession httpSession = request.getSession(false);
                    boolean authentifie = false;

                    if (httpSession != null) {
                        Object valeur = httpSession.getAttribute(nomAuth);
                        authentifie = Boolean.TRUE.equals(valeur);
                    }

                    if (!authentifie) {
                        response.sendRedirect(request.getContextPath() + urlLogin);
                        return;
                    }

                    Auth auth = met.getAnnotation(Auth.class);
                    String[] rolesAutorises = auth.roles();

                    if (rolesAutorises.length > 0) {

                        Object roleSession = httpSession.getAttribute(rl);
                        boolean roleAutorise = false;

                        if (roleSession instanceof String) {

                            String roleUtilisateur = (String) roleSession;
                            for (String role : rolesAutorises) {
                                if (role.equals(roleUtilisateur)) {
                                    roleAutorise = true;
                                    break;
                                }
                            }
                        }
                        if (!roleAutorise) {
                            if (!roleAutorise) {
                                response.sendError(HttpServletResponse.SC_FORBIDDEN,
                                        "Vous n'avez pas le droit d'accéder à cette page.");
                                return;
                            }
                            return;
                        }
                    }
                }

                Object result = null;

                if (met.getParameterCount() == 0) {
                    result = met.invoke(controller);
                } else {

                    Parameter[] parametres = met.getParameters();
                    Map<String, String[]> parametrePage = request.getParameterMap();
                    Object[] valeurs = new Object[parametres.length];

                    Map<String, Part> fichiers = new HashMap<String, Part>();

                    if (request.getContentType() != null
                            && request.getContentType().toLowerCase().startsWith("multipart/form-data")) {

                        for (Part part : request.getParts()) {

                            if (part.getSubmittedFileName() != null
                                    && !part.getSubmittedFileName().isEmpty()) {

                                fichiers.put(part.getName(), part);
                            }
                        }
                    }

                    for (int i = 0; i < parametres.length; i++) {
                        Parameter p = parametres[i];
                        Class<?> type = p.getType();
                        Session session = null;

                        if (type == ApplicationContext.class) {
                            valeurs[i] = springContext;

                        } else if (type == Session.class) {
                            HttpSession httpSession = request.getSession();
                            Map<String, Session> sessions = (Map<String, Session>) getServletContext()
                                    .getAttribute("sessions");

                            if (!sessions.containsKey(httpSession.getId())) {
                                sessions.put(httpSession.getId(), new Session(httpSession));
                            }
                            session = sessions.get(httpSession.getId());
                            valeurs[i] = session;

                        } else if (util.estTypeSimple(type)) {

                            String[] valeurParametre = parametrePage.get(p.getName());

                            if (valeurParametre != null) {

                                String valeur = valeurParametre[0];
                                valeurs[i] = util.convertObject(type, valeur);
                            }
                        } else if (type == MapFile.class) {
                            String nomParametre = p.getName();
                            Part fichier = fichiers.get(nomParametre);

                            if (fichier != null
                                    && fichier.getSubmittedFileName() != null
                                    && !fichier.getSubmittedFileName().isEmpty()) {

                                valeurs[i] = util.construireMapFile(fichier);

                            } else {

                                valeurs[i] = null;
                            }

                        } else if (List.class.isAssignableFrom(type)) {

                            Type typeGenerique = p.getParameterizedType();

                            if (typeGenerique instanceof ParameterizedType) {

                                ParameterizedType typeParametre = (ParameterizedType) typeGenerique;

                                Type typeElement = typeParametre.getActualTypeArguments()[0];

                                if (typeElement instanceof Class<?>) {

                                    Class<?> classeElement = (Class<?>) typeElement;

                                    String prefix = classeElement.getSimpleName().toLowerCase();
                                    valeurs[i] = util.construireListeObjets(
                                            classeElement,
                                            prefix,
                                            parametrePage, fichiers);
                                }

                            }
                        } else {

                            String prefix = type.getSimpleName().toLowerCase();
                            valeurs[i] = util.construireObjet(
                                    type,
                                    prefix,
                                    parametrePage, fichiers);

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

                if (result instanceof String) {
                    String texte = (String) result;

                    if (texte.startsWith("redirect:")) {
                        String url = texte.substring("redirect:".length());
                        response.sendRedirect(request.getContextPath() + url);
                        return;
                    }
                    response.getWriter().println(texte);
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
