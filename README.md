# Ayaresa — Clinic OS

*Ayaresa* is Twi for "healing". It is a desktop clinic management system written in Java 17 and Swing, with an interface in the style of iOS 27 / macOS 27 (Liquid Glass). It is built for small clinics in Ghana: Ghana cedis, NHIS claims, Mobile Money and Ghana Card numbers are all supported.

Version 4.0 is a full rebuild of the earlier **UPSA Clinic (v2.0)** Java Swing project. This README shows the old and new designs side by side, compares what each version can do, and lists everything that was added.

<p align="center">
  <img width="92%" src="docs/screenshots/new/dashboard.png" alt="Ayaresa Today dashboard" />
</p>

---

## Contents

1. [Old design vs new design](#1-old-design-vs-new-design)
2. [What the old system gives vs what the new system gives](#2-what-the-old-system-gives-vs-what-the-new-system-gives)
3. [All new features](#3-all-new-features)
4. [Access control](#4-access-control)
5. [Technical changes](#5-technical-changes)
6. [Run it](#6-run-it)
7. [Project layout](#7-project-layout)
8. [Notes and limits](#8-notes-and-limits)

---

## 1. Old design vs new design

The old screenshots were taken by running the original v2.0 code from this repository's first commit. The new screenshots come from the current version. Both use the same window size.

| Screen | Old (UPSA Clinic v2.0) | New (Ayaresa v4.0) |
| --- | --- | --- |
| Sign in | <img src="docs/screenshots/old/login.png" width="300" /> | <img src="docs/screenshots/new/login.png" width="300" /> |
| Dashboard | <img src="docs/screenshots/old/dashboard.png" width="420" /> | <img src="docs/screenshots/new/dashboard.png" width="420" /> |
| Patients | <img src="docs/screenshots/old/patients.png" width="420" /> | <img src="docs/screenshots/new/patients.png" width="420" /> |
| Appointments | <img src="docs/screenshots/old/appointments.png" width="420" /> | <img src="docs/screenshots/new/appointments.png" width="420" /> |
| Vitals | <img src="docs/screenshots/old/vitals.png" width="420" /> | <img src="docs/screenshots/new/vitals.png" width="420" /> |
| Beds | <img src="docs/screenshots/old/beds.png" width="420" /> | <img src="docs/screenshots/new/beds.png" width="420" /> |
| Pharmacy | <img src="docs/screenshots/old/pharmacy.png" width="420" /> | <img src="docs/screenshots/new/pharmacy.png" width="420" /> |
| Billing | <img src="docs/screenshots/old/billing.png" width="420" /> | <img src="docs/screenshots/new/billing.png" width="420" /> |
| Doctors / Staff | <img src="docs/screenshots/old/doctors.png" width="420" /> | <img src="docs/screenshots/new/staff.png" width="420" /> |
| Messages | <img src="docs/screenshots/old/messages.png" width="420" /> | <img src="docs/screenshots/new/messages.png" width="420" /> |
| Settings | <img src="docs/screenshots/old/settings.png" width="420" /> | <img src="docs/screenshots/new/settings.png" width="420" /> |

**Screens that only exist in the new version**

| Live patient queue | Patient chart |
| --- | --- |
| <img src="docs/screenshots/new/queue.png" width="420" /> | <img src="docs/screenshots/new/patient-chart.png" width="420" /> |
| **Command palette (Ctrl+K)** | **Allergy check before dispensing** |
| <img src="docs/screenshots/new/command-palette.png" width="420" /> | <img src="docs/screenshots/new/dispense-allergy-check.png" width="420" /> |
| **Dark mode** | **Dark mode, queue** |
| <img src="docs/screenshots/new/dark-mode.png" width="420" /> | <img src="docs/screenshots/new/dark-queue.png" width="420" /> |
| **Receptionist's dashboard** | **Pharmacist's dashboard** |
| <img src="docs/screenshots/new/receptionist-view.png" width="420" /> | <img src="docs/screenshots/new/pharmacist-view.png" width="420" /> |
| **Register a patient** | **Blocked screen (doctor opening Billing)** |
| <img src="docs/screenshots/new/register-patient.png" width="420" /> | <img src="docs/screenshots/new/access-denied.png" width="420" /> |

### How the design changed

| Design element | Old | New |
| --- | --- | --- |
| Overall style | Flat Swing with a dark navy sidebar and a white header bar | iOS 27 / macOS 27 Liquid Glass: floating translucent sidebar and toolbar over a soft background |
| Icons | Emoji (🏥 👥 💊) in "Segoe UI Emoji". They only render properly on Windows | Bundled Tabler icon font (MIT), the same on Windows, macOS and Linux |
| Font | "Segoe UI" hard-coded in 128 places (Windows only) | Inter, set once in `Theme` |
| Colours | Many one-off colours, including a rainbow of chart colours | One set of design tokens (`Theme`) with Apple system colours and matching status colours everywhere |
| Light and dark | Light only | Light and dark mode, switchable in Settings or with Ctrl+K |
| Glass | None | Liquid Glass intensity slider (Clear ↔ Tinted), like iOS 27 |
| Messages to the user | `JOptionPane` popups for everything (122 of them) that block the screen | Capsule toasts that fade out. Sheets for forms. Confirmations only where an action is risky |
| Forms | Grey `JOptionPane` dialogs with labels on the left | Rounded sheets with labelled fields, placeholders, inline errors and live validation |
| Tables | Default Swing tables with a dark header and a cramped grid | Clean tables with avatars, status pills, hover highlight, and Enter or double-click to open |
| Navigation | Every role saw every menu item, then got an "Access Denied" popup | Users only see the screens they can use. Sidebar badges show live counts |
| Search | A search box that searched nothing (its placeholder was stored as text) | Ctrl+K command palette: patients, screens and actions in one place |
| Sign in | Gradient card. The password box was pre-filled with placeholder dots. "Remember me" and "Forgot password" did nothing | Glass card with icon fields, show/hide password, Enter to submit, inline errors and lockout messages |
| Dashboard | Fixed numbers (24 appointments, 1,248 patients, ₵16,968) that never changed | Live numbers from the data, chosen for the signed-in role, and updated the moment anything changes |

---

## 2. What the old system gives vs what the new system gives

| Area | Old system (UPSA Clinic v2.0) | New system (Ayaresa v4.0) |
| --- | --- | --- |
| **Data** | Hard-coded sample rows in each screen. Screens didn't share data. Everything was lost on close | One shared data store. All screens read the same records and update live. Saved to disk between runs |
| **Dashboard** | Static, made-up statistics and charts | Live statistics for each role: patients in the clinic and average wait, visits today, NEWS2 alerts, bed occupancy, stock alerts, money collected, appointments. Plus a 14-day visit trend, a "Needs attention" list, upcoming appointments and quick actions |
| **Patient queue** | None | Live queue with tokens (A001…), triage priority (Emergency / Urgent / Routine), stages from Waiting to Billing, and waiting times that turn amber after 30 minutes and red after 60 |
| **Patients** | Table with add / edit / delete in popups. Allergies always showed "None reported" | Register and edit with Ghana Card (format checked), NHIS number and expiry, blood group, allergies and conditions. Search by name, ID, phone, Ghana Card or NHIS number. Filters: NHIS active, NHIS expired, allergies, admitted, in the clinic today. CSV export |
| **Patient record** | Popup listing table columns | Patient chart side panel: allergy and NHIS banners, admission and queue status, latest observations, and a timeline of every visit, vital sign, medicine, appointment and bill |
| **Vitals** | Table of readings. Status based only on temperature ("Fever" or "High Fever") | NEWS2 early warning score (Royal College of Physicians) from 7 measurements, with the score previewed live while typing. Risk bands Low / Low–medium / Medium / High with the recommended response. A medium or high score automatically raises the patient's queue priority and adds an alert. Impossible values are rejected |
| **Appointments** | Table of appointments | Day view per clinician with previous/next day. Double bookings are blocked. Check in (adds the patient to the queue), complete, no-show and cancel |
| **Beds** | Table of beds | Ward map. Each bed is coloured by its patient's latest NEWS2 risk. Click a free bed to admit, an occupied bed to open the chart or discharge |
| **Doctors / Staff** | Doctors table | Staff directory (doctors, nurses, midwife, pharmacist, front desk) with on-duty status (admins can change it) and today's appointment count |
| **Pharmacy** | Stock table. The expiry date was only text | Reorder levels, low-stock and out-of-stock flags, a 90-day expiry warning, expired medicines blocked, and NHIS cover shown. Dispensing checks for allergies, stock and expiry first. Allergy warnings need a confirmation, which is recorded. Receive stock. Dispensed medicines are added to the patient's bill |
| **Billing** | Invoice table. NHIS was just a word in a column. Amounts were text | Amounts in Ghana cedis using exact decimal arithmetic. Payment by cash, Mobile Money (transaction ID required), card or waiver. NHIS claims are refused if membership had expired on the day of service. Claims can be marked paid or rejected. Shows collected today, last 7 days, outstanding and pending claims, with a 7-day chart and itemised invoices |
| **Messages** | Simple message list | Team channels (Ward A, Pharmacy, Front desk) with chat bubbles. Your own messages appear on the right |
| **Settings** | Settings form that didn't save anything | Light/dark mode, Liquid Glass slider and inactivity sign-out time (all remembered). The audit log viewer and demo-data reset are for admins only |
| **Alerts** | A bell that counted test notifications | A bell with real alerts (high NEWS2, emergencies in the queue, expired or low stock, rejected NHIS claims). Click an alert to go to its source |
| **Search** | Not working | Ctrl+K command palette for patients, screens and actions |
| **Roles** | Admin, Doctor, Nurse | Admin, Doctor, Nurse, Pharmacist, Receptionist |
| **Access control** | Two lists that disagreed. The sidebar showed everything to everyone | One policy with None / View / Full for every screen. Screens and buttons a role can't use are hidden |
| **Passwords** | Plain text in the code | PBKDF2-SHA256 hashes, salted, with a constant-time check |
| **Brute-force protection** | None | 60-second lockout after 5 failed attempts |
| **Session security** | None | Automatic sign-out after inactivity (15 minutes by default) |
| **Audit trail** | None | Records sign-ins, failures, lockouts, denied access and every data change, with who and when. Kept between runs |
| **Keyboard** | None | Ctrl+K, Ctrl+N (new patient), Ctrl+1–9 (screens), Enter to submit, Esc to close |
| **Accessibility** | Emoji icons with no labels | Screen-reader names on icon buttons, the chart, the segmented control and avatars |
| **Build** | No build file (both CI workflows failed). Compiled `.class` files committed | Maven with tests and a single runnable jar. One working CI workflow |
| **Tests** | None | 32 unit tests (access policy, sign-in, NEWS2, clinical and billing rules) |

---

## 3. All new features

### Clinical
- **NEWS2 early warning score**, calculated automatically from respiration rate, SpO₂, oxygen, systolic BP, pulse, consciousness (ACVPU) and temperature. The score previews live as you type, and the form shows each measurement's contribution.
- **Automatic escalation**: a medium or high NEWS2 raises the patient's queue priority to Urgent or Emergency.
- **Range checks on vitals**: impossible readings are refused (for example, diastolic higher than systolic, or SpO₂ above 100%).
- **Allergy safety check** before dispensing, using each drug's allergy class (penicillin, sulfa, NSAID). It can only be overridden with a confirmation, and the override is logged and shown on the patient's timeline.
- **Expired medicine block**: an expired batch can never be dispensed.
- **Bed map by risk**: each occupied bed takes the colour of its patient's latest NEWS2.

### Front desk and flow
- **Live patient queue**: tokens, triage priority, stages from Waiting to Billing, live waiting times, and "Start triage", "Send to doctor", "Send to pharmacy", "Send to billing" and "Finish visit" buttons.
- **Check-in** from the queue, the dashboard, a patient chart or Ctrl+K. Checking in an appointment also adds the patient to the queue.
- **Appointment day view** for each clinician, with double bookings blocked.
- **Patient registration** with Ghana Card format check (GHA-123456789-0), NHIS number and expiry, allergies and conditions.

### Money (Ghana)
- **Ghana cedis** everywhere, with exact decimal maths.
- **Mobile Money** payments (transaction ID required), plus cash, card and waivers.
- **NHIS claims**: submitted only if membership was valid on the day of service. Claims can be marked paid or rejected.
- **Automatic billing**: dispensed medicines are added to the patient's open bill, and NHIS is chosen automatically when membership is active and the drug is covered.
- **Revenue view**: collected today, last 7 days, outstanding and pending claims, with a 7-day chart.

### Pharmacy
- Reorder levels, low-stock and out-of-stock flags, a 90-day expiry warning, and NHIS cover shown for each medicine.
- Receive stock.
- Filters: low stock, expiring, expired, NHIS covered.

### Everyday use
- **Command palette (Ctrl+K / ⌘K)** to find patients, jump to screens and run actions.
- **Patient chart** side panel with a complete timeline.
- **Live updates**: change something on one screen and every other screen, badge and alert updates straight away.
- **Toasts** instead of popups. **Sheets** for forms, with inline errors.
- **Dark mode** and a **Liquid Glass slider**.
- **Dashboards for each role**: an admin, doctor, nurse, pharmacist and receptionist each see a different Today screen.
- **Team messages** by channel.
- **CSV export** of patients, safe to open in Excel (formula injection blocked).
- **Data saved between runs** (`~/.ayaresa/clinic.dat`), written so that a crash never leaves a half-written file.

### Security
- One access policy (None / View / Full) used by every screen and button.
- New **Pharmacist** and **Receptionist** roles.
- Hashed passwords, a lockout after repeated failures, automatic sign-out after inactivity, and an audit log.

---

## 4. Access control

Who can do what is defined in a single file, `security/AccessPolicy`. Anything not granted is denied. The sidebar, navigation, command palette and every action button read from that one policy. A screen a role can't open is never built.

| Screen | Admin | Doctor | Nurse | Pharmacist | Receptionist |
| --- | --- | --- | --- | --- | --- |
| Today | Full | Full | Full | Full | Full |
| Queue | Full | Full | Full | View | Full |
| Patients | Full | Full | Full | View | Full |
| Appointments | Full | Full | Full | – | Full |
| Vitals | Full | Full | Full | – | – |
| Beds | Full | View | Full | – | – |
| Staff | Full | View | View | – | View |
| Pharmacy | Full | View | View | Full | – |
| Billing | Full | – | – | – | Full |
| Messages | Full | Full | Full | Full | Full |
| Settings | Full | View | View | View | View |

Settings at View level gives the personal appearance options only. Full adds security, the audit log and data tools.

**Demo accounts:**

| Username | Password | Role |
| --- | --- | --- |
| admin | 1234 | Administrator |
| doctor1 | 3333 | Doctor |
| nurse1 | 2222 | Nurse |
| pharm1 | 4444 | Pharmacist |
| front1 | 5555 | Receptionist |

---

## 5. Technical changes

| | Old | New |
| --- | --- | --- |
| Java version | Not set (CI used 11 and 17) | 17 (runs on 17+) |
| Build | None (CI referenced missing `pom.xml` and `build.xml`) | Maven: `mvn exec:java`, `mvn package` → `ayaresa-4.0.0.jar` |
| Package | `upsa.clinic` inside a folder named `Java Source Files (.java)` | `com.ayaresa` in the standard `src/main/java` layout |
| Architecture | Each screen held its own hard-coded table data | Layers: `domain` (immutable records) → `clinical` (NEWS2, safety checks) → `store` (rules, events, saving) → `screens` / `shell` / `ui` |
| Data flow | None between screens | The store publishes change events; screens subscribe and refresh only when visible |
| Saving | None | Serialized snapshot written on a background thread with an atomic file replace. An unreadable file is set aside and demo data is loaded instead |
| Money | Text such as "₵420.00" | `BigDecimal` (never floating point) |
| Look and feel | System default, set after the screens were built (so it had no effect) | FlatLaf, set once before any screen is created, plus custom-painted glass components |
| Icons | Emoji | Tabler icon font bundled as a resource (no extra library) |
| Dependencies | None | FlatLaf, FlatLaf Inter font, JUnit 5 (tests only) |
| Tests | None | 32 unit tests |
| Lines of Java | About 6,100 in 16 files | About 8,900 in 77 files |

---

## 6. Run it

Requires **Java 17+** and **Maven 3.9+**.

```bash
cd Clinic-demo            # or the folder you unzipped

mvn exec:java                          # run from source
mvn package                            # run the tests and build one runnable jar
java -jar target/ayaresa-4.0.0.jar
```

In IntelliJ IDEA, Eclipse or NetBeans, open the folder as a **Maven project** and run `com.ayaresa.Main`.

Data is saved to `~/.ayaresa/clinic.dat`. Delete that file, or use **Settings → Reset demo data**, to start fresh.

---

## 7. Project layout

```
src/main/java/com/ayaresa/
  Main.java, Brand.java, Preferences.java
  domain/      Patient, QueueEntry, VitalSigns, Appointment, Bed, Drug, Invoice, ... (immutable records)
  clinical/    News2 (early warning score), SafetyCheck (allergy / stock / expiry before dispensing)
  store/       ClinicStore (validation, change events, audit, atomic save), SeedData
  security/    Role, Screen, AccessLevel, AccessPolicy, AuthService, PasswordHasher, AuditLog
  shell/       MainWindow, LoginWindow, Sidebar, Toolbar, CommandPalette, IdleLock
  screens/     Today, Queue, Patients, Appointments, Vitals, Beds, Staff, Pharmacy, Billing,
               Messages, Settings, PatientChart, Sheets (all forms)
  ui/          Theme (light/dark tokens), GlassPanel, Card, DataTable, Sheet, Drawer, Toast,
               Chips, BarChart, Icons, VerticalScroll, ...
src/main/resources/com/ayaresa/fonts/   Tabler icon font (MIT licence)
src/test/java/com/ayaresa/              Access policy, sign-in, NEWS2 and store tests
docs/screenshots/old/                   Screenshots of the original v2.0 app
docs/screenshots/new/                   Screenshots of Ayaresa v4.0
```

The product name is set in one place: `Brand.java`.

---

## 8. Notes and limits

- **NEWS2** is a screening aid and doesn't replace clinical judgement. SpO₂ scale 2 (for patients with hypercapnic respiratory failure) isn't offered, because it should only be used under clinician direction.
- **Storage** is a single file on one computer, which suits a single-PC clinic. For several PCs sharing data, put PostgreSQL behind `ClinicStore`; the screens don't need to change.
- **NHIS and Mobile Money** are recorded in the app; there is no live connection to the NHIA or mobile money networks.
- **Demo accounts** are created in code for the demo. A real deployment needs user management backed by the database.

## License

MIT © Jerry Myron
