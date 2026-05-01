package com.usc.campusactivities;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import com.google.gson.JsonObject;

public class LogoutServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        logout(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        logout(request, response);
    }

    private void logout(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setHeader("Pragma", "no-cache");

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        // Prefer a normal browser redirect so sign-out cannot get "stuck" on a fetch/JSON-only response.
        if ("1".equals(request.getParameter("json"))) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            JsonObject jsonResponse = new JsonObject();
            jsonResponse.addProperty("success", true);
            jsonResponse.addProperty("message", "Logout successful");
            response.getWriter().write(jsonResponse.toString());
            return;
        }

        // Land on the public welcome page (user can click Sign In again if they want).
        String home = response.encodeRedirectURL(request.getContextPath() + "/index.html?signedOut=1");
        response.sendRedirect(home);
    }
}
