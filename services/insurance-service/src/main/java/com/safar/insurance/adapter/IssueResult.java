package com.safar.insurance.adapter;

public record IssueResult(
        String externalPolicyId,             // provider's policy / certificate number
        String certificateUrl,               // provider-hosted PDF cert
        long premiumPaise,                   // confirmed premium (may differ slightly from quote)
        long sumInsuredPaise,
        String currency,
        String providerStatus                // raw provider status string
) {}
