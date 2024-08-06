package com.inference.whatsappintegration.application.dto.facebook.facebookoutresponse;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FacebookOutResponse {

    @JsonProperty("recipient_id")
    private String recipientId;

    @JsonProperty("message_id")
    private String messageId;
}
