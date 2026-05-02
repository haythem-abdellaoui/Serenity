-- ====================================================================
-- Migration script: Prescription refactoring
-- Run this BEFORE restarting derbelmicroservice.
-- ====================================================================

-- 1. Drop old columns from prescriptions table
ALTER TABLE prescriptions DROP COLUMN IF EXISTS medication_name;
ALTER TABLE prescriptions DROP COLUMN IF EXISTS medications_data;
ALTER TABLE prescriptions DROP COLUMN IF EXISTS dosage;
ALTER TABLE prescriptions DROP COLUMN IF EXISTS frequency;
ALTER TABLE prescriptions DROP COLUMN IF EXISTS start_date;
ALTER TABLE prescriptions DROP COLUMN IF EXISTS end_date;
ALTER TABLE prescriptions DROP COLUMN IF EXISTS instructions;
ALTER TABLE prescriptions DROP COLUMN IF EXISTS quantity;

-- Note: 'status' column is kept (still used in the new model).

-- 2. Hibernate will auto-create these tables on startup:
--    - medicines
--    - prescription_items
-- No manual action needed for new tables with ddl-auto=update.
