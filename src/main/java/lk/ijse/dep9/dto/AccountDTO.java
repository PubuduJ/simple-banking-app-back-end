package lk.ijse.dep9.dto;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountDTO implements Serializable {
    private String account;
    private String name;
    private String address;
    private BigDecimal balance;
}
