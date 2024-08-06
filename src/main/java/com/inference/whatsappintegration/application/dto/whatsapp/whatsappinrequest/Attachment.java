package com.inference.whatsappintegration.application.dto.whatsapp.whatsappinrequest;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Attachment {
    private String url;
    private String name;
    private String size;
}
