package lk.ijse.dep9.api;

import jakarta.annotation.Resource;
import jakarta.json.JsonException;
import jakarta.json.bind.JsonbBuilder;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import lk.ijse.dep9.dto.AccountDTO;
import lk.ijse.dep9.exception.ResponseStatusException;

import javax.sql.DataSource;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

@WebServlet(name = "account-servlet", value = "/accounts/*", loadOnStartup = 0)
public class AccountServlet extends HttpServlet {

    @Resource(lookup = "java:comp/env/jdbc/bank_db")
    private DataSource pool;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (request.getPathInfo() == null || request.getPathInfo().equals("/")) {
            if (request.getContentType() == null || !request.getContentType().startsWith("application/json")) {
                throw new ResponseStatusException(400, "Invalid JSON");
            }
            AccountDTO accountDTO = JsonbBuilder.create().fromJson(request.getReader(), AccountDTO.class);
            createAccount(accountDTO, response);
        }
        else {
            throw new ResponseStatusException(501);
        }
    }

    private void createAccount(AccountDTO accountDTO, HttpServletResponse response) throws IOException {
        try (Connection connection = pool.getConnection()) {
            if (accountDTO.getName() == null || !accountDTO.getName().matches("[A-Za-z ]+")) {
                throw new ResponseStatusException(400, "Invalid account holder name");
            }
            else if (accountDTO.getAddress() == null || !accountDTO.getAddress().matches("[-A-Za-z\\d/\\\\,:;|. ]+")) {
                throw new ResponseStatusException(400, "Invalid account holder address");
            }

            accountDTO.setAccount(UUID.randomUUID().toString());

            PreparedStatement stm = connection.prepareStatement("INSERT INTO Account (account_number, holder_name, holder_address) VALUES (?, ?, ?)");
            stm.setString(1, accountDTO.getAccount());
            stm.setString(2, accountDTO.getName());
            stm.setString(3, accountDTO.getAddress());
            if (stm.executeUpdate() == 1) {
                response.setStatus(HttpServletResponse.SC_CREATED);
                response.setContentType("application/json");
                JsonbBuilder.create().toJson(accountDTO, response.getWriter());
            }
            else {
                throw new SQLException("Something went wrong, try again");
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
