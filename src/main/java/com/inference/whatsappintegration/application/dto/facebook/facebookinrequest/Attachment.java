package com.inference.whatsappintegration.application.dto.facebook.facebookinrequest;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Attachment {
    private String type;
    private Payload payload;
}
