-- ============================================================================
-- V83: Plotted-development support for builder projects
-- ============================================================================
-- Until now builder_projects assumed apartment/villa towers — every
-- project_unit_types row required a BHK + carpet area, which doesn't fit
-- plotted layouts ("Sunshine Acres — 120 plots, 1200-4800 sqft").
--
-- This migration adds:
--   1. project_type discriminator on builder_projects (default keeps existing
--      rows on APARTMENT_TOWNSHIP for back-compat)
--   2. unit_kind discriminator on project_unit_types + nullable BHK/carpet
--      fields so plot rows can omit them
--   3. plot-specific fields mirroring sale_properties land schema:
--      plot_area_sqft, plot_length_ft, plot_breadth_ft, corner_plot, facing
--   4. price_per_sqft_paise — primary pricing input for plotted projects;
--      base_price_paise stays on the row as the computed total (per sqft × area)
-- ============================================================================

ALTER TABLE builder_projects
    ADD COLUMN IF NOT EXISTS project_type VARCHAR(40) NOT NULL
        DEFAULT 'APARTMENT_TOWNSHIP';

ALTER TABLE project_unit_types
    ADD COLUMN IF NOT EXISTS unit_kind VARCHAR(10) NOT NULL DEFAULT 'UNIT',
    ADD COLUMN IF NOT EXISTS plot_area_sqft        INTEGER,
    ADD COLUMN IF NOT EXISTS plot_length_ft        INTEGER,
    ADD COLUMN IF NOT EXISTS plot_breadth_ft       INTEGER,
    ADD COLUMN IF NOT EXISTS corner_plot           BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS facing                VARCHAR(10),
    ADD COLUMN IF NOT EXISTS price_per_sqft_paise  BIGINT;

-- Existing apartment rows had bhk NOT NULL — relax so plot rows can omit it
ALTER TABLE project_unit_types
    ALTER COLUMN bhk DROP NOT NULL;

CREATE INDEX IF NOT EXISTS idx_builder_projects_project_type
    ON builder_projects(project_type);

CREATE INDEX IF NOT EXISTS idx_project_unit_types_unit_kind
    ON project_unit_types(unit_kind);
