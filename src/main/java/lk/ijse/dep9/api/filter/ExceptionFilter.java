package lk.ijse.dep9.api.filter;

import jakarta.json.bind.JsonbBuilder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lk.ijse.dep9.dto.ResponseStatusDTO;
import lk.ijse.dep9.exception.ResponseStatusException;

import java.io.IOException;
import java.util.Date;

public class ExceptionFilter extends HttpFilter {

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        try {
            super.doFilter(request, response, chain);
        }
        catch (Throwable t) {
            ResponseStatusException r = t instanceof ResponseStatusException ? (ResponseStatusException) t : null;
            if (r == null || r.getStatus() >= 500) {
                t.printStackTrace();
            }
            ResponseStatusDTO statusDTO = new ResponseStatusDTO();

            statusDTO.setStatus(r == null ? 500 : r.getStatus());
            statusDTO.setMessage(t.getMessage());
            statusDTO.setPath(request.getRequestURI());
            statusDTO.setTimestamp(new Date().getTime());

            response.setContentType("application/json");
            response.setStatus(statusDTO.getStatus());
            JsonbBuilder.create().toJson(statusDTO, response.getWriter());
        }
    }
}
