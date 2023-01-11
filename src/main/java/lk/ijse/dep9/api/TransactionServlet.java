package lk.ijse.dep9.api;

import jakarta.annotation.Resource;
import jakarta.json.Json;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.stream.JsonParser;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import lk.ijse.dep9.dto.TransactionDTO;
import lk.ijse.dep9.dto.TransferDTO;
import javax.sql.DataSource;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@WebServlet(name = "transaction-servlet", value = "/transactions/*", loadOnStartup = 1)
public class TransactionServlet extends HttpServlet {

    @Resource(lookup = "java:comp/env/jdbc/bank_db")
    private DataSource pool;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (request.getPathInfo() == null || request.getPathInfo().equals("/")) {
            try {
                if (request.getContentType() == null || !request.getContentType().startsWith("application/json")){
                    throw new JsonException("Invalid JSON");
                }
                JsonParser parser = Json.createParser(request.getReader());
                parser.next();
                JsonObject jsonObj = parser.getObject();
                String transactionType = jsonObj.getString("type");
                String json = jsonObj.toString();
                if (transactionType.equalsIgnoreCase("withdraw")) {
                    TransactionDTO transactionDTO = JsonbBuilder.create().fromJson(json, TransactionDTO.class);
                    withdrawMoney(transactionDTO, response);

                }
                else if (transactionType.equalsIgnoreCase("deposit")) {
                    TransactionDTO transactionDTO = JsonbBuilder.create().fromJson(json, TransactionDTO.class);
                    depositMoney(transactionDTO, response);
                }
                else if (transactionType.equalsIgnoreCase("transfer")) {
                    TransferDTO transferDTO = JsonbBuilder.create().fromJson(json, TransferDTO.class);
                    transferMoney(transferDTO, response);
                }
                else {
                    throw new JsonException("Invalid JSON");
                }
            }
            catch (JsonException e) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            }
        }
        else {
            response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
        }
    }

    private void depositMoney(TransactionDTO transactionDTO, HttpServletResponse response) throws IOException {
        try {
            if (transactionDTO.getAccount() == null || !transactionDTO.getAccount().matches("[A-Fa-f0-9]{8}(-[A-Fa-f0-9]{4}){3}-[A-Fa-f0-9]{12}")){
                throw new JsonException("Invalid account number");
            }
            else if (transactionDTO.getAmount() == null || transactionDTO.getAmount().compareTo(new BigDecimal(1000)) < 0) {
                throw new JsonException("Invalid amount");
            }
            Connection connection = pool.getConnection();
            PreparedStatement stm = connection.prepareStatement("SELECT * FROM Account WHERE account_number = ?");
            stm.setString(1, transactionDTO.getAccount());
            ResultSet rst = stm.executeQuery();
            if (!rst.next()){
                throw new JsonException("Invalid account number");
            }

        }
        catch (JsonException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
        catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to deposit the money to the account");
        }
    }

    private void withdrawMoney(TransactionDTO transactionDTO, HttpServletResponse response) {
        System.out.println("Withdraw money");
        System.out.println(transactionDTO);
    }

    private void transferMoney(TransferDTO transferDTO, HttpServletResponse response) {
        System.out.println("Transfer money");
        System.out.println(transferDTO);
    }
}
