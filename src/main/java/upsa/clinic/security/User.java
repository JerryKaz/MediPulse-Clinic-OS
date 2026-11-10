package upsa.clinic.security;

import java.util.Objects;

/** An authenticated staff member. Immutable; never holds the password. */
public record User(String username, String displayName, Role role) {

    public User {
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(role, "role");
    }

    public boolean canView(Screen module) {
        return AccessPolicy.canView(role, module);
    }

    public boolean canEdit(Screen module) {
        return AccessPolicy.canEdit(role, module);
    }

    /** "Ama Mensah" -> "AM". */
    public String initials() {
        String[] parts = displayName.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!p.isEmpty() && sb.length() < 2) {
                sb.append(Character.toUpperCase(p.charAt(0)));
            }
        }
        return sb.toString();
    }

    public String firstName() {
        return displayName.trim().split("\\s+")[0];
    }
}
