package com.safar.chef.dto;

public record MenuItemRequest(
        String name,
        String description,
        String category,
        Boolean isVeg
) {}
