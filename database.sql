CREATE DATABASE buses_timetable; 
USE buses_timetable;
CREATE TABLE routes(
	id INT AUTO_INCREMENT PRIMARY KEY, 
    route_number VARCHAR(10) UNIQUE,
    bus_type VARCHAR(50),
    destination VARCHAR(100), 
    departure_time TIME, 
    arrival_time TIME
);

INSERT INTO routes (route_number, bus_type, destination, departure_time, arrival_time) VALUES
('101', 'МАЗ', 'Брест', '06:00:00', '11:00:00'),
('103', 'Мерседес', 'Минск', '10:00:00', '15:30:00'),
('104', 'МАЗ', 'Гродно', '14:00:00', '20:00:00'),
('108', 'Мерседес', 'Минск', '13:00:00', '17:30:00');

SELECT * FROM routes WHERE arrival_time <= SUBTIME('23:00:00', '12:00:00'); 
USE buses_timetable;

CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    login VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(64) NOT NULL,
    role TINYINT NOT NULL DEFAULT 0
);

USE buses_timetable;

CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    login VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(64) NOT NULL,
    role TINYINT NOT NULL DEFAULT 0
);

INSERT IGNORE INTO users (login, password, role) VALUES 
('admin', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918', 1),
('user',  '5994471abb01112afcc18159f6cc74b4f511b99806da59b3caf5a9c173cacfc5', 0);
