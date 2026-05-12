ALTER TABLE booking_status_history ADD COLUMN until TIMESTAMPTZ;

-- Close all non-latest history entries per booking
UPDATE booking_status_history h
SET until = (
    SELECT MIN(h2.changed_at)
    FROM booking_status_history h2
    WHERE h2.booking_id = h.booking_id
      AND h2.changed_at > h.changed_at
)
WHERE EXISTS (
    SELECT 1 FROM booking_status_history h2
    WHERE h2.booking_id = h.booking_id
      AND h2.changed_at > h.changed_at
);

ALTER TABLE bookings DROP COLUMN status;
ALTER TABLE bookings DROP COLUMN status_since;
ALTER TABLE bookings DROP COLUMN status_changed_by;
ALTER TABLE bookings DROP COLUMN status_reason;
