-- Step 1: Drop the old constraint that is causing the conflict.
-- The name 'CK__bookings__status__32767D0B' is taken from your error message.
-- If your actual constraint name is different, please update it here.
ALTER TABLE bookings DROP CONSTRAINT CK__bookings__status__32767D0B;
GO

-- Step 2: Add a new constraint with the updated list of valid statuses, including 'RENTING'.
-- This ensures the database schema matches the Java BookingStatus enum.
ALTER TABLE bookings
ADD CONSTRAINT CK_bookings_status CHECK (status IN (
    'AWAITING_PAYMENT',
    'CONFIRMED',
    'RENTING',
    'COMPLETED',
    'CANCELLED',
    'REJECTED'
));
GO

PRINT 'Database constraint for bookings.status has been successfully updated.';
