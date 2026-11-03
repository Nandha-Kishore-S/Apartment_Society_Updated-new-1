APARTMENT SOCIETY MANAGEMENT SYSTEM
Java 17 + MySQL | Single Main.java | No Maven | CMD

SETUP
1. Open Main.java and replace YOUR_MYSQL_PASSWORD with your local MySQL password.
2. Run schema.sql in MySQL Workbench.
3. Put the MySQL Connector/J .jar file in a folder named lib beside Main.java.
4. Open CMD in the project folder and compile:
   mkdir out
   javac -cp "lib\\*" -d out Main.java
5. Run:
   java -cp "out;lib\\*" Main

MENU
1 Register Resident
2 View Apartments
3 View Residents
4 Record Maintenance Payment
5 View Pending Payments (current month)
6 Register Complaint
7 View Complaints and Pending Complaints
8 Resolve Complaint
9 Monthly Collection Report
10 Remove Resident
0 Exit

JAVA CONCEPTS USED
- OOP: Encapsulation, abstraction, inheritance and polymorphism in the model classes.
- Collections Framework: ArrayList<Complaint> stores complaints retrieved from MySQL.
- Lambda Expressions: filter(c -> c.getStatus().equals("OPEN")) identifies pending complaints.
- Stream API: streams format monthly report rows and calculate the grand total.
- JDBC: DriverManager, Connection, PreparedStatement, Statement and ResultSet connect Java to MySQL.

DBMS / SQL CONCEPTS USED
- Primary keys, foreign keys and constraints (NOT NULL, UNIQUE, CHECK, ENUM).
- CRUD operations and PreparedStatement.
- SQL JOIN: Resident and Apartment details are displayed together; Complaint and Resident details are also joined.
- GROUP BY: monthly maintenance payments are grouped by payment_month, with COUNT and SUM totals.
- Nested query: NOT EXISTS finds residents who have not paid maintenance for the current month.
- Transaction using COMMIT and ROLLBACK during resident removal.

VALIDATION
- Required fields cannot be blank.
- Email format is checked.
- Phone must contain 7-15 digits.
- IDs and payment amounts must be positive.
- Month must be YYYY-MM when recording a payment.
- A resident cannot have two payment records for the same month.

NOTES
- Payment entry records a payment; it does not transfer money.
- Pending payments are for the current month only.
- The monthly collection report displays all recorded months, grouped by month.
- Removing a resident deletes their maintenance payments and complaints; the apartment remains.
- The database uses four tables: Apartment, Resident, MaintenancePayment and Complaint.
- Back up an older database before changing its structure. CREATE TABLE IF NOT EXISTS does not update existing table structures.
