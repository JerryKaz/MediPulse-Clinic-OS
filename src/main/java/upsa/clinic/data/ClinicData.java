package upsa.clinic.data;

import upsa.clinic.security.Screen;
import upsa.clinic.security.User;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Sample data for the dashboard. Everything the UI shows comes through this class, so
 * swapping it for a database-backed repository later won't touch the screens.
 */
public final class ClinicData {

    public enum Range { TODAY, WEEK, MONTH }

    public enum CareStatus { CHECKED_IN, WAITING, STABLE, CRITICAL }

    public enum BedStatus { FREE, STABLE, OBSERVATION, CRITICAL }

    public record Appointment(LocalTime time, String patient, String reason, String doctor, CareStatus status) {
    }

    public record Bed(String id, BedStatus status) {
    }

    public record StockItem(String drug, int unitsLeft, int reorderLevel) {
    }

    /** One headline number; {@code good} decides whether the trend is shown green or red. */
    public record Stat(Screen module, String label, String value, String trend, Boolean good) {
    }

    private ClinicData() {
    }

    public static List<Stat> stats(Range range) {
        return switch (range) {
            case TODAY -> List.of(
                    new Stat(Screen.PATIENTS, "Patients seen", "48", "+12% vs yesterday", true),
                    new Stat(Screen.APPOINTMENTS, "Appointments", "12", "4 remaining", null),
                    new Stat(Screen.BEDS, "Bed occupancy", "78%", "2 beds free", false),
                    new Stat(Screen.PHARMACY, "Low stock", "5", "Reorder needed", false),
                    new Stat(Screen.BILLING, "Revenue", "GH₵ 6,420", "+8% vs yesterday", true),
                    new Stat(Screen.MESSAGES, "Unread messages", "3", "1 from Dr. Mensah", null));
            case WEEK -> List.of(
                    new Stat(Screen.PATIENTS, "Patients seen", "291", "+5% vs last week", true),
                    new Stat(Screen.APPOINTMENTS, "Appointments", "74", "9 no-shows", false),
                    new Stat(Screen.BEDS, "Bed occupancy", "71%", "Avg. this week", null),
                    new Stat(Screen.PHARMACY, "Low stock", "5", "Reorder needed", false),
                    new Stat(Screen.BILLING, "Revenue", "GH₵ 38,900", "+3% vs last week", true),
                    new Stat(Screen.MESSAGES, "Unread messages", "3", "1 from Dr. Mensah", null));
            case MONTH -> List.of(
                    new Stat(Screen.PATIENTS, "Patients seen", "1,184", "+9% vs last month", true),
                    new Stat(Screen.APPOINTMENTS, "Appointments", "312", "94% attended", true),
                    new Stat(Screen.BEDS, "Bed occupancy", "69%", "Avg. this month", null),
                    new Stat(Screen.PHARMACY, "Low stock", "5", "Reorder needed", false),
                    new Stat(Screen.BILLING, "Revenue", "GH₵ 152,300", "-2% vs last month", false),
                    new Stat(Screen.MESSAGES, "Unread messages", "3", "1 from Dr. Mensah", null));
        };
    }

    public static List<Appointment> upcomingAppointments() {
        return List.of(
                new Appointment(LocalTime.of(14, 30), "Kwame Asante", "General check-up", "Dr. Mensah", CareStatus.CHECKED_IN),
                new Appointment(LocalTime.of(15, 0), "Ama Boateng", "Follow-up", "Dr. Owusu", CareStatus.STABLE),
                new Appointment(LocalTime.of(15, 15), "Yaw Ofori", "High BP review", "Dr. Mensah", CareStatus.CRITICAL),
                new Appointment(LocalTime.of(15, 45), "Efua Addo", "Lab results", "Dr. Owusu", CareStatus.WAITING),
                new Appointment(LocalTime.of(16, 10), "Kojo Amponsah", "Malaria test", "Dr. Mensah", CareStatus.WAITING));
    }

    public static List<Bed> wardA() {
        BedStatus[] s = {
                BedStatus.CRITICAL, BedStatus.STABLE, BedStatus.STABLE, BedStatus.OBSERVATION, BedStatus.STABLE, BedStatus.FREE,
                BedStatus.STABLE, BedStatus.STABLE, BedStatus.FREE, BedStatus.STABLE, BedStatus.OBSERVATION, BedStatus.STABLE};
        List<Bed> beds = new ArrayList<>();
        for (int i = 0; i < s.length; i++) {
            beds.add(new Bed("A" + (i + 1), s[i]));
        }
        return beds;
    }

    public static List<StockItem> lowStock() {
        return List.of(
                new StockItem("Amoxicillin 500 mg", 18, 50),
                new StockItem("Artemether-Lumefantrine", 9, 40),
                new StockItem("Paracetamol 500 mg", 60, 200),
                new StockItem("ORS sachets", 22, 60),
                new StockItem("Insulin (Actrapid)", 3, 10));
    }

    /** Only notifications about modules the user can see. */
    public static List<String> notificationsFor(User user) {
        List<String> out = new ArrayList<>();
        if (user.canView(Screen.VITALS)) {
            out.add("Yaw Ofori's blood pressure is critical (182/110).");
        }
        if (user.canView(Screen.PHARMACY)) {
            out.add("Insulin (Actrapid) is below reorder level.");
        }
        if (user.canView(Screen.MESSAGES)) {
            out.add("New message from Dr. Mensah.");
        }
        return out;
    }
}
