package com.inference.whatsappintegration.application.mapper;

import com.inference.whatsappintegration.application.dto.facebook.facebookinrequest.Attachment;
import com.inference.whatsappintegration.application.dto.facebook.facebookinrequest.FacebookInRequest;
import com.inference.whatsappintegration.application.dto.facebook.facebookinrequest.Payload;
import com.inference.whatsappintegration.application.dto.facebook.facebookinrequest.User;
import com.inference.whatsappintegration.application.dto.facebook.facebookinresponse.FacebookInResponse;
import com.inference.whatsappintegration.application.dto.facebook.facebookinresponse.Message;
import com.inference.whatsappintegration.application.dto.facebook.facebookinresponse.Recipient;
import com.inference.whatsappintegration.domain.model.Conversation;
import com.inference.whatsappintegration.infrastructure.persistence.entity.Sessions;
import com.inference.whatsappintegration.infrastructure.persistence.entity.WhatsappDailyConversationHistory;
import com.inference.whatsappintegration.util.Constants;
import com.inference.whatsappintegration.util.Utils;
import com.inference.whatsappintegration.util.enums.EnumConversationStatus;
import com.inference.whatsappintegration.util.enums.EnumWhatsappContentType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class FacebookMapper {

    @Value("${property.redis.defaultExpirationTime}")
    private long defaultExpirationTime;

    public Sessions facebookEventInputToSession(FacebookInRequest facebookInRequest){
        User user = facebookInRequest.getEntry().get(Constants.DEFAULT_META_ENTRY_START).getMessaging()
                .get(Constants.DEFAULT_META_ENTRY_START).getSender();
        return Sessions.builder()
                .identifier(user.getId() + "-" + Constants.FACEBOOK_CHANNEL)
                .senderId(user.getId())
                .digitalChannelType(Constants.FACEBOOK_CHANNEL)
                .build();
    }

    public List<WhatsappDailyConversationHistory> facebookConversationToHistory(Conversation conversation
            , Sessions sessions){
        List<WhatsappDailyConversationHistory> whatsappDailyConversationHistories = new ArrayList<>();
        if (conversation.getMessageText() != null && !conversation.getMessageText().equals(Constants.EMPTY_STRING)){
            whatsappDailyConversationHistories.add(
                WhatsappDailyConversationHistory.builder().conversationId(conversation.getConversationId())
                        .sender(Constants.CLIENT_DEFAULT_IDENTIFIER)
                        .receiver(sessions.getChannelType() == Constants.CHANNEL_TYPE_FIVE9_AGENT ?
                                Constants.FIVE9_RECEIVER : Constants.INFERENCE_BOT_RECEIVER)
                        .type(Constants.INBOUND_TYPE)
                        .content(conversation.getMessageText())
                        .channel(conversation.getChannelType())
                        .build()
                );
            }
        if(conversation.getAttachmentList() != null && !conversation.getAttachmentList().isEmpty()){
            for (Attachment attachment : conversation.getAttachmentList()) {
                whatsappDailyConversationHistories.add(
                        WhatsappDailyConversationHistory.builder().conversationId(conversation.getConversationId())
                                .sender(Constants.CLIENT_DEFAULT_IDENTIFIER)
                                .receiver(sessions.getChannelType() == Constants.CHANNEL_TYPE_FIVE9_AGENT ?
                                        Constants.FIVE9_RECEIVER : Constants.INFERENCE_BOT_RECEIVER)
                                .type(Constants.INBOUND_TYPE)
                                .content(attachment.getPayload().getUrl())
                                .channel(conversation.getChannelType())
                                .build()
                );
            }
        }
        return whatsappDailyConversationHistories;
    }

    public List<FacebookInResponse> sendFacebookMessagesResponse(Conversation conversation){
        List<FacebookInResponse> facebookInResponses = new ArrayList<>();
        String textMessage = conversation.getStatus() == EnumConversationStatus.TRANSFER_AGENT ?
                conversation.getMessageText() :
                conversation.getMessageResponse();

        textMessage = Utils.replaceSpecialCharacters(textMessage);
        List<String> listTextMessages = Utils.splitSpecialMessageLinks(textMessage);

        for (String textMessageIterate : listTextMessages) {
            String type = obtainType(textMessageIterate);
            if (type != null) {
                facebookInResponses.add(
                        FacebookInResponse.builder()
                                .recipient(Recipient.builder().id(conversation.getClientId()).build())
                                .messagingType(Constants.DEFAULT_FACEBOOK_MESSAGING_RESPONSE_TYPE)
                                .notificationType(Constants.DEFAULT_FACEBOOK_NOTIFICATION_TYPE)
                                .message(Message.builder()
                                        .text(null)
                                        .attachment(Attachment.builder()
                                                .type(type)
                                                .payload(Payload.builder()
                                                        .url(Utils.extractUrlFromMessage(textMessageIterate))
                                                        .build())
                                                .build())
                                        .build())
                                .build()
                );
            } else {
                facebookInResponses.add(
                        FacebookInResponse.builder()
                                .recipient(Recipient.builder().id(conversation.getClientId()).build())
                                .messagingType(Constants.DEFAULT_FACEBOOK_MESSAGING_RESPONSE_TYPE)
                                .notificationType(Constants.DEFAULT_FACEBOOK_NOTIFICATION_TYPE)
                                .message(Message.builder()
                                        .text(textMessageIterate).build())
                                .build()
                );
            }

        }
        return facebookInResponses;
    }

    private String obtainType(String textMessage) {
        String matchingContentType = Utils.findMatchingContentType(textMessage);
        if (matchingContentType != null) {
            EnumWhatsappContentType contentType = EnumWhatsappContentType.fromFieldName(matchingContentType);
            switch (contentType) {
                case AUDIO:
                    return "audio";
                case IMAGE:
                    return "image";
                case VIDEO:
                    return "video";
                case DOCUMENT:
                    return "file";
                default:
                    return null;
            }
        } else {
            return null;
        }
    }


}
