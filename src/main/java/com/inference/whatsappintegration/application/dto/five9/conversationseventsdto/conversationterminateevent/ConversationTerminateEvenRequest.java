package com.inference.whatsappintegration.application.dto.five9.conversationseventsdto.conversationterminateevent;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class ConversationTerminateEvenRequest {

    private String correlationId;

    private String displayName;

    private String externalId;

    private String eventSerialNumber;

    private String timestamp;

    private String userId;

}
