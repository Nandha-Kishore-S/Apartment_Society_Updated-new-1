CREATE DATABASE IF NOT EXISTS apartment_society;
USE apartment_society;

-- Required tables for this revised version.
-- Back up an existing database before changing its structure.
-- CREATE TABLE IF NOT EXISTS does not alter columns in an older table.

CREATE TABLE IF NOT EXISTS Apartment (
    apartment_id INT AUTO_INCREMENT PRIMARY KEY,
    flat_number VARCHAR(20) NOT NULL UNIQUE,
    block_name VARCHAR(30) NOT NULL,
    monthly_maintenance DECIMAL(10,2) NOT NULL CHECK (monthly_maintenance > 0)
);

CREATE TABLE IF NOT EXISTS Resident (
    resident_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    phone VARCHAR(15) NOT NULL,
    email VARCHAR(120) NOT NULL,
    apartment_id INT NOT NULL,
    CONSTRAINT fk_resident_apartment FOREIGN KEY (apartment_id)
        REFERENCES Apartment(apartment_id)
);

CREATE TABLE IF NOT EXISTS MaintenancePayment (
    payment_id INT AUTO_INCREMENT PRIMARY KEY,
    resident_id INT NOT NULL,
    payment_month CHAR(7) NOT NULL,
    amount DECIMAL(10,2) NOT NULL CHECK (amount > 0),
    payment_date DATE NOT NULL,
    status ENUM('PAID') NOT NULL DEFAULT 'PAID',
    CONSTRAINT uq_resident_payment_month UNIQUE (resident_id, payment_month),
    CONSTRAINT fk_payment_resident FOREIGN KEY (resident_id)
        REFERENCES Resident(resident_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS Complaint (
    complaint_id INT AUTO_INCREMENT PRIMARY KEY,
    resident_id INT NOT NULL,
    description VARCHAR(500) NOT NULL,
    status ENUM('OPEN','RESOLVED') NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP NULL DEFAULT NULL,
    CONSTRAINT fk_complaint_resident FOREIGN KEY (resident_id)
        REFERENCES Resident(resident_id) ON DELETE CASCADE
);

INSERT INTO Apartment(flat_number,block_name,monthly_maintenance)
SELECT 'A-101','A',2500.00 WHERE NOT EXISTS (SELECT 1 FROM Apartment WHERE flat_number='A-101');
INSERT INTO Apartment(flat_number,block_name,monthly_maintenance)
SELECT 'A-102','A',2500.00 WHERE NOT EXISTS (SELECT 1 FROM Apartment WHERE flat_number='A-102');
INSERT INTO Apartment(flat_number,block_name,monthly_maintenance)
SELECT 'B-201','B',3000.00 WHERE NOT EXISTS (SELECT 1 FROM Apartment WHERE flat_number='B-201');
