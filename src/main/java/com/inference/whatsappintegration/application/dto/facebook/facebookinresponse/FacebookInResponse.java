package com.inference.whatsappintegration.application.dto.facebook.facebookinresponse;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FacebookInResponse {

    private Recipient recipient;

    @JsonProperty("messaging_type")
    private String messagingType;

    @JsonProperty("notification_type")
    private String notificationType;

    private Message message;

}
