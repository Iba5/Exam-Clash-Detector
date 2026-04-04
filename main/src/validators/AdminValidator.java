package src.validators;

public class AdminValidator {

    public static void validateLogin(String username, String password) {

        Validator.requireNonEmpty(username, "Username required");
        Validator.requireNonEmpty(password, "Password required");
    }
}
