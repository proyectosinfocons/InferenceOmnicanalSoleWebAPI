package com.inference.whatsappintegration.application.dto.webwidget.webwidgetoutresponse;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class WebWidgetIncomingMessageResponseDTO {
    private String subject;
    private String receiverId;
    private String sessionId;
    private String contentMessage;
    private String conversationId;
}
