CREATE TABLE recovery_code (
   id BIGSERIAL PRIMARY KEY,
   email VARCHAR(255) NOT NULL,
   code VARCHAR(6) NOT NULL,
   expiration TIMESTAMP NOT NULL
);
