ALTER TABLE project
    ADD COLUMN owner_id BIGINT;

-- backfill owner_id using the first representative tied to each project (if any),
-- otherwise fall back to the earliest user to avoid null constraint violations
WITH first_rep AS (
    SELECT DISTINCT ON (project_id) project_id, user_id
    FROM representative
    ORDER BY project_id, representative_id
)
UPDATE project p
SET owner_id = COALESCE(
        (SELECT fr.user_id FROM first_rep fr WHERE fr.project_id = p.project_id),
        (SELECT user_id FROM user_account ORDER BY user_id LIMIT 1)
    );

ALTER TABLE project
    ALTER COLUMN owner_id SET NOT NULL,
    ADD CONSTRAINT fk_project_owner FOREIGN KEY (owner_id) REFERENCES user_account(user_id);
