package com.safar.chef.dto;

import java.util.List;
import java.util.UUID;

public record AssignStaffRequest(List<Assignment> assignments) {

    public record Assignment(
            UUID staffId,
            String role,
            Long ratePaise
    ) {}
}
