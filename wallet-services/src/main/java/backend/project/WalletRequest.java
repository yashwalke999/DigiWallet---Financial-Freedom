package backend.project;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WalletRequest {
    String userName;
}
