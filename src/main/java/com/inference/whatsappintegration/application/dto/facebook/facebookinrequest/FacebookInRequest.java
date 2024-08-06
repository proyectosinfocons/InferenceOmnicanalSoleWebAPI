package com.inference.whatsappintegration.application.dto.facebook.facebookinrequest;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class FacebookInRequest {
    private List<Entry> entry;
    private String object;
}
