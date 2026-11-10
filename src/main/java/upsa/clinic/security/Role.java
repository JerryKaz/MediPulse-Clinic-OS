package upsa.clinic.security;

/** Staff roles. What each role may do is defined in one place: {@link AccessPolicy}. */
public enum Role {
    ADMIN("Administrator"),
    DOCTOR("Doctor"),
    NURSE("Nurse"),
    PHARMACIST("Pharmacist");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
