package com.safar.chef.entity.enums;

public enum KycDocumentType {
    AADHAAR,
    PAN,
    GST,
    FSSAI,                  // mandatory: cake/cook (e-commerce food sale legally requires it)
    POLICE_VERIFICATION,    // mandatory: staff-hire (entering customer homes)
    LINEAGE_PROOF,          // mandatory: pandit (parampara letter or institution cert)
    IPRS,                   // optional: singer (commercial public performance license)
    INSURANCE,              // optional: decor (electricals/heavy props)
    OTHER
}
