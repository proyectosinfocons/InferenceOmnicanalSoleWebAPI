package com.inference.whatsappintegration.application.dto.whatsapp.whatsappinrequest;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Location {
    private double longitude;
    private double latitude;
    private String locationAddress;
}
