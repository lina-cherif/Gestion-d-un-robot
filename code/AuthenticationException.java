public class AuthenticationException extends RobotException {
    public AuthenticationException(String message) { super("[AUTH ERROR] " + message); }
}
