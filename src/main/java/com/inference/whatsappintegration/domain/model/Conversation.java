package com.inference.whatsappintegration.domain.model;

import com.inference.whatsappintegration.application.dto.facebook.facebookinrequest.Attachment;
import com.inference.whatsappintegration.application.dto.whatsapp.whatsappinrequest.Location;
import com.inference.whatsappintegration.util.enums.EnumConversationStatus;
import lombok.*;

import java.util.List;

@Builder
@Getter
@Setter
public class Conversation {

    private String conversationId;

    private String sessionId;
    private String clientId;
    private String imSubject;
    private String clientName;
    private String messageText;
    private String messageAttachment;
    private Location location;
    private String messageResponse;
    private String agentSurvey;
    private String messageTextType;
    private String subjectId;
    private String cascadeId;
    private String widgetId;
    private String channel;
    private String nodeCode;
    private String channelType;
    private List<Attachment> attachmentList;
    private EnumConversationStatus status;
    private TransferAgentInformation transferAgentInformation;

}

