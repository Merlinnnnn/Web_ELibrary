package com.spkt.libraSys.service.drm;

public class LicenseRevokedException extends RuntimeException {
    public LicenseRevokedException(String message) {
        super(message);
    }
}