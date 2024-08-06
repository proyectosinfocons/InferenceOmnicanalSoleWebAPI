package com.inference.whatsappintegration.application.mapper;

import com.inference.whatsappintegration.application.dto.webwidget.webwidgetinrequest.WebWidgetIncomingMessageRequestDTO;
import com.inference.whatsappintegration.application.dto.webwidget.webwidgetoutresponse.WebWidgetIncomingMessageResponseDTO;
import com.inference.whatsappintegration.domain.model.Conversation;
import com.inference.whatsappintegration.infrastructure.persistence.entity.Sessions;
import com.inference.whatsappintegration.util.Constants;
import com.inference.whatsappintegration.util.enums.EnumConversationStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class WidgetMapper {

    @Value("${property.redis.defaultExpirationTime}")
    private long defaultExpirationTime;

    public Sessions widgetReceiveSession(WebWidgetIncomingMessageRequestDTO webWidgetIncomingMessageRequestDTO){
        return Sessions.builder().identifier(webWidgetIncomingMessageRequestDTO.getSessionId()+ "-"
                        + webWidgetIncomingMessageRequestDTO.getSubject())
                .widgetId(webWidgetIncomingMessageRequestDTO.getSessionId())
                .digitalChannelType(Constants.WIDGET_SUBJECT_DEFAULT)
                .expiration(defaultExpirationTime).build();
    }

    public WebWidgetIncomingMessageResponseDTO webWidgetIncomingMessageResponseDTO(Conversation conversation, Sessions incomingSession){
        String textMessage = conversation.getStatus() == EnumConversationStatus.TRANSFER_AGENT ?
                conversation.getMessageText() :
                conversation.getMessageResponse();
        return WebWidgetIncomingMessageResponseDTO.builder().contentMessage(textMessage)
                .subject(conversation.getImSubject())
                .conversationId(conversation.getConversationId())
                .receiverId(conversation.getClientId())
                .sessionId(incomingSession.getWidgetId())
                .build();
    }

}