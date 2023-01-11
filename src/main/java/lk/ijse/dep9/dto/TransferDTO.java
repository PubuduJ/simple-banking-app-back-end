package lk.ijse.dep9.dto;

import jakarta.json.bind.annotation.JsonbPropertyOrder;
import lombok.*;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonbPropertyOrder({"type","from","to","amount"})
public class TransferDTO implements Serializable {
    private String type;
    private String from;
    private String to;
    private BigDecimal amount;
}
