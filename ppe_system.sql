compliance_records CREATE DATABASE ppe_system;
USE ppe_system;
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50),usersid
    password VARCHAR(50)
);
CREATE TABLE compliance_records (
    id INT AUTO_INCREMENT PRIMARY KEY,
    image_name VARCHAR(100),
    result VARCHAR(50),
    confidence FLOAT,
    date DATETIME,
    image_path VARCHAR(255)
);

INSERT INTO users (username, password)
VALUES ('admin', 'admin123');

INSERT INTO compliance_records (image_name, result, confidence, date, image_path)
VALUES ('img1.jpg', 'Helmet Detected', 0.95, NOW(), '/images/img1.jpg');

SELECT * FROM users;
SELECT * FROM compliance_records;

INSERT INTO users (username, password)
VALUES 
('admin', 'admin123'),
('user1', 'pass123');

ALTER TABLE compliance_records AUTO_INCREMENT = 1;
ALTER TABLE users AUTO_INCREMENT = 1;

