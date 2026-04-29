package projetrobot;
public class UrgenceVitaleException extends RobotException {
    private final int niveauESI;
    private final String patientId;
    public UrgenceVitaleException(String patientId, int niveauESI, String raison) {
        super("[CODE BLUE] Patient " + patientId + " - ESI " + niveauESI + " : " + raison);
        this.niveauESI = niveauESI;
        this.patientId = patientId;
    }
    public int getNiveauESI() { return niveauESI; }
    public String getPatientId() { return patientId; }
}
