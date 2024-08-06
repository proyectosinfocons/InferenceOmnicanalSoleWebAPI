package com.inference.whatsappintegration.application.dto.whatsapp.whatappoutrequest;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class Attachment {
    private String url;
    private String name;
}
