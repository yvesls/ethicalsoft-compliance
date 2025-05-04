CREATE TABLE refresh_tokens (
    id SERIAL NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES user_account(user_id)
);
