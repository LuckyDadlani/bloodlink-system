package com.bloodlink.bloodlink.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum BloodGroup {
    O_POS,
    O_NEG,
    A_POS,
    A_NEG,
    B_POS,
    B_NEG,
    AB_POS,
    AB_NEG;

    @JsonValue
    public String toJson() {
        return this.name();
    }
}