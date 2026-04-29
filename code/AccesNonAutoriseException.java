package projetrobot;


// Tentative d'accès à une ressource sans les droits suffisants
class AccesNonAutoriseException extends RobotException {
    public AccesNonAutoriseException(String message) { super(message); }
}

