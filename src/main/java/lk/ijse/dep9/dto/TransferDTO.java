package lk.ijse.dep9.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public class TransferDTO implements Serializable {
    private String type;
    private String from;
    private String to;
    private BigDecimal amount;
}
