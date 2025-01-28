CREATE TABLE representative_roles (
    representative_id INT NOT NULL,
    role_id INT NOT NULL,
    PRIMARY KEY (representative_id, role_id),
    CONSTRAINT fk_representative FOREIGN KEY (representative_id) REFERENCES representative(representative_id),
    CONSTRAINT fk_role FOREIGN KEY (role_id) REFERENCES role(role_id)
);

ALTER TABLE representative DROP COLUMN role_id;
