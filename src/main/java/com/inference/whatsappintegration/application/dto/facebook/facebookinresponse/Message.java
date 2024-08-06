package com.inference.whatsappintegration.application.dto.facebook.facebookinresponse;

import com.inference.whatsappintegration.application.dto.facebook.facebookinrequest.Attachment;
import lombok.*;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Message {
    private String text;
    private Attachment attachment;
}
