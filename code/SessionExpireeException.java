package projetrobot;


//Token de session invalide ou expiré
public class SessionExpireeException extends RobotException {
 public SessionExpireeException(String message) { super(message); }
}
