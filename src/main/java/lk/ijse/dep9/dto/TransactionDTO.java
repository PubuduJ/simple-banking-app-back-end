package lk.ijse.dep9.dto;

import jakarta.json.bind.annotation.JsonbPropertyOrder;
import lombok.*;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonbPropertyOrder({"type","account","amount"})
public class TransactionDTO implements Serializable {
    private String type;
    private String account;
    private BigDecimal amount;
}
