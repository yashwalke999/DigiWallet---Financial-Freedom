package backend.project;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.core.JsonProcessingException;

@RestController
@RequestMapping("wallet")
public class WalletController {
    @Autowired
    WalletService walletService;

    @PostMapping("create-wallet")
    public ResponseEntity<Wallet> createWallet(@RequestParam("username") String userName) throws JsonProcessingException {
        return new ResponseEntity<>(walletService.createWallet(userName), HttpStatus.CREATED);
    }

    @GetMapping("get-wallet")
    public ResponseEntity<Wallet> getWalletByUserName(@RequestParam("username") String userName) {
        return new ResponseEntity<>(walletService.getWalletByUserName(userName), HttpStatus.CREATED);
    }
}
