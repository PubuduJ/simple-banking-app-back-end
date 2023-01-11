package lk.ijse.dep9.api;

import jakarta.json.JsonException;
import jakarta.json.bind.JsonbBuilder;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import lk.ijse.dep9.dto.AccountDTO;
import java.io.IOException;


@WebServlet(name = "account-servlet", value = "/accounts/*", loadOnStartup = 0)
public class AccountServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (request.getPathInfo() == null || request.getPathInfo().equals("/")) {
            try {
                if (request.getContentType() == null || !request.getContentType().startsWith("application/json")) {
                    throw new JsonException("Invalid JSON");
                }
                AccountDTO accountDTO = JsonbBuilder.create().fromJson(request.getReader(), AccountDTO.class);
                createAccount(accountDTO, response);
            }
            catch (JsonException e) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            }
        }
        else {
            response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
        }
    }

    private void createAccount(AccountDTO accountDTO, HttpServletResponse response) {
        System.out.println(accountDTO);
    }
}
