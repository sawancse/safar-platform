-- ============================================================================
-- V84: Builder contact phone for WhatsApp / call-back deep links
-- ============================================================================
-- Until now builder_projects had no phone column, so the project page's
-- "WhatsApp Builder" button was hardcoded to a placeholder number. This
-- migration adds builder_phone so the wizard can capture it and the buyer
-- page can deep-link directly to the right builder. Optional column —
-- existing rows stay NULL until builders re-edit; the buyer page falls back
-- to the inquiry form when phone is missing.
-- ============================================================================

ALTER TABLE builder_projects
    ADD COLUMN IF NOT EXISTS builder_phone VARCHAR(15);
