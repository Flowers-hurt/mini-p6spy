CREATE DATABASE IF NOT EXISTS mini_p6spy;
USE mini_p6spy;
DROP TABLE IF EXISTS user_demo;
CREATE TABLE user_demo (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64),
    age INT
);
INSERT INTO user_demo(name, age) VALUES ('Alice', 20), ('Bob', 25);

