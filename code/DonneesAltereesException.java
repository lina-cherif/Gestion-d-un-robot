package projetrobot;
//Données reçues altérées ou non chiffrées (simulant un ransomware)
public class DonneesAltereesException extends RobotException {
 public DonneesAltereesException(String message) { super(message); }
}
