import java.math.BigDecimal;
import java.sql.*;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

public class Main {
    private static final String URL = "jdbc:mysql://localhost:3306/apartment_society";
    private static final String USER = "root";
    private static final String PASSWORD = "root";
    private static final Scanner sc = new Scanner(System.in);
    private static Connection con;

    // Custom exceptions provide clear messages for invalid input and missing records.
    static class InvalidInputException extends Exception {
        InvalidInputException(String message) { super(message); }
    }
    static class RecordNotFoundException extends Exception {
        RecordNotFoundException(String message) { super(message); }
    }

    // OOP: Abstraction. A generic Person is not created directly in this application.
    static abstract class Person {
        private final String name, phone, email;
        Person(String name, String phone, String email) {
            this.name = name; this.phone = phone; this.email = email;
        }
        public String getName() { return name; }
        public String getPhone() { return phone; }
        public String getEmail() { return email; }
        public abstract String getPersonType();
        public String getDetails() {
            return getPersonType() + ": " + name + " | Phone: " + phone + " | Email: " + email;
        }
    }

    // OOP: Inheritance and polymorphism through overridden methods.
    static class Resident extends Person {
        private final int residentId, apartmentId;
        private final String flatNumber;
        Resident(int id, String name, String phone, String email, int apartmentId, String flat) {
            super(name, phone, email);
            this.residentId = id; this.apartmentId = apartmentId; this.flatNumber = flat;
        }
        public int getResidentId() { return residentId; }
        public int getApartmentId() { return apartmentId; }
        public String getFlatNumber() { return flatNumber; }
        @Override public String getPersonType() { return "Resident"; }
        @Override public String getDetails() {
            return super.getDetails() + " | Flat: " + flatNumber + " | Resident ID: " + residentId;
        }
    }

    // OOP: Encapsulation in the remaining model classes.
    static class Apartment {
        private final int id;
        private final String flat, block;
        private final BigDecimal maintenance;
        Apartment(int id, String flat, String block, BigDecimal maintenance) {
            this.id = id; this.flat = flat; this.block = block; this.maintenance = maintenance;
        }
        public int getId() { return id; }
        public String getFlat() { return flat; }
        public String getBlock() { return block; }
        public BigDecimal getMaintenance() { return maintenance; }
    }
    static class MaintenancePayment {
        private final int id, residentId;
        private final String month;
        private final BigDecimal amount;
        MaintenancePayment(int id, int residentId, String month, BigDecimal amount) {
            this.id = id; this.residentId = residentId; this.month = month; this.amount = amount;
        }
        public int getId() { return id; }
        public int getResidentId() { return residentId; }
        public String getMonth() { return month; }
        public BigDecimal getAmount() { return amount; }
    }
    static class Complaint {
        private final int id, residentId;
        private final String residentName, description, status;
        private final Timestamp createdAt;
        Complaint(int id, int residentId, String residentName, String description,
                  String status, Timestamp createdAt) {
            this.id = id; this.residentId = residentId; this.residentName = residentName;
            this.description = description; this.status = status; this.createdAt = createdAt;
        }
        public int getId() { return id; }
        public int getResidentId() { return residentId; }
        public String getResidentName() { return residentName; }
        public String getDescription() { return description; }
        public String getStatus() { return status; }
        public Timestamp getCreatedAt() { return createdAt; }
    }

    // A small report model used when monthly collections are grouped and displayed.
    static class MonthlyCollection {
        private final String month;
        private final int paymentCount;
        private final BigDecimal total;

        MonthlyCollection(String month, int paymentCount, BigDecimal total) {
            this.month = month;
            this.paymentCount = paymentCount;
            this.total = total;
        }
        public String getMonth() { return month; }
        public int getPaymentCount() { return paymentCount; }
        public BigDecimal getTotal() { return total; }
    }

    public static void main(String[] args) {
        try {
            con = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Connected to Apartment Society database.");
            boolean running = true;
            while (running) {
                menu();
                System.out.print("Enter choice: ");
                String choice = sc.nextLine().trim();
                if (choice.isEmpty()) { System.out.println("Error: Menu choice cannot be empty.\n"); continue; }
                try {
                    switch (choice) {
                        case "1" -> registerResident();
                        case "2" -> viewApartments();
                        case "3" -> viewResidents();
                        case "4" -> recordPayment();
                        case "5" -> pendingPayments();
                        case "6" -> registerComplaint();
                        case "7" -> viewComplaints();
                        case "8" -> resolveComplaint();
                        case "9" -> collectionReport();
                        case "10" -> removeResident();
                        case "0" -> { running = false; System.out.println("Goodbye!"); }
                        default -> System.out.println("Invalid choice. Enter a number from 0 to 10.");
                    }
                } catch (InvalidInputException | RecordNotFoundException e) {
                    System.out.println("Error: " + e.getMessage());
                } catch (SQLIntegrityConstraintViolationException e) {
                    System.out.println("Database constraint error: duplicate or linked record. " + e.getMessage());
                } catch (SQLException e) {
                    System.out.println("Database error: " + e.getMessage());
                } catch (Exception e) {
                    System.out.println("Unexpected error: " + e.getMessage());
                }
                System.out.println();
            }
        } catch (SQLException e) {
            System.out.println("Could not connect to MySQL. Check that MySQL is running and verify URL, username and password.");
            System.out.println("Details: " + e.getMessage());
        } finally {
            if (con != null) try { con.close(); } catch (SQLException e) {
                System.out.println("Could not close database connection: " + e.getMessage());
            }
            sc.close();
        }
    }

