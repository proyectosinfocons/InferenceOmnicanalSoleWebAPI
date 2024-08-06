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
public class Entry {
    private String id;
    private Long time;
    private List<Messaging> messaging;
}
