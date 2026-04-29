package projetrobot;

// Compte verrouillé après trop de tentatives échouées
public class CompteVerrouilleException extends RobotException {
    public CompteVerrouilleException(String message) { super(message); }
}

