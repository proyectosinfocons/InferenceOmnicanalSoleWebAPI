package com.inference.whatsappintegration.application.dto.whatsapp.whatsappinrequest;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class WhatsappIncomingMessageRequestDTO {

    private Long id;
    private String subject;
    private Long subjectId;
    private Subscriber subscriber;
    private UserInfo userInfo;
    private MessageContent messageContent;
    private String receivedAt;
}
