package backend.project;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequest {
    private String fromUser;
    private String toUser;
    private int amount;
}
