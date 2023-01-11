package lk.ijse.dep9.dto;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionDTO implements Serializable {
    private String type;
    private String account;
    private BigDecimal amount;
}
