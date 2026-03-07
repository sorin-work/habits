ALTER TABLE users ADD COLUMN week_start VARCHAR(10) NOT NULL DEFAULT 'MONDAY';

ALTER TABLE users ADD CONSTRAINT users_week_start_check CHECK (week_start IN ('MONDAY', 'SUNDAY'));
