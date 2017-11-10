package io.narayana.web;

import java.io.IOException;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.narayana.ejb.RemoteEJBClient;


@WebServlet("/client")
public class ServletClient extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @EJB RemoteEJBClient caller; 
 
    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String[] ids = request.getParameterMap().get("id");
        
        try {
            caller.call(ids != null ? ids[0] : "1");
        } catch (Throwable e) {
            e.printStackTrace();
        }
        response.getWriter().append("Served at: ").append(request.getContextPath());
    }

    /**
     * See {@link #doGet(HttpServletRequest, HttpServletResponse)}
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
}
