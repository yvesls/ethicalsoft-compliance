-- STEP 1: Drop old check constraint BEFORE updating values
-- (the old constraint only accepted old enum names, so updates would be blocked)
ALTER TABLE question DROP CONSTRAINT IF EXISTS question_classification_check;

-- STEP 2: Migrate existing rows from old classification enum values to new names
UPDATE question SET classification = 'WHOLE_PROJECT' WHERE classification = 'PROJETO_INTEIRO';
UPDATE question SET classification = 'RECURRING'     WHERE classification = 'BASE_ITERACAO';
UPDATE question SET classification = 'RECURRING'     WHERE classification = 'ROTATIVA' AND type = 'BASE';
UPDATE question SET classification = 'CURRENT_STAGE' WHERE classification = 'ROTATIVA' AND type = 'CUSTOM';

-- STEP 3: Add new constraint enforcing valid type+classification combinations:
--   BASE  + WHOLE_PROJECT  -> valid (template question answered once for the project)
--   BASE  + RECURRING      -> valid (template question repeated each iteration)
--   CUSTOM + WHOLE_PROJECT -> valid (user question answered once for the project)
--   CUSTOM + RECURRING     -> valid (user question repeated each iteration)
--   CUSTOM + CURRENT_STAGE -> valid (user question for current sprint only)
--   BASE  + CURRENT_STAGE  -> INVALID (base template questions cannot be stage-specific)
ALTER TABLE question ADD CONSTRAINT question_classification_check
    CHECK (
        classification IS NULL
        OR (type = 'BASE'   AND classification IN ('WHOLE_PROJECT', 'RECURRING'))
        OR (type = 'CUSTOM' AND classification IN ('WHOLE_PROJECT', 'RECURRING', 'CURRENT_STAGE'))
    );
