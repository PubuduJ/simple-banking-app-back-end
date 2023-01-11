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
import lk.ijse.dep9.dto.AccountDTO;
import lk.ijse.dep9.dto.TransactionDTO;
import lk.ijse.dep9.dto.TransferDTO;
import javax.sql.DataSource;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.*;
import java.util.Date;

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
            /* Data validation */
            if (transactionDTO.getAccount() == null || !transactionDTO.getAccount().matches("[A-Fa-f\\d]{8}(-[A-Fa-f\\d]{4}){3}-[A-Fa-f\\d]{12}")) {
                throw new JsonException("Invalid account number");
            }
            else if (transactionDTO.getAmount() == null || transactionDTO.getAmount().compareTo(new BigDecimal(100)) < 0) {
                throw new JsonException("Invalid amount");
            }

            /* Business validation */
            Connection connection = pool.getConnection();
            PreparedStatement stm = connection.prepareStatement("SELECT * FROM Account WHERE account_number = ?");
            stm.setString(1, transactionDTO.getAccount());
            ResultSet rst = stm.executeQuery();
            if (!rst.next()) throw new JsonException("Invalid account number");

            /* Begin transactions */
            try {
                connection.setAutoCommit(false);
                PreparedStatement stmUpdate = connection.prepareStatement("UPDATE Account SET balance = balance + ? WHERE account_number = ?");
                stmUpdate.setBigDecimal(1, transactionDTO.getAmount());
                stmUpdate.setString(2, transactionDTO.getAccount());
                if (stmUpdate.executeUpdate() != 1) throw new SQLException("Failed to update the balance");

                PreparedStatement stmNewTransaction =
                        connection.prepareStatement("INSERT INTO Transaction (account, type, description, amount, date) VALUES (?, ?, ?, ?, ?)");
                stmNewTransaction.setString(1, transactionDTO.getAccount());
                stmNewTransaction.setString(2, "CREDIT");
                stmNewTransaction.setString(3, "Deposit");
                stmNewTransaction.setBigDecimal(4, transactionDTO.getAmount());
                stmNewTransaction.setTimestamp(5, new Timestamp(new Date().getTime()));
                if (stmNewTransaction.executeUpdate() != 1) throw new SQLException("Failed to add a credit transaction record");

                connection.commit();

                ResultSet resultSet = stm.executeQuery();
                resultSet.next();
                String name = resultSet.getString("holder_name");
                String address = resultSet.getString("holder_address");
                BigDecimal balance = resultSet.getBigDecimal("balance");
                AccountDTO accountDTO = new AccountDTO(transactionDTO.getAccount(), name, address, balance);

                response.setStatus(HttpServletResponse.SC_CREATED);
                response.setContentType("application/json");
                JsonbBuilder.create().toJson(accountDTO, response.getWriter());
            }
            catch (Throwable t) {
                connection.rollback();
                t.printStackTrace();
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to deposit the money to the account");
            }
            finally {
                connection.setAutoCommit(true);
            }
            connection.close();
        }
        catch (JsonException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
        catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to deposit the money to the account");
        }
    }

    private void withdrawMoney(TransactionDTO transactionDTO, HttpServletResponse response) throws IOException {
        try {
            /* Data validation */
            if (transactionDTO.getAccount() == null || !transactionDTO.getAccount().matches("[A-Fa-f\\d]{8}(-[A-Fa-f\\d]{4}){3}-[A-Fa-f\\d]{12}")) {
                throw new JsonException("Invalid account number");
            }else if (transactionDTO.getAmount() == null || transactionDTO.getAmount().compareTo(new BigDecimal(100)) < 0) {
                throw new JsonException("Invalid amount");
            }

            /* Business validation */
            Connection connection = pool.getConnection();
            PreparedStatement stm = connection.prepareStatement("SELECT * FROM Account WHERE account_number = ?");
            stm.setString(1, transactionDTO.getAccount());
            ResultSet rst = stm.executeQuery();
            if (!rst.next()){
                throw new JsonException("Invalid account number");
            }

            BigDecimal currentBalance = rst.getBigDecimal("balance");
            if (currentBalance.subtract(transactionDTO.getAmount()).compareTo(new BigDecimal(100)) < 0){
                throw new JsonException("Insufficient account balance");
            }

            /* Begin transactions */
            try {
                connection.setAutoCommit(false);
                PreparedStatement stmUpdate = connection.prepareStatement("UPDATE  Account SET balance = balance - ? WHERE account_number = ?");
                stmUpdate.setBigDecimal(1, transactionDTO.getAmount());
                stmUpdate.setString(2, transactionDTO.getAccount());
                if (stmUpdate.executeUpdate() != 1) throw new SQLException("Failed to update the balance");

                PreparedStatement stmNewTransaction =
                        connection.prepareStatement("INSERT INTO Transaction (account, type, description, amount, date) VALUES (?, ?, ?, ?, ?)");
                stmNewTransaction.setString(1, transactionDTO.getAccount());
                stmNewTransaction.setString(2, "DEBIT");
                stmNewTransaction.setString(3, "Withdraw");
                stmNewTransaction.setBigDecimal(4, transactionDTO.getAmount());
                stmNewTransaction.setTimestamp(5, new Timestamp(new Date().getTime()));
                if (stmNewTransaction.executeUpdate() != 1) throw new SQLException("Failed to add the debit transaction record");

                Thread.sleep(5000);
                connection.commit();

                ResultSet resultSet = stm.executeQuery();
                resultSet.next();
                String name = resultSet.getString("holder_name");
                String address = resultSet.getString("holder_address");
                BigDecimal balance = resultSet.getBigDecimal("balance");
                AccountDTO accountDTO = new AccountDTO(transactionDTO.getAccount(), name, address, balance);

                response.setStatus(HttpServletResponse.SC_CREATED);
                response.setContentType("application/json");
                JsonbBuilder.create().toJson(accountDTO, response.getWriter());
            }
            catch (Throwable t) {
                connection.rollback();
                t.printStackTrace();
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to withdraw the money");
            }
            finally {
                connection.setAutoCommit(true);
            }
            connection.close();
        }
        catch (JsonException  e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
        catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to withdraw");
        }
    }

    private void transferMoney(TransferDTO transferDTO, HttpServletResponse response) throws IOException {
        try {
            /* Data validation */
            if (transferDTO.getFrom() == null || !transferDTO.getFrom().matches("[A-Fa-f\\d]{8}(-[A-Fa-f\\d]{4}){3}-[A-Fa-f\\d]{12}")) {
                throw new JsonException("Invalid from account number");
            }
            else if (transferDTO.getTo() == null || !transferDTO.getTo().matches("[A-Fa-f\\d]{8}(-[A-Fa-f\\d]{4}){3}-[A-Fa-f\\d]{12}")) {
                throw new JsonException("Invalid to account number");
            }
            else if (transferDTO.getAmount() == null || transferDTO.getAmount().compareTo(new BigDecimal(100)) < 0) {
                throw new JsonException("Invalid amount");
            }

            /* Business validation */
            Connection connection = pool.getConnection();
            PreparedStatement stm1 = connection.prepareStatement("SELECT * FROM Account WHERE account_number = ?");
            stm1.setString(1, transferDTO.getTo());
            ResultSet rst1 = stm1.executeQuery();
            if (!rst1.next()) throw new JsonException("Invalid to account number");

            PreparedStatement stm2 = connection.prepareStatement("SELECT * FROM Account WHERE account_number = ?");
            stm2.setString(1, transferDTO.getFrom());

            ResultSet rst2 = stm2.executeQuery();
            if (!rst2.next()) throw new JsonException("Invalid from account number");

            BigDecimal toAccountBalance = rst1.getBigDecimal("balance");
            BigDecimal fromAccountBalance = rst2.getBigDecimal("balance");
            if (fromAccountBalance.subtract(transferDTO.getAmount()).compareTo(new BigDecimal(100)) < 0){
                throw new JsonException("Insufficient account balance");
            }

            try {
                connection.setAutoCommit(false);
                PreparedStatement stmWithdraw = connection.prepareStatement("UPDATE  Account SET balance = ? WHERE account_number = ?");
                stmWithdraw.setBigDecimal(1, fromAccountBalance.subtract(transferDTO.getAmount()));
                stmWithdraw.setString(2, transferDTO.getFrom());
                if (stmWithdraw.executeUpdate() != 1) throw new SQLException("Failed to update the balance of the from account");

                PreparedStatement stmNewTransaction =
                        connection.prepareStatement("INSERT INTO Transaction (account, type, description, amount, date) VALUES (?, ?, ?, ?, ?)");
                stmNewTransaction.setString(1, transferDTO.getFrom());
                stmNewTransaction.setString(2, "DEBIT");
                stmNewTransaction.setString(3, "Transfer");
                stmNewTransaction.setBigDecimal(4, transferDTO.getAmount());
                stmNewTransaction.setTimestamp(5, new Timestamp(new Date().getTime()));
                if (stmNewTransaction.executeUpdate() != 1) throw new SQLException("Failed to add the debit transaction record");

                PreparedStatement stmDeposit = connection.prepareStatement("UPDATE  Account SET balance = ? WHERE account_number = ?");
                stmDeposit.setBigDecimal(1, toAccountBalance.add(transferDTO.getAmount()));
                stmDeposit.setString(2, transferDTO.getTo());
                if (stmDeposit.executeUpdate() != 1) throw new SQLException("Failed to update the balance");

                stmNewTransaction =
                        connection.prepareStatement("INSERT INTO Transaction (account, type, description, amount, date) VALUES (?, ?, ?, ?, ?)");
                stmNewTransaction.setString(1, transferDTO.getTo());
                stmNewTransaction.setString(2, "CREDIT");
                stmNewTransaction.setString(3, "Transfer");
                stmNewTransaction.setBigDecimal(4, transferDTO.getAmount());
                stmNewTransaction.setTimestamp(5, new Timestamp(new Date().getTime()));
                if (stmNewTransaction.executeUpdate() != 1) throw new SQLException("Failed to add the credit transaction record");

                connection.commit();

                ResultSet resultSet = stm2.executeQuery();
                resultSet.next();
                String name = resultSet.getString("holder_name");
                String address = resultSet.getString("holder_address");
                BigDecimal balance = resultSet.getBigDecimal("balance");
                AccountDTO accountDTO = new AccountDTO(transferDTO.getFrom(), name, address, balance);

                response.setStatus(HttpServletResponse.SC_CREATED);
                response.setContentType("application/json");
                JsonbBuilder.create().toJson(accountDTO, response.getWriter());
            }
            catch (Throwable t) {
                connection.rollback();
                t.printStackTrace();
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to transfer the money");
            }
            finally {
                connection.setAutoCommit(true);
            }
            connection.close();
        }
        catch (JsonException  e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
        catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to transfer the amount");
        }
    }
}
