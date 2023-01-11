package lk.ijse.dep9.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public class TransactionDTO implements Serializable {
    private String type;
    private String account;
    private BigDecimal amount;
}
