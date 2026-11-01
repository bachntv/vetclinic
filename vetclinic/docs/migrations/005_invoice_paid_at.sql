BEGIN;
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS paid_at timestamp;
-- Existing PAID invoices keep NULL: their actual collection time is unknown.
COMMIT;
