-- Manual ordering of habits. Existing habits keep their creation order.
ALTER TABLE habits ADD COLUMN position INT NOT NULL DEFAULT 0;

UPDATE habits SET position = CAST(id AS INT);
