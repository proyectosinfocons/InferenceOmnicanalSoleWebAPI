package com.inference.whatsappintegration.application.dto.whatsapp.whatappoutrequest;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
public class WhatsappContent {

    private String contentType;
    private String text;
    private Attachment attachment;

}