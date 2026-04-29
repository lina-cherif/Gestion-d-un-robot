package projetrobot;

/**
 * Levée quand le honeypot est déclenché.
 * Indique une intrusion confirmée à 100% dans le système.
 */
class HoneypotException extends RobotException {
    public HoneypotException(String message) { super(message); }
}