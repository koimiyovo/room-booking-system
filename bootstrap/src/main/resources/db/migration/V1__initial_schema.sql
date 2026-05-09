CREATE TABLE users (
    id                UUID         NOT NULL,
    name              VARCHAR(255) NOT NULL,
    email             VARCHAR(255) NOT NULL,
    password          VARCHAR(255) NOT NULL,
    role              VARCHAR(50)  NOT NULL,
    registration_date TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE TABLE user_status_history (
    id      UUID         NOT NULL,
    user_id UUID         NOT NULL,
    status  VARCHAR(50)  NOT NULL,
    since   TIMESTAMPTZ  NOT NULL,
    until   TIMESTAMPTZ,
    reason  VARCHAR(255),
    CONSTRAINT pk_user_status_history PRIMARY KEY (id),
    CONSTRAINT fk_user_status_history_user_id FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_user_status_history_user_id ON user_status_history (user_id);
CREATE INDEX idx_user_status_history_until   ON user_status_history (until);

CREATE TABLE rooms (
    id                  UUID         NOT NULL,
    name                VARCHAR(255) NOT NULL,
    capacity            INT          NOT NULL,
    requires_validation BOOLEAN      NOT NULL,
    created_by          UUID         NOT NULL,
    CONSTRAINT pk_rooms PRIMARY KEY (id),
    CONSTRAINT fk_room_created_by FOREIGN KEY (created_by) REFERENCES users (id)
);

CREATE TABLE bookings (
    id                UUID         NOT NULL,
    room_id           UUID         NOT NULL,
    user_id           UUID         NOT NULL,
    start_date        DATE         NOT NULL,
    end_date          DATE         NOT NULL,
    number_of_people  INT          NOT NULL,
    special_requests  TEXT,
    status            VARCHAR(50)  NOT NULL,
    status_since      TIMESTAMPTZ  NOT NULL,
    status_changed_by UUID,
    status_reason     VARCHAR(255),
    CONSTRAINT pk_bookings PRIMARY KEY (id),
    CONSTRAINT fk_booking_room_id           FOREIGN KEY (room_id)           REFERENCES rooms (id),
    CONSTRAINT fk_booking_user_id           FOREIGN KEY (user_id)           REFERENCES users (id),
    CONSTRAINT fk_booking_status_changed_by FOREIGN KEY (status_changed_by) REFERENCES users (id)
);

CREATE INDEX idx_bookings_room_id ON bookings (room_id);
CREATE INDEX idx_bookings_user_id ON bookings (user_id);

CREATE TABLE booking_status_history (
    id         UUID        NOT NULL,
    booking_id UUID        NOT NULL,
    status     VARCHAR(50) NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL,
    changed_by UUID,
    reason     VARCHAR(255),
    CONSTRAINT pk_booking_status_history PRIMARY KEY (id),
    CONSTRAINT fk_bsh_booking_id FOREIGN KEY (booking_id) REFERENCES bookings (id),
    CONSTRAINT fk_bsh_changed_by FOREIGN KEY (changed_by) REFERENCES users (id)
);

CREATE INDEX idx_bsh_booking_id ON booking_status_history (booking_id);
