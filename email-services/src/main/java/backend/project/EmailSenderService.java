package backend.project;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailSenderService {
    @Autowired
    JavaMailSender javaMailSender;

    @Autowired
    SimpleMailMessage simpleMailMessage;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    EmailRepository emailRepository;


    @KafkaListener(topics = "SEND_MAIL", groupId = "eWalletMsgGroup")
    public void sendEmails(String message) throws JsonProcessingException {
        JSONObject mailRequest = objectMapper.readValue(message, JSONObject.class);

        String email = (String) mailRequest.get("email");
        String messageBody = (String) mailRequest.get("message");

        // saving email to table
        EmailEntity emailEntity = EmailEntity.builder()
                .status("DRAFT")
                .emailTo(email).build();
        emailRepository.save(emailEntity);

        simpleMailMessage.setTo(email);
        simpleMailMessage.setFrom("codewithdk1@gmail.com");
        simpleMailMessage.setSubject("eWallet Transaction Mail");
        simpleMailMessage.setText(messageBody);

        javaMailSender.send(simpleMailMessage);

        emailEntity.setStatus("SENT");
        emailRepository.save(emailEntity);
    }

}
