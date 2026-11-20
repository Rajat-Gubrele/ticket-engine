-- 1. Create the parent 'shows' table
CREATE TABLE shows (
                       id VARCHAR(255) PRIMARY KEY,
                       title VARCHAR(255) NOT NULL
);

-- 2. Create the child 'seats' table
CREATE TABLE seats (
                       id BIGSERIAL PRIMARY KEY,
                       show_id VARCHAR(255) NOT NULL,
                       seat_id VARCHAR(255) NOT NULL,
                       seat_row VARCHAR(255) NOT NULL,
                       seat_number INT NOT NULL,
                       status VARCHAR(50) NOT NULL,
                       held_by_user_id VARCHAR(255),
                       hold_expires_at TIMESTAMPTZ,
                       CONSTRAINT fk_seats_show FOREIGN KEY (show_id) REFERENCES shows (id) ON DELETE CASCADE,
                       CONSTRAINT uk_seats_show_seat UNIQUE (show_id, seat_id)
);

-- 3. Composite B-Tree Index for high-throughput queries
-- Accelerates "SELECT ... WHERE show_id = ? AND status = ?"
CREATE INDEX idx_seats_show_status ON seats (show_id, status);