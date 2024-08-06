package com.inference.whatsappintegration.application.service.imp;

import com.inference.whatsappintegration.application.dto.facebook.facebookinrequest.Attachment;
import com.inference.whatsappintegration.application.dto.facebook.facebookinrequest.FacebookInRequest;
import com.inference.whatsappintegration.application.dto.five9.conversationsdto.five9conversationsendmessagerequest.SendConversationMessageRequest;
import com.inference.whatsappintegration.application.mapper.ConversationMapper;
import com.inference.whatsappintegration.application.mapper.FacebookMapper;
import com.inference.whatsappintegration.domain.model.Conversation;
import com.inference.whatsappintegration.domain.service.*;
import com.inference.whatsappintegration.infrastructure.config.mdc.MdcAwareExecutor;
import com.inference.whatsappintegration.infrastructure.exception.ErrorMessages;
import com.inference.whatsappintegration.infrastructure.exception.GenericException;
import com.inference.whatsappintegration.infrastructure.persistence.entity.Five9Session;
import com.inference.whatsappintegration.infrastructure.persistence.entity.Sessions;
import com.inference.whatsappintegration.infrastructure.persistence.repository.Five9SessionRepository;
import com.inference.whatsappintegration.infrastructure.persistence.repository.SessionRepository;
import com.inference.whatsappintegration.util.Constants;
import com.inference.whatsappintegration.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FacebookServiceImp implements FacebookService {

    private static final Logger LOGGER  = LoggerFactory.getLogger(FacebookServiceImp.class);

    private FacebookMapper facebookMapper;
    private RedisService redisService;
    private ConversationMapper conversationMapper;
    private MdcAwareExecutor mdcAwareExecutor;
    private DatabaseOperationsService databaseOperationsService;
    private ConversationService conversationService;
    private SessionRepository sessionRepository;
    private Five9SessionRepository five9SessionRepository;
    private Five9ConversationService five9ConversationService;

    @Value("${property.inference.bot.disable}")
    private int botFlag;

    public FacebookServiceImp(FacebookMapper facebookMapper, RedisService redisService
            , ConversationMapper conversationMapper, MdcAwareExecutor mdcAwareExecutor
            , DatabaseOperationsService databaseOperationsService, ConversationService conversationService
            ,SessionRepository sessionRepository, Five9SessionRepository five9SessionRepository,
                              Five9ConversationService five9ConversationService){
        this.facebookMapper = facebookMapper;
        this.redisService = redisService;
        this.conversationMapper = conversationMapper;
        this.mdcAwareExecutor = mdcAwareExecutor;
        this.databaseOperationsService = databaseOperationsService;
        this.conversationService = conversationService;
        this.sessionRepository = sessionRepository;
        this.five9SessionRepository = five9SessionRepository;
        this.five9ConversationService = five9ConversationService;
    }


    @Override
    public void processReceiveInteraction(FacebookInRequest facebookInRequest) {
        LOGGER.info("Starting process receive interaction");
        try{
            Utils.logAsJson(LOGGER, facebookInRequest, "FacebookInputRequest:");
            Sessions incomingSessionTmp = facebookMapper.facebookEventInputToSession(facebookInRequest);
            Sessions incomingSession = redisService.getSessionFromRequest(incomingSessionTmp);
            Conversation conversation = conversationMapper.facebookMessageEventRequestToConversation(incomingSession,
                    facebookInRequest);
            mdcAwareExecutor.execute(() -> databaseOperationsService.processFacebookMessageEvent(conversation,
                    incomingSession));

            if (botFlag == Constants.BOT_FLAG_DEACTIVATED && incomingSession.getSessionId() == null){
                handleBotDeactivatedAndSessionIsNotPresent(conversation, incomingSession);
            } else if (botFlag == Constants.BOT_FLAG_DEACTIVATED){
                handleBotDeactivatedAndSessionIsPresent(conversation, incomingSession);
            } else {
                switch (incomingSession.getChannelType()){
                    case Constants.CHANNEL_TYPE_INFERENCE_BOT:
                        processBotInteraction(conversation, incomingSession);
                        break;
                    case Constants.CHANNEL_TYPE_FIVE9_AGENT:
                        processAgentInteraction(conversation, incomingSession);
                        break;
                    default:
                        throw new GenericException("No channel type session matching");
                }
            }

        }
        catch (Exception ex) {
            LOGGER.error("Exception while processing recieve interaction: {}", ex.getMessage());
        }  finally {
            LOGGER.info("Finish process receive interaction");
            Utils.clearMDCParameters();
        }
    }


    private void processBotInteraction(Conversation conversation, Sessions incomingSession) {
        LOGGER.info("Starting process bot interaction");
        try {
            conversation = conversationService.sendConversationToInferenceBot(conversation);
            Conversation tempConversation = conversation;
            mdcAwareExecutor.execute(() -> databaseOperationsService.insertBotMetricsInteraction(tempConversation));
            switch (conversation.getStatus()) {
                case IN_PROGRESS:
                    if (incomingSession.getSessionId() == null) {
                        incomingSession.setSessionId(conversation.getSessionId());
                    }
                    redisService.saveWithAudit(incomingSession, Constants.EXPIRATION_TIME_DEFAULT);
                    conversationService.sendConversationFacebookResponse(conversation);
                    break;
                case TRANSFER_AGENT:
                    incomingSession.setChannelType(Constants.CHANNEL_TYPE_FIVE9_AGENT);
                    incomingSession.setSessionId(null);
                    redisService.saveWithAudit(incomingSession, null);
                    if (conversation.getTransferAgentInformation() != null) {
                        conversationService.sendConversationFacebookResponse(conversation);
                    }
                    conversationService.sendConversationToFive9Agent(conversation, incomingSession);
                    break;
                case TERMINATED:
                    sessionRepository.delete(incomingSession);
                    conversationService.sendConversationFacebookResponse(conversation);
                    break;
                default:
                    throw new GenericException("No conversation status matching");
            }
        } catch (Exception ex) {
            LOGGER.error("Exception while processing bot interaction: {}", ex.getMessage());
        }  finally {
            LOGGER.info("Finish process bot interaction");
        }
    }

    private void processAgentInteraction(Conversation conversation, Sessions incomingSession){
        LOGGER.info("Starting process agent interaction");
        try {
            Five9Session five9Session = five9SessionRepository.findById(incomingSession.getSessionId()).orElseThrow(
                    () -> new GenericException(ErrorMessages.FIVE9_SESSION_NOT_PRESENT));
            mdcAwareExecutor.execute(() -> databaseOperationsService.processClientSummaryInteraction(conversation));
            if(conversation.getMessageText() != null && !conversation.getMessageText().equals(Constants.EMPTY_STRING)){
                SendConversationMessageRequest sendConversationMessageRequest =
                        conversationMapper.conversationToFive9SendConversationMessage(conversation);
                five9ConversationService.sendFive9MessageConversation(five9Session, sendConversationMessageRequest);
            }
            if (conversation.getAttachmentList() != null && !conversation.getAttachmentList().isEmpty()){
                for (Attachment attachment : conversation.getAttachmentList()){
                    SendConversationMessageRequest sendConversationMessageRequest =
                            conversationMapper.conversationToFive9SendConversationMessage(Conversation.builder()
                                            .messageText(attachment.getPayload().getUrl())
                                            .conversationId(conversation.getConversationId())
                                    .build());
                    five9ConversationService.sendFive9MessageConversation(five9Session, sendConversationMessageRequest);
                }
            }
        } catch (Exception ex) {
            LOGGER.error("Exception while processing agent interaction: {}", ex.getMessage());
        } finally {
            LOGGER.info("Finish process agent interaction");
        }
    }

    private void handleBotDeactivatedAndSessionIsNotPresent(Conversation conversation, Sessions incomingSession){
        LOGGER.info("Bot flow is deactivated and sessionId is not present, sending interaction to five 9 agent");
        try {
            if (incomingSession.getChannelType() == Constants.CHANNEL_TYPE_INFERENCE_BOT){
                incomingSession.setChannelType(Constants.CHANNEL_TYPE_FIVE9_AGENT);
                redisService.saveWithAudit(incomingSession, null);
            }
            conversationService.sendConversationToFive9Agent(conversation, incomingSession);
        } catch (Exception ex) {
            LOGGER.error("Exception while processing bot deactivated and sessionId not present: {}", ex.getMessage());
        } finally {
            LOGGER.info("Finish process bot deactivated and sessionId not present");
        }
    }

    private void handleBotDeactivatedAndSessionIsPresent(Conversation conversation, Sessions incomingSession){
        LOGGER.info("Bot flow is deactivated and sessionId is present");
        try {
            if (five9SessionRepository.findById(incomingSession.getSessionId()).isEmpty()){
                conversationService.sendConversationToFive9Agent(conversation, incomingSession);
            } else {
                processAgentInteraction(conversation, incomingSession);
            }
        } catch (Exception ex) {
            LOGGER.error("Exception while processing bot deactivated and sessionId present: {}", ex.getMessage());
        } finally {
            LOGGER.info("Finish process bot deactivated and sessionId present");
        }
    }
}
