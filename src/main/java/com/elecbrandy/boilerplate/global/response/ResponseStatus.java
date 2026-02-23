package com.elecbrandy.boilerplate.global.response;

public enum ResponseStatus {
    SUCCESS("success"),
    FAIL("fail"),
    ERROR("error");

    private final String value;

    ResponseStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
