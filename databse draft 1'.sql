

CREATE TABLE STUDENT (
    student_id INT PRIMARY KEY AUTO_INCREMENT,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL
);

CREATE TABLE TEAM (
    team_id INT PRIMARY KEY AUTO_INCREMENT,
    team_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    capacity INT NOT NULL,
    created_by INT NOT NULL,
    CONSTRAINT fk_team_creator
        FOREIGN KEY (created_by)
        REFERENCES STUDENT(student_id),
    CONSTRAINT chk_team_capacity
        CHECK (capacity > 0)
);

CREATE TABLE JOIN_REQUEST (
    request_id INT PRIMARY KEY AUTO_INCREMENT,
    student_id INT NOT NULL,
    team_id INT NOT NULL,
    status ENUM('Pending', 'Accepted', 'Rejected')
        NOT NULL DEFAULT 'Pending',
    requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_request_student
        FOREIGN KEY (student_id)
        REFERENCES STUDENT(student_id),
    CONSTRAINT fk_request_team
        FOREIGN KEY (team_id)
        REFERENCES TEAM(team_id),
    CONSTRAINT unique_student_team_request
        UNIQUE (student_id, team_id)
);

CREATE TABLE TEAM_MEMBER (
    team_id INT NOT NULL,
    student_id INT NOT NULL,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    role VARCHAR(30) DEFAULT 'Member',
    PRIMARY KEY (team_id, student_id),
    CONSTRAINT fk_member_team
        FOREIGN KEY (team_id)
        REFERENCES TEAM(team_id),
    CONSTRAINT fk_member_student
        FOREIGN KEY (student_id)
        REFERENCES STUDENT(student_id)
);

INSERT INTO STUDENT (full_name, email, password_hash)
VALUES
('Krish Rana', 'krish@example.com', 'demo123'),
('Aditi Sharma', 'aditi@example.com', 'demo123'),
('Arjun Singh', 'arjun@example.com', 'demo123');

INSERT INTO TEAM (team_name, description, capacity, created_by)
VALUES (
    'skill match',
    'PBL 5TH SEMESTER',
    4,
    1
);

INSERT INTO TEAM_MEMBER (team_id, student_id, role)
VALUES (1, 1, 'Leader');

INSERT INTO JOIN_REQUEST (student_id, team_id, status)
VALUES (2, 1, 'Pending');

SELECT * FROM STUDENT;
SELECT * FROM TEAM_MEMBER;
SELECT * FROM JOIN_REQUEST;
SELECT * FROM TEAM;


SHOW TABLES;

DESCRIBE STUDENT;
DESCRIBE TEAM;
DESCRIBE JOIN_REQUEST;
DESCRIBE TEAM_MEMBER;