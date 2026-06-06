DELETE FROM member_stage_compliance_result
WHERE result_id IN (
    SELECT result_id FROM questionnaire_result
    WHERE questionnaire_id IN (3, 4, 5, 13)
);

DELETE FROM member_compliance_result
WHERE result_id IN (
    SELECT result_id FROM questionnaire_result
    WHERE questionnaire_id IN (3, 4, 5, 13)
);

DELETE FROM questionnaire_result WHERE questionnaire_id IN (3, 4, 5, 13);
DELETE FROM project_isep_result    WHERE project_id IN (2, 7);

INSERT INTO questionnaire_result (
    project_id, questionnaire_id, isep, band,
    team_simple_avg, team_std_dev, calculated_at,
    ethics_score, process_score, fairness_score, esg_score,
    ethics_debt_score, tech_debt_score
) VALUES (
    7, 13, 0.9200, 'A',
    0.8950, 0.0320, NOW() - INTERVAL '2 days',
    0.9500, 0.8800, 0.9000, 0.8700,
    0.0300, 0.0800
);

INSERT INTO member_compliance_result (result_id, representative_id, icp, band)
SELECT r.result_id, 13, 0.9000, 'A'
FROM questionnaire_result r WHERE r.questionnaire_id = 13;

INSERT INTO member_compliance_result (result_id, representative_id, icp, band)
SELECT r.result_id, 14, 0.9400, 'A'
FROM questionnaire_result r WHERE r.questionnaire_id = 13;

INSERT INTO member_stage_compliance_result (result_id, representative_id, stage_id, iem)
SELECT r.result_id, 13, 17, 0.9000
FROM questionnaire_result r WHERE r.questionnaire_id = 13;

INSERT INTO member_stage_compliance_result (result_id, representative_id, stage_id, iem)
SELECT r.result_id, 14, 17, 0.9400
FROM questionnaire_result r WHERE r.questionnaire_id = 13;

INSERT INTO questionnaire_result (
    project_id, questionnaire_id, isep, band,
    team_simple_avg, team_std_dev, calculated_at,
    ethics_score, process_score, fairness_score, esg_score,
    ethics_debt_score, tech_debt_score
) VALUES (
    2, 3, 0.5800, 'C',
    0.5600, 0.0950, NOW() - INTERVAL '14 days',
    0.5200, 0.5800, 0.4500, 0.5000,
    0.4200, 0.3800
);

INSERT INTO member_compliance_result (result_id, representative_id, icp, band)
SELECT r.result_id, 3, 0.5000, 'C'
FROM questionnaire_result r WHERE r.questionnaire_id = 3;

INSERT INTO member_compliance_result (result_id, representative_id, icp, band)
SELECT r.result_id, 4, 0.6600, 'C'
FROM questionnaire_result r WHERE r.questionnaire_id = 3;

INSERT INTO member_stage_compliance_result (result_id, representative_id, stage_id, iem)
SELECT r.result_id, 3, s, v
FROM questionnaire_result r,
     (VALUES (3, 0.4800), (4, 0.5200), (5, 0.5000), (6, 0.5100)) AS t(s, v)
WHERE r.questionnaire_id = 3;

INSERT INTO member_stage_compliance_result (result_id, representative_id, stage_id, iem)
SELECT r.result_id, 4, s, v
FROM questionnaire_result r,
     (VALUES (3, 0.6500), (4, 0.6700), (5, 0.6600), (6, 0.6800)) AS t(s, v)
WHERE r.questionnaire_id = 3;

INSERT INTO questionnaire_result (
    project_id, questionnaire_id, isep, band,
    team_simple_avg, team_std_dev, calculated_at,
    ethics_score, process_score, fairness_score, esg_score,
    ethics_debt_score, tech_debt_score
) VALUES (
    2, 4, 0.7200, 'B',
    0.7000, 0.0700, NOW() - INTERVAL '7 days',
    0.7500, 0.7000, 0.6800, 0.6500,
    0.2200, 0.2500
);

INSERT INTO member_compliance_result (result_id, representative_id, icp, band)
SELECT r.result_id, 3, 0.6500, 'C'
FROM questionnaire_result r WHERE r.questionnaire_id = 4;

INSERT INTO member_compliance_result (result_id, representative_id, icp, band)
SELECT r.result_id, 4, 0.7900, 'B'
FROM questionnaire_result r WHERE r.questionnaire_id = 4;

INSERT INTO member_stage_compliance_result (result_id, representative_id, stage_id, iem)
SELECT r.result_id, 3, s, v
FROM questionnaire_result r,
     (VALUES (3, 0.6200), (4, 0.6500), (5, 0.6400), (6, 0.6800)) AS t(s, v)
WHERE r.questionnaire_id = 4;

INSERT INTO member_stage_compliance_result (result_id, representative_id, stage_id, iem)
SELECT r.result_id, 4, s, v
FROM questionnaire_result r,
     (VALUES (3, 0.7800), (4, 0.8000), (5, 0.7900), (6, 0.8000)) AS t(s, v)
WHERE r.questionnaire_id = 4;

INSERT INTO questionnaire_result (
    project_id, questionnaire_id, isep, band,
    team_simple_avg, team_std_dev, calculated_at,
    ethics_score, process_score, fairness_score, esg_score,
    ethics_debt_score, tech_debt_score
) VALUES (
    2, 5, 0.8800, 'A',
    0.8700, 0.0420, NOW() - INTERVAL '1 day',
    0.9000, 0.8700, 0.8500, 0.8300,
    0.0700, 0.1000
);

INSERT INTO member_compliance_result (result_id, representative_id, icp, band)
SELECT r.result_id, 3, 0.8300, 'A'
FROM questionnaire_result r WHERE r.questionnaire_id = 5;

INSERT INTO member_compliance_result (result_id, representative_id, icp, band)
SELECT r.result_id, 4, 0.9300, 'A'
FROM questionnaire_result r WHERE r.questionnaire_id = 5;

INSERT INTO member_stage_compliance_result (result_id, representative_id, stage_id, iem)
SELECT r.result_id, 3, s, v
FROM questionnaire_result r,
     (VALUES (3, 0.8200), (4, 0.8400), (5, 0.8200), (6, 0.8500)) AS t(s, v)
WHERE r.questionnaire_id = 5;

INSERT INTO member_stage_compliance_result (result_id, representative_id, stage_id, iem)
SELECT r.result_id, 4, s, v
FROM questionnaire_result r,
     (VALUES (3, 0.9200), (4, 0.9400), (5, 0.9300), (6, 0.9300)) AS t(s, v)
WHERE r.questionnaire_id = 5;

INSERT INTO project_isep_result (
    project_id, isep, band, questionnaire_count, calculated_at, closed_by,
    team_simple_avg, team_std_dev,
    ethics_score, process_score, fairness_score, esg_score,
    ethics_debt_score, tech_debt_score
) VALUES (
    7, 0.9200, 'A', 1, NOW() - INTERVAL '2 days', 'Sistema (seed)',
    0.8950, 0.0320,
    0.9500, 0.8800, 0.9000, 0.8700,
    0.0300, 0.0800
);

INSERT INTO project_isep_result (
    project_id, isep, band, questionnaire_count, calculated_at, closed_by,
    team_simple_avg, team_std_dev,
    ethics_score, process_score, fairness_score, esg_score,
    ethics_debt_score, tech_debt_score
) VALUES (
    2, 0.7267, 'B', 3, NOW() - INTERVAL '1 day', 'Sistema (seed)',
    0.7100, 0.0690,
    0.7233, 0.6833, 0.6933, 0.6600,
    0.2367, 0.2433
);

SELECT
    qr.questionnaire_id,
    qr.project_id,
    qr.isep,
    qr.band,
    round(qr.ethics_debt_score * 100, 1) AS "ética_dívida_%",
    round(qr.tech_debt_score   * 100, 1) AS "técnica_dívida_%",
    qr.calculated_at
FROM questionnaire_result qr
WHERE qr.questionnaire_id IN (3, 4, 5, 13)
ORDER BY qr.project_id, qr.questionnaire_id;

SELECT
    pir.project_id,
    pir.isep,
    pir.band,
    round(pir.ethics_debt_score * 100, 1) AS "ética_dívida_%",
    round(pir.tech_debt_score   * 100, 1) AS "técnica_dívida_%",
    pir.questionnaire_count
FROM project_isep_result pir
WHERE pir.project_id IN (2, 7)
ORDER BY pir.project_id;

