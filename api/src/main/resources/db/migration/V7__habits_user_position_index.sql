-- The habit list and the today view both read a user's habits in position order.
CREATE INDEX habits_user_position_idx ON habits (user_id, position);
