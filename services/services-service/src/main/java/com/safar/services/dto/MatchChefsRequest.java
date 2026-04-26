package com.safar.services.dto;

import java.util.List;
import java.util.UUID;

public record MatchChefsRequest(
        List<UUID> dishIds
) {}
