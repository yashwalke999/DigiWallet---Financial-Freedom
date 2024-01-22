package backend.project;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("user")
public class UserController {
    @Autowired
    UserService userService;

    @PostMapping("create-user")
    public ResponseEntity<String> createUser(@RequestBody UserResponse userResponse) {
        userService.createUser(userResponse);
        return new ResponseEntity<>("User Created", HttpStatus.CREATED);
    }

    @GetMapping("get-user-by-id")
    public ResponseEntity<UserResponse> getUserById(@RequestParam("id") int id) {
        UserResponse userResponse =  userService.getUserById(id);
        return new ResponseEntity<>(userResponse, HttpStatus.FOUND);
    }

    @GetMapping("get-user-by-username")
    public ResponseEntity<UserResponse> getUserByUserName(@RequestParam("username") String userName) throws Exception {
        UserResponse userResponse =  userService.getUserByUserName(userName);
        return new ResponseEntity<>(userResponse, HttpStatus.FOUND);
    }
}
