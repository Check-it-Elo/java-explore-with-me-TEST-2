-- Users
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(254) NOT NULL UNIQUE
);

-- Categories
CREATE TABLE IF NOT EXISTS categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

-- Locations
CREATE TABLE IF NOT EXISTS locations (
    id BIGSERIAL PRIMARY KEY,
    lat DOUBLE PRECISION NOT NULL,
    lon DOUBLE PRECISION NOT NULL,
    CONSTRAINT chk_locations_lat CHECK (lat BETWEEN -90 AND 90),
    CONSTRAINT chk_locations_lon CHECK (lon BETWEEN -180 AND 180)
);

-- Events
CREATE TABLE IF NOT EXISTS events (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(120) NOT NULL,
    annotation VARCHAR(2000) NOT NULL,
    description TEXT NOT NULL,
    category_id BIGINT NOT NULL REFERENCES categories(id) ON DELETE RESTRICT,
    initiator_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    location_id BIGINT NOT NULL REFERENCES locations(id) ON DELETE RESTRICT,
    event_date TIMESTAMP NOT NULL,
    created_on TIMESTAMP NOT NULL DEFAULT NOW(),
    published_on TIMESTAMP NULL,
    paid BOOLEAN NOT NULL DEFAULT FALSE,
    participant_limit INT NOT NULL DEFAULT 0,
    request_moderation BOOLEAN NOT NULL DEFAULT TRUE,
    state VARCHAR(20) NOT NULL, -- PENDING/PUBLISHED/CANCELED
    CONSTRAINT chk_events_title_len CHECK (char_length(title) >= 3),
    CONSTRAINT chk_events_annotation_len CHECK (char_length(annotation) >= 20),
    CONSTRAINT chk_events_desc_len CHECK (char_length(description) >= 20),
    CONSTRAINT chk_events_participant_limit CHECK (participant_limit >= 0)
);

-- Participation Requests
CREATE TABLE IF NOT EXISTS participation_requests (
    id BIGSERIAL PRIMARY KEY,
    created TIMESTAMP NOT NULL DEFAULT NOW(),
    event_id BIGINT NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    requester_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL, -- PENDING/CONFIRMED/REJECTED/CANCELED
    CONSTRAINT uq_request_unique UNIQUE (event_id, requester_id)
);

-- Compilations
CREATE TABLE IF NOT EXISTS compilations (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    pinned BOOLEAN NOT NULL DEFAULT FALSE
);

-- Compilation <-> Event (M:M)
CREATE TABLE IF NOT EXISTS compilation_events (
    compilation_id BIGINT NOT NULL REFERENCES compilations(id) ON DELETE CASCADE,
    event_id BIGINT NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    PRIMARY KEY (compilation_id, event_id)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_events_category ON events(category_id);
CREATE INDEX IF NOT EXISTS idx_events_initiator ON events(initiator_id);
CREATE INDEX IF NOT EXISTS idx_events_state ON events(state);
CREATE INDEX IF NOT EXISTS idx_events_paid ON events(paid);
CREATE INDEX IF NOT EXISTS idx_events_event_date ON events(event_date);
CREATE INDEX IF NOT EXISTS idx_events_published_on ON events(published_on);

CREATE INDEX IF NOT EXISTS idx_requests_event ON participation_requests(event_id);
CREATE INDEX IF NOT EXISTS idx_requests_requester ON participation_requests(requester_id);

-- Simple text search helpers
CREATE INDEX IF NOT EXISTS idx_events_title_like ON events (LOWER(title));
CREATE INDEX IF NOT EXISTS idx_events_annotation_like ON events (LOWER(annotation));
CREATE INDEX IF NOT EXISTS idx_events_description_like ON events (LOWER(description));