package backend.project;

public class UserConverter {
    public static User convertResponseToEntity(UserResponse userResponse) {
        User user = User.builder()
                .userName(userResponse.getUserName())
                .name(userResponse.getName())
                .email(userResponse.getEmail())
                .mobileNo(userResponse.getMobileNo()).build();

        return user;
    }

    public static UserResponse convertEntityToResponseDto(User user) {
        UserResponse userResponse = UserResponse.builder()
                .userName(user.getUserName())
                .name(user.getName())
                .email(user.getEmail())
                .mobileNo(user.getMobileNo()).build();

        return userResponse;
    }
}
