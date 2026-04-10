package com.safar.chef.dto;

import java.util.List;
import java.util.UUID;

public record MatchChefsRequest(
        List<UUID> dishIds
) {}
