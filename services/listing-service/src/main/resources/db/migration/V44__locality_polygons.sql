-- Locality Polygons: OpenStreetMap boundary data for precise area search
CREATE TABLE IF NOT EXISTS listings.locality_polygons (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100),
    osm_id VARCHAR(50),
    boundary_geo_json TEXT,
    centroid_lat DOUBLE PRECISION,
    centroid_lng DOUBLE PRECISION,
    polygon_type VARCHAR(30) NOT NULL DEFAULT 'NEIGHBORHOOD',
    listing_count INT NOT NULL DEFAULT 0,
    area_km2 DOUBLE PRECISION,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    last_updated_from_osm TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_lp_city ON listings.locality_polygons(city);
CREATE INDEX idx_lp_name ON listings.locality_polygons(name);
CREATE INDEX idx_lp_active ON listings.locality_polygons(active);
CREATE UNIQUE INDEX idx_lp_name_city ON listings.locality_polygons(name, city);
