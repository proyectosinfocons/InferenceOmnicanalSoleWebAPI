package com.inference.whatsappintegration.application.dto.webwidget.webwidgetinrequest;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class WebWidgetIncomingMessageRequestDTO {
    private String subject;
    private String receiverId;
    private String sessionId;
    private String contentMessage;
}
