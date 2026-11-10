package upsa.clinic.security;

/**
 * Every screen in the app. The declaration order is the sidebar order.
 * Adding a screen means adding it here and giving it a level in {@link AccessPolicy};
 * the sidebar, navigation guard and dashboard all read from these two types.
 */
public enum Screen {
    DASHBOARD("Today", Section.MAIN),
    PATIENTS("Patients", Section.MAIN),
    APPOINTMENTS("Appointments", Section.MAIN),
    VITALS("Vitals", Section.MAIN),
    BEDS("Beds", Section.MAIN),
    DOCTORS("Doctors", Section.OPERATIONS),
    PHARMACY("Pharmacy", Section.OPERATIONS),
    BILLING("Billing", Section.OPERATIONS),
    MESSAGES("Messages", Section.OPERATIONS),
    SETTINGS("Settings", Section.OPERATIONS);

    public enum Section {
        MAIN("Main"), OPERATIONS("Operations");

        private final String label;

        Section(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    private final String title;
    private final Section section;

    Screen(String title, Section section) {
        this.title = title;
        this.section = section;
    }

    public String title() {
        return title;
    }

    public Section section() {
        return section;
    }
}
