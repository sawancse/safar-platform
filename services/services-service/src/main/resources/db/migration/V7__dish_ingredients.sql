-- Dish-level ingredients (Coox-style shopping list feature)
CREATE TABLE IF NOT EXISTS chefs.dish_ingredients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    menu_item_id UUID NOT NULL REFERENCES chefs.menu_items(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    quantity NUMERIC(10,2),
    unit VARCHAR(30),
    category VARCHAR(50) NOT NULL DEFAULT 'GROCERY',
    is_optional BOOLEAN DEFAULT FALSE,
    notes VARCHAR(300),
    sort_order INT DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_dish_ingredients_menu_item ON chefs.dish_ingredients(menu_item_id);

COMMENT ON TABLE chefs.dish_ingredients IS 'Per-dish ingredient list. Quantities are per serving (1 person). Scale by guest count for shopping list.';
COMMENT ON COLUMN chefs.dish_ingredients.category IS 'GROCERY, FRESH_PRODUCE, SPICES, DAIRY, CONDIMENTS, MEAT_FISH, MISC';
COMMENT ON COLUMN chefs.dish_ingredients.unit IS 'g, kg, ml, L, pcs, tbsp, tsp, cup, bunch, as_needed';