    private static void menu() {
        System.out.println("========== APARTMENT SOCIETY MANAGEMENT ==========");
        System.out.println("1. Register Resident\n2. View Apartments\n3. View Residents");
        System.out.println("4. Record Maintenance Payment\n5. View Pending Payments");
        System.out.println("6. Register Complaint\n7. View Complaints\n8. Resolve Complaint");
        System.out.println("9. Monthly Collection Report\n10. Remove Resident\n0. Exit");
        System.out.println("==================================================");
    }

    // ---------- Input validation ----------
    private static String readRequired(String prompt) throws InvalidInputException {
        System.out.print(prompt);
        String s = sc.nextLine().trim();
        if (s.isEmpty()) throw new InvalidInputException("This field cannot be left empty.");
        return s;
    }
    private static int readPositiveInt(String prompt) throws InvalidInputException {
        String s = readRequired(prompt);
        try {
            int n = Integer.parseInt(s);
            if (n <= 0) throw new InvalidInputException("Enter a number greater than zero.");
            return n;
        } catch (NumberFormatException e) {
            throw new InvalidInputException("Enter a valid whole number (digits only).");
        }
    }
    private static BigDecimal readAmount(String prompt) throws InvalidInputException {
        String s = readRequired(prompt);
        try {
            BigDecimal n = new BigDecimal(s);
            if (n.compareTo(BigDecimal.ZERO) <= 0)
                throw new InvalidInputException("Amount must be greater than zero.");
            return n;
        } catch (NumberFormatException e) {
            throw new InvalidInputException("Enter a valid amount, for example 2500 or 2500.50.");
        }
    }
    private static String readEmail() throws InvalidInputException {
        String email = readRequired("Email: ");
        if (!Pattern.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", email))
            throw new InvalidInputException("Invalid email. Example: name@example.com");
        return email;
    }
    private static String readPhone() throws InvalidInputException {
        String phone = readRequired("Phone number: ");
        if (!phone.matches("[0-9]{7,15}"))
            throw new InvalidInputException("Phone number must contain 7 to 15 digits only.");
        return phone;
    }
    private static String readMonth() throws InvalidInputException {
        String month = readRequired("Month (YYYY-MM): ");
        try { YearMonth.parse(month); return month; }
        catch (Exception e) { throw new InvalidInputException("Invalid month. Use YYYY-MM, for example 2026-11."); }
    }
    private static void checkResident(int id) throws SQLException, RecordNotFoundException {
        try (PreparedStatement ps = con.prepareStatement("SELECT resident_id FROM Resident WHERE resident_id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new RecordNotFoundException("Resident ID " + id + " does not exist.");
            }
        }
    }

    // 1. Register resident
    private static void registerResident() throws SQLException, InvalidInputException, RecordNotFoundException {
        System.out.println("--- Register Resident ---");
        String name = readRequired("Name: ");
        if (!name.matches("[\\p{L} .'-]{2,80}"))
            throw new InvalidInputException("Enter a valid name (2-80 characters).");
        String phone = readPhone();
        String email = readEmail();
        int apartmentId = readPositiveInt("Apartment ID: ");
        try (PreparedStatement ps = con.prepareStatement("SELECT apartment_id FROM Apartment WHERE apartment_id=?")) {
            ps.setInt(1, apartmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new RecordNotFoundException("Apartment ID " + apartmentId + " was not found.");
            }
        }
        String sql = "INSERT INTO Resident(name,phone,email,apartment_id) VALUES(?,?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name); ps.setString(2, phone); ps.setString(3, email); ps.setInt(4, apartmentId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) System.out.println("Resident registered. New Resident ID: " + keys.getInt(1));
                else System.out.println("Resident registered successfully.");
            }
        }
    }

    // 2. View apartments
    private static void viewApartments() throws SQLException {
        String sql = "SELECT apartment_id,flat_number,block_name,monthly_maintenance FROM Apartment ORDER BY block_name,flat_number";
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            System.out.printf("%-12s %-15s %-12s %-20s%n", "Apartment ID", "Flat", "Block", "Monthly Maintenance");
            boolean found = false;
            while (rs.next()) {
                found = true;
                Apartment a = new Apartment(rs.getInt("apartment_id"), rs.getString("flat_number"),
                        rs.getString("block_name"), rs.getBigDecimal("monthly_maintenance"));
                System.out.printf("%-12d %-15s %-12s Rs. %-16s%n", a.getId(), a.getFlat(), a.getBlock(), a.getMaintenance());
            }
            if (!found) System.out.println("No apartments found.");
        }
    }

    // 3. View residents
    private static void viewResidents() throws SQLException {
        String sql = "SELECT r.resident_id,r.name,r.phone,r.email,r.apartment_id,a.flat_number " +
                     "FROM Resident r JOIN Apartment a ON r.apartment_id=a.apartment_id ORDER BY r.resident_id";
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            boolean found = false;
            while (rs.next()) {
                found = true;
                Resident r = new Resident(rs.getInt("resident_id"), rs.getString("name"),
                        rs.getString("phone"), rs.getString("email"), rs.getInt("apartment_id"), rs.getString("flat_number"));
                // Parent reference + overridden method demonstrates runtime polymorphism.
                Person person = r;
                System.out.println(person.getDetails());
            }
            if (!found) System.out.println("No residents registered.");
        }
    }

    // 4. Record maintenance payment
    private static void recordPayment() throws SQLException, InvalidInputException, RecordNotFoundException {
        System.out.println("--- Record Maintenance Payment ---");
        int residentId = readPositiveInt("Resident ID: ");
        checkResident(residentId);
        String month = readMonth();
        BigDecimal amount = readAmount("Amount paid: Rs. ");
        String sql = "INSERT INTO MaintenancePayment(resident_id,payment_month,amount,payment_date,status) VALUES(?,?,?,CURRENT_DATE,'PAID')";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, residentId); ps.setString(2, month); ps.setBigDecimal(3, amount);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    MaintenancePayment p = new MaintenancePayment(keys.getInt(1), residentId, month, amount);
                    System.out.println("Payment saved. Payment ID: " + p.getId() + ", Month: " + p.getMonth() + ", Amount: Rs. " + p.getAmount());
                } else System.out.println("Payment recorded successfully.");
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new InvalidInputException("A payment for this resident and month already exists.");
        }
    }

    // 5. Pending payments for the current month
    private static void pendingPayments() throws SQLException {
        String sql = "SELECT r.resident_id,r.name,a.flat_number,a.monthly_maintenance " +
                "FROM Resident r JOIN Apartment a ON r.apartment_id=a.apartment_id " +
                "WHERE NOT EXISTS (SELECT 1 FROM MaintenancePayment p WHERE p.resident_id=r.resident_id " +
                "AND p.payment_month=DATE_FORMAT(CURRENT_DATE,'%Y-%m') AND p.status='PAID') ORDER BY r.resident_id";
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            System.out.println("Pending payments for " + YearMonth.now());
            System.out.printf("%-8s %-20s %-12s %-18s%n", "ID", "Resident", "Flat", "Amount Due");
            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.printf("%-8d %-20s %-12s Rs. %-14s%n", rs.getInt("resident_id"),
                        rs.getString("name"), rs.getString("flat_number"), rs.getBigDecimal("monthly_maintenance"));
            }
            if (!found) System.out.println("No pending payments for this month.");
        }
    }

    // 6. Register complaint
    private static void registerComplaint() throws SQLException, InvalidInputException, RecordNotFoundException {
        System.out.println("--- Register Complaint ---");
        int residentId = readPositiveInt("Resident ID: ");
        checkResident(residentId);
        String description = readRequired("Complaint description: ");
        if (description.length() < 5 || description.length() > 500)
            throw new InvalidInputException("Description must contain 5 to 500 characters.");
        String sql = "INSERT INTO Complaint(resident_id,description,status) VALUES(?,?,'OPEN')";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, residentId); ps.setString(2, description); ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) System.out.println("Complaint registered. Complaint ID: " + keys.getInt(1));
                else System.out.println("Complaint registered successfully.");
            }
        }
    }

    // 7. View complaints. ArrayList demonstrates the Collections Framework;
    // a lambda expression filters OPEN complaints from the list.
    private static void viewComplaints() throws SQLException {
        String sql = "SELECT c.complaint_id,c.resident_id,r.name,c.description,c.status,c.created_at " +
                "FROM Complaint c JOIN Resident r ON c.resident_id=r.resident_id ORDER BY c.created_at DESC";

        List<Complaint> complaints = new ArrayList<>();
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                complaints.add(new Complaint(
                        rs.getInt("complaint_id"), rs.getInt("resident_id"), rs.getString("name"),
                        rs.getString("description"), rs.getString("status"), rs.getTimestamp("created_at")));
            }
        }

        if (complaints.isEmpty()) {
            System.out.println("No complaints found.");
            return;
        }

        System.out.println("--- All Complaints ---");
        System.out.printf("%-6s %-8s %-18s %-34s %-11s %-20s%n",
                "ID", "Res.ID", "Resident", "Description", "Resolved?", "Created");
        for (Complaint c : complaints) {
            String desc = c.getDescription().length() > 31
                    ? c.getDescription().substring(0, 28) + "..." : c.getDescription();
            System.out.printf("%-6d %-8d %-18s %-34s %-11s %-20s%n",
                    c.getId(), c.getResidentId(), c.getResidentName(), desc,
                    c.getStatus().equals("RESOLVED") ? "YES" : "NO", c.getCreatedAt());
        }

        System.out.println("\n--- Pending Complaints (filtered using a lambda expression) ---");
        long pendingCount = complaints.stream()
                .filter(c -> c.getStatus().equals("OPEN"))
                .peek(c -> System.out.println("Complaint #" + c.getId() + " | "
                        + c.getResidentName() + " | " + c.getDescription()))
                .count();
        if (pendingCount == 0) System.out.println("No pending complaints.");
        else System.out.println("Total pending complaints: " + pendingCount);
    }

    // 8. Resolve complaint by updating its status (no staff columns)
    private static void resolveComplaint() throws SQLException, InvalidInputException, RecordNotFoundException {
        int id = readPositiveInt("Complaint ID to resolve: ");
        try (PreparedStatement ps = con.prepareStatement("SELECT status FROM Complaint WHERE complaint_id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new RecordNotFoundException("Complaint ID " + id + " does not exist.");
                if (rs.getString("status").equals("RESOLVED")) {
                    System.out.println("This complaint is already resolved."); return;
                }
            }
        }
        try (PreparedStatement ps = con.prepareStatement("UPDATE Complaint SET status='RESOLVED',resolved_at=CURRENT_TIMESTAMP WHERE complaint_id=?")) {
            ps.setInt(1, id); ps.executeUpdate();
        }
        System.out.println("Complaint marked as RESOLVED. View option 7 to see the updated column.");
    }

    // 9. Monthly collection report.
    // SQL GROUP BY calculates each month's totals; ArrayList stores the rows,
    // and the Stream API formats the report.
    private static void collectionReport() throws SQLException {
        String sql = "SELECT payment_month, COUNT(*) AS payment_count, " +
                "COALESCE(SUM(amount),0) AS total " +
                "FROM MaintenancePayment WHERE status='PAID' " +
                "GROUP BY payment_month ORDER BY payment_month";

        List<MonthlyCollection> monthlyCollections = new ArrayList<>();
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                monthlyCollections.add(new MonthlyCollection(
                        rs.getString("payment_month"),
                        rs.getInt("payment_count"),
                        rs.getBigDecimal("total")));
            }
        }

        System.out.println("--- Monthly Maintenance Collection Report ---");
        if (monthlyCollections.isEmpty()) {
            System.out.println("No maintenance payments have been recorded.");
            return;
        }

        System.out.printf("%-15s %-18s %-20s%n", "Month", "Payment Count", "Total Collected");
        monthlyCollections.stream()
                .map(m -> String.format("%-15s %-18d Rs. %-16s",
                        m.getMonth(), m.getPaymentCount(), m.getTotal()))
                .forEach(System.out::println);

        BigDecimal grandTotal = monthlyCollections.stream()
                .map(MonthlyCollection::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        System.out.println("Grand total collected: Rs. " + grandTotal);
    }

    // 10. Remove resident and dependent rows atomically.
    private static void removeResident() throws SQLException, InvalidInputException, RecordNotFoundException {
        int id = readPositiveInt("Resident ID to remove: ");
        checkResident(id);
        String confirm = readRequired("This deletes the resident, their payments and complaints. Type YES to confirm: ");
        if (!confirm.equalsIgnoreCase("YES")) { System.out.println("Removal cancelled."); return; }

        boolean oldAutoCommit = con.getAutoCommit();
        try {
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM MaintenancePayment WHERE resident_id=?")) {
                ps.setInt(1, id); ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM Complaint WHERE resident_id=?")) {
                ps.setInt(1, id); ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM Resident WHERE resident_id=?")) {
                ps.setInt(1, id);
                if (ps.executeUpdate() != 1) throw new SQLException("Resident deletion did not complete.");
            }
            con.commit();
            System.out.println("Resident and linked payment/complaint records removed successfully.");
        } catch (SQLException e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(oldAutoCommit);
        }
    }
}
