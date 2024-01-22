package backend.project;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class WalletService {
    @Autowired
    WalletRepository walletRepository;

    @Autowired
    ObjectMapper objectMapper;
    
    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;

    @KafkaListener(topics = "CREATE_WALLET", groupId = "eWalletMsgGroup")
    Wallet createWallet(String message) throws JsonProcessingException {
        // Decrypting the message and extracting userName
        JSONObject walletRequest = objectMapper.readValue(message, JSONObject.class);
        String userName = (String) walletRequest.get("userName");

        // creating wallet for this userName
        int surpriseAmount = (int) ( Math.random()*101 ) ; // for adding some initial wallet amount

        Wallet wallet = Wallet.builder()
                .userName(userName)
                .balance(surpriseAmount).build(); // Wallet created

        walletRepository.save(wallet); // Wallet Saved

        return wallet;
    }

    Wallet getWalletByUserName(String userName) {
        Wallet wallet = walletRepository.findByUserName(userName);
        return wallet;
    }

    Wallet incrementWallet(String userName, int amount) {
        // 1st way
        Wallet wallet = walletRepository.findByUserName(userName);
        int newAmount = wallet.getBalance() + amount;
        wallet.setBalance(newAmount);

        walletRepository.save(wallet);
        // 2nd way is By Using Builder notation

        // 3rd way - by Custom Query
        return wallet;
    }

    Wallet decrementWallet(String userName, int amount) {
        Wallet wallet = walletRepository.findByUserName(userName);
        int newAmount = wallet.getBalance() - amount;

        wallet.setBalance(newAmount);
        walletRepository.save(wallet);

        return wallet;
    }

    void updateWalletBalance(String userName, int amount) {
        Wallet wallet = walletRepository.findByUserName(userName);
        int newAmount = wallet.getBalance() + amount;

        wallet.setBalance(newAmount);

        walletRepository.save(wallet);
    }

    @KafkaListener(topics = "UPDATE_BALANCE", groupId = "eWalletMsgGroup")
    public void updateWallet(String message) throws JsonProcessingException {
        System.out.println("I M Wallet msg received for UPDATE_BALANCE");
        // Decrypt Message
        JSONObject walletRequest = objectMapper.readValue(message, JSONObject.class);

        String transactionId = (String) walletRequest.get("transactionId");
        String fromUser = (String) walletRequest.get("fromUser");
        String toUser = (String) walletRequest.get("toUser");
        int transactionAmount = (Integer) walletRequest.get("amount");

        // get wallet of sender
        Wallet wallet = walletRepository.findByUserName(fromUser);

        // update senders and receivers wallets and send reply to transaction as transaction status
        JSONObject transactionRequest = new JSONObject();
        transactionRequest.put("transactionId", transactionId);
        
        if(wallet.getBalance()>=transactionAmount) {
            // Transaction can Happens
            transactionRequest.put("transactionStatus", "SUCCESS");
            String successMessage = transactionRequest.toString();
//            walletRepository.updateWallet(fromUser, -1*transactionAmount);
//            walletRepository.updateWallet(toUser, transactionAmount);
            updateWalletBalance(fromUser, -1*transactionAmount);
            updateWalletBalance(toUser, transactionAmount);
            
            kafkaTemplate.send("UPDATE_TRANSACTION",successMessage);
        } 
        else {
            // Low Balanse -> send Transaction Status as FAILED
            transactionRequest.put("transactionStatus", "FAILED");
            String failedMessage = transactionRequest.toString();
            
            kafkaTemplate.send("UPDATE_TRANSACTION",failedMessage);
        }
    }

}
