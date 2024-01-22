package backend.project;

import java.net.URI;
import java.util.Date;
import java.util.UUID;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class TransactionService {
    @Autowired
    TransactionRepository transactionRepository;

    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    RestTemplate restTemplate;

    public void createTransaction(TransactionRequest transactionRequest) {
        Transaction transaction = Transaction.builder()
                    .fromUser(transactionRequest.getFromUser())
                    .toUser(transactionRequest.getToUser())
                    .amount(transactionRequest.getAmount())
                    .transactionId(UUID.randomUUID().toString())
                    .transactionStatus("PENDING")
                    .transactionTime(new Date().toString())
                    .build();
        
        transactionRepository.save(transaction);

        // Next task is to check balance in wallet & Update Wallet by kafka communication 
        JSONObject walletRequest = new JSONObject();

        walletRequest.put("transactionId", transaction.getTransactionId());
        walletRequest.put("fromUser", transactionRequest.getFromUser());
        walletRequest.put("toUser", transactionRequest.getToUser());
        walletRequest.put("amount", transactionRequest.getAmount());

        String message = walletRequest.toString();

        kafkaTemplate.send("UPDATE_BALANCE", message);
        
    }

    @KafkaListener(topics = "UPDATE_TRANSACTION", groupId = "eWalletMsgGroup")
    public void updateTransaction(String message) throws JsonMappingException, JsonProcessingException {
        JSONObject transactionUpdate = objectMapper.readValue(message, JSONObject.class);

        String transactionStatus = (String) transactionUpdate.get("transactionStatus");
        String transactionId = (String) transactionUpdate.get("transactionId");

        Transaction transaction = transactionRepository.findByTransactionId(transactionId);
        transaction.setTransactionStatus(transactionStatus);

        // SAVE AGAIN TO UPDATE TRANSACTION STATUS
        transactionRepository.save(transaction);

        // CALL NOTIFICATION SERVICE AND SEND EMAILS
        callNotificationService(transaction);
    }

    private void callNotificationService(Transaction transaction) {
        // FETCH EMAIL FROM USER SERVICE

        String transactionId = transaction.getTransactionId();
        String fromUser = transaction.getFromUser();
        String toUser = transaction.getToUser();

        URI url = URI.create("http://localhost:8081/user/get-user-by-username?username="+fromUser);
        HttpEntity httpEntity = new HttpEntity(new HttpHeaders());

        JSONObject fromUserObject = restTemplate.exchange(url, HttpMethod.GET,httpEntity,JSONObject.class).getBody();

        String senderName = (String)fromUserObject.get("name");
        String senderEmail = (String)fromUserObject.get("email");

        url = URI.create("http://localhost:8081/user/get-user-by-username?username="+toUser);
        JSONObject toUserObject = restTemplate.exchange(url, HttpMethod.GET,httpEntity,JSONObject.class).getBody();

        String receiverEmail = (String)toUserObject.get("email");
        String receiverName = (String)toUserObject.get("name");

        // SEND THE EMAIL AND MESSAGE TO NOTIFICATIONS-SERVICE VIA KAFKA
        JSONObject emailRequest = new JSONObject();

        //SENDER should always receive email
        String senderMessageBody = String.format("Hi %s,\n" +
                        "    The transaction with transactionId %s has been %s of Rs %d.\n\n " +
                        "Thank You for using e-Wallet App. ",
                senderName,transactionId,transaction.getTransactionStatus(),transaction.getAmount());

        emailRequest.put("email", senderEmail);
        emailRequest.put("message" , senderMessageBody);

        String message = emailRequest.toString() ;

        kafkaTemplate.send("SEND_MAIL", message);

        // RECEIVER WILL GET MAIL ONLY WHEN TRANSACTION IS SUCCESSFUL

        if(transaction.getTransactionStatus().equals("SUCCESS")) {

            String receiverMessageBody = String.format("Hi %s,\n" +
                            "    You have received an amount of %d from %s.\n\n" +
                            "Use \"e-Wallet\" and Get Exciting Rewards...",
                    receiverName,transaction.getAmount(),senderName);

            emailRequest.put("email", receiverEmail);
            emailRequest.put("message" , receiverMessageBody);

            message = emailRequest.toString();

            kafkaTemplate.send("SEND_MAIL",message);
        }
    }
}
