package com.inference.whatsappintegration.application.dto.five9.conversationsdto.five9conversationsendmessagerequest;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SendConversationMessageRequest {

    private String message;

    private String externalId;

    private String messageType;

}
