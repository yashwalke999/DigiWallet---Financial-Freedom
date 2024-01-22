package backend.project;

import java.time.Duration;
import java.util.Map;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class UserService {
    @Autowired
    UserRepository userRepository;

    @Autowired
    RedisTemplate redisTemplate;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;

    public final String REDIS_PREFIX_KEY = "user";

    // Creating A User
    public void createUser(UserResponse userResponse) {
        // User creating
        User user = UserConverter.convertResponseToEntity(userResponse);

        // first save user in DB then cache 
        // b\c if saving fail then shoud not put in cache otherwise data inconsistency
        userRepository.save(user);

        // Saving user in the cache
        saveInCache(user);

        // Sending a message through kafka:
        JSONObject walletRequest = new JSONObject();

        walletRequest.put("userName", user.getUserName());

        String message = walletRequest.toString(); 

        kafkaTemplate.send("CREATE_WALLET", message);

    }

    public UserResponse getUserById(int id) {
        User userEntity = userRepository.findById(id).get();

        UserResponse userResponse = UserConverter.convertEntityToResponseDto(userEntity);

        return userResponse;
    }

    private void saveInCache(User user) {
        Map map = objectMapper.convertValue(user, Map.class);

        redisTemplate.opsForHash().putAll(REDIS_PREFIX_KEY+user.getUserName(), map);
        
        // redis cache is for infinite tie unless we dont save a expire
        redisTemplate.expire(REDIS_PREFIX_KEY+user.getUserName() , Duration.ofHours(12));
    }

    public UserResponse getUserByUserName(String userName) throws Exception {
        User user;
        // 1. Find the user in cache
        Map map = redisTemplate.opsForHash().entries(REDIS_PREFIX_KEY+userName);

        // if information is not in Cache
        if(map==null || map.size()==0) {
            // find in the DB
            user = userRepository.findByUserName(userName);

            if(user!=null) {
                // after find in DB storing it in cache
                saveInCache(user);
            } else {
                throw new Exception("No User Found");
            }
           
        } 
        else { // Found Information in cache
            user = objectMapper.convertValue(map, User.class);
        }

        UserResponse userResponse = UserConverter.convertEntityToResponseDto(user);
        return userResponse;
    }
}
