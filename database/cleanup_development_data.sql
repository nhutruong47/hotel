/*
  Development data cleanup helper.

  Safety:
  - SQL Server oriented.
  - Does nothing unless @ALLOW_DESTRUCTIVE_CLEANUP is set to 1.
  - Blocks obvious production-like database names.
  - Prints candidate counts before any delete.
  - Runs in a transaction and rolls back by default.

  Review the output first, then change both safety flags deliberately.
*/

SET NOCOUNT ON;

DECLARE @ALLOW_DESTRUCTIVE_CLEANUP bit = 0;
DECLARE @COMMIT_CHANGES bit = 0;
DECLARE @databaseName sysname = DB_NAME();

IF @databaseName LIKE '%prod%' OR @databaseName LIKE '%production%'
BEGIN
    THROW 50000, 'Refusing to run cleanup on a production-like database name.', 1;
END;

PRINT 'Database: ' + @databaseName;
PRINT 'Mode: report-only unless @ALLOW_DESTRUCTIVE_CLEANUP = 1 and @COMMIT_CHANGES = 1';

BEGIN TRANSACTION;

IF OBJECT_ID('dbo.users', 'U') IS NOT NULL
BEGIN
    SELECT
        'candidate_test_users' AS check_name,
        COUNT(*) AS candidate_count
    FROM dbo.users
    WHERE username IN ('a', 'admin', 'testuser', 'testadmin')
       OR email LIKE '%@example.com'
       OR email LIKE '%@test.local';
END;

IF OBJECT_ID('dbo.bookings', 'U') IS NOT NULL
BEGIN
    SELECT
        'candidate_old_unpaid_bookings' AS check_name,
        COUNT(*) AS candidate_count
    FROM dbo.bookings
    WHERE status IN ('PENDING', 'PENDING_PAYMENT', 'AWAITING_PAYMENT', 'EXPIRED')
      AND created_at < DATEADD(day, -30, SYSDATETIME());
END;

IF OBJECT_ID('dbo.contact_messages', 'U') IS NOT NULL
BEGIN
    SELECT
        'candidate_test_contact_messages' AS check_name,
        COUNT(*) AS candidate_count
    FROM dbo.contact_messages
    WHERE email LIKE '%@example.com'
       OR email LIKE '%@test.local'
       OR subject LIKE '%test%';
END;

IF @ALLOW_DESTRUCTIVE_CLEANUP = 1
BEGIN
    PRINT 'Destructive cleanup flag enabled.';

    /*
      Add reviewed DELETE statements here after checking candidate counts.
      Keep predicates narrow and environment-specific.
    */

    IF @COMMIT_CHANGES = 1
    BEGIN
        COMMIT TRANSACTION;
        PRINT 'Cleanup committed.';
    END
    ELSE
    BEGIN
        ROLLBACK TRANSACTION;
        PRINT 'Cleanup rolled back because @COMMIT_CHANGES = 0.';
    END;
END
ELSE
BEGIN
    ROLLBACK TRANSACTION;
    PRINT 'Report-only complete. No data changed.';
END;
