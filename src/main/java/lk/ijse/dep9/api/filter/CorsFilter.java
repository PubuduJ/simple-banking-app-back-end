package lk.ijse.dep9.api.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class CorsFilter extends HttpFilter {

    private String[] origins;
    @Override
    public void init() throws ServletException {
        String origin = getFilterConfig().getInitParameter("origin-locations");
        origins = origin.split(", ");
    }

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        String requestedOrigin = request.getHeader("Origin");
        for (String origin : origins) {
            if (requestedOrigin.startsWith(origin.trim())) {
                response.setHeader("Access-Control-Allow-Origin", requestedOrigin);
                break;
            }
        }
        if (request.getMethod().equalsIgnoreCase("OPTIONS")) {
            response.setHeader("Access-Control-Allow-Methods", "OPTIONS, GET, HEAD, POST, PATCH, DELETE");

            String requestedMethod = request.getHeader("Access-Control-Request-Method");
            String requestedHeaders = request.getHeader("Access-Control-Request-Headers");

            if (requestedMethod.equalsIgnoreCase("POST") && requestedHeaders.toLowerCase().contains("content-type")) {
                response.setHeader("Access-Control-Allow-Headers", "Content-Type");
            }
        }
        chain.doFilter(request, response);
    }

}
