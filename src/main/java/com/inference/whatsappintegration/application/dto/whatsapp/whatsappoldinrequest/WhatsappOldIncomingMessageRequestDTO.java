package com.inference.whatsappintegration.application.dto.whatsapp.whatsappoldinrequest;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class WhatsappOldIncomingMessageRequestDTO {
    private Long id;
    private String imSubject;
    private String address;
    private String receivedAt;
    private String imType;
    private String contentType;
    private String text;
    private String caption;
    private String attachmentUrl;
    private String attachmentName;
    private Double longitude;
    private Double latitude;
    private String payload;
    private String locationAddress;
    private String userName;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private String apiKeyId;
    private String historyMessages;
    private Integer imSubjectId;
}
