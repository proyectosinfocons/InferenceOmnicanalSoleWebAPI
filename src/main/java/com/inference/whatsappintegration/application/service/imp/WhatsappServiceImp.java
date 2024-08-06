package com.inference.whatsappintegration.application.service.imp;

import com.inference.whatsappintegration.application.dto.five9.conversationsdto.five9conversationsendmessagerequest.SendConversationMessageRequest;
import com.inference.whatsappintegration.application.dto.whatsapp.whatsappinrequest.WhatsappIncomingMessageRequestDTO;
import com.inference.whatsappintegration.application.dto.whatsapp.whatsappoldinrequest.WhatsappOldIncomingMessageRequestDTO;
import com.inference.whatsappintegration.application.mapper.ConversationMapper;
import com.inference.whatsappintegration.application.mapper.WhatsappMapper;
import com.inference.whatsappintegration.domain.model.Conversation;
import com.inference.whatsappintegration.domain.service.*;
import com.inference.whatsappintegration.infrastructure.config.mdc.MdcAwareExecutor;
import com.inference.whatsappintegration.infrastructure.exception.ErrorMessages;
import com.inference.whatsappintegration.infrastructure.exception.GenericException;
import com.inference.whatsappintegration.infrastructure.persistence.entity.Five9Session;
import com.inference.whatsappintegration.infrastructure.persistence.entity.Sessions;
import com.inference.whatsappintegration.infrastructure.persistence.repository.Five9SessionRepository;
import com.inference.whatsappintegration.infrastructure.persistence.repository.SessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.inference.whatsappintegration.util.Utils;
import com.inference.whatsappintegration.util.Constants;

@Service
public class WhatsappServiceImp implements WhatsappService {

    private static final Logger LOGGER  = LoggerFactory.getLogger(WhatsappServiceImp.class);

    private WhatsappMapper whatsappMapper;
    private ConversationMapper conversationMapper;
    private SessionRepository sessionRepository;
    private ConversationService conversationService;
    private Five9SessionRepository five9SessionRepository;
    private Five9ConversationService five9ConversationService;
    private DatabaseOperationsService databaseOperationsService;
    private MdcAwareExecutor mdcAwareExecutor;
    private RedisService redisService;

    @Value("${property.inference.bot.disable}")
    private int botFlag;

    public WhatsappServiceImp(WhatsappMapper whatsappMapper,ConversationMapper conversationMapper,
                              SessionRepository sessionRepository, ConversationService conversationService,
                              Five9SessionRepository five9SessionRepository, Five9ConversationService five9ConversationService,
                              DatabaseOperationsService databaseOperationsService, MdcAwareExecutor mdcAwareExecutor,
                              RedisService redisService){
        this.whatsappMapper = whatsappMapper;
        this.conversationMapper = conversationMapper;
        this.sessionRepository = sessionRepository;
        this.conversationService = conversationService;
        this.five9SessionRepository = five9SessionRepository;
        this.five9ConversationService = five9ConversationService;
        this.databaseOperationsService = databaseOperationsService;
        this.mdcAwareExecutor = mdcAwareExecutor;
        this.redisService = redisService;
    }

    public void processReceiveInteraction(WhatsappIncomingMessageRequestDTO whatsappIncomingMessageRequestDTO){
        LOGGER.info("Starting process receive interaction");
        try {
            Utils.logAsJson(LOGGER, whatsappIncomingMessageRequestDTO, "WhatsappIncomingMessageRequestDTO");
            Sessions incomingSessionTmp = whatsappMapper.whatsappReceiveDTOtoSession(whatsappIncomingMessageRequestDTO);
            Sessions incomingSession = redisService.getSessionFromRequest(incomingSessionTmp);
            Conversation conversation = conversationMapper
                    .whatsappIncomingRequestToConversation(incomingSession, whatsappIncomingMessageRequestDTO);
            mdcAwareExecutor.execute(() -> databaseOperationsService.insertClientInteraction(conversation, incomingSession));
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
                    case Constants.CHANNEL_TYPE_SURVEY:
                        processSurveyBotInteraction(conversation, incomingSession);
                        break;
                    default:
                        throw new GenericException("No channel type session matching");
                }
            }
        } catch (Exception ex) {
            LOGGER.error("Exception while processing recieve interaction: {}", ex.getMessage());
        }  finally {
            LOGGER.info("Finish process receive interaction");
            Utils.clearMDCParameters();
        }

    }

    public void processReceiveInteractionOld(WhatsappOldIncomingMessageRequestDTO whatsappOldIncomingMessageRequestDTO){
        LOGGER.info("Starting process receive interaction");
        try {
            Utils.logAsJson(LOGGER, whatsappOldIncomingMessageRequestDTO, "WhatsappOldIncomingMessageRequestDTO");
            if (!Utils.isBlockedNumber(whatsappOldIncomingMessageRequestDTO)){
                Sessions incomingSessionTmp = whatsappMapper.whatsappReceiveOldDTOtoSession(whatsappOldIncomingMessageRequestDTO);
                Sessions incomingSession = redisService.getSessionFromRequest(incomingSessionTmp);
                Conversation conversation = conversationMapper
                        .whatsappOldIncomingRequestToConversation(incomingSession, whatsappOldIncomingMessageRequestDTO);
                mdcAwareExecutor.execute(() -> databaseOperationsService.insertClientInteraction(conversation, incomingSession));
                if (botFlag == Constants.BOT_FLAG_DEACTIVATED && incomingSession.getSessionId() == null){
                    LOGGER.info("DVA is deactivated and sessionId is null");
                    handleBotDeactivatedAndSessionIsNotPresent(conversation, incomingSession);
                } else if (botFlag == Constants.BOT_FLAG_DEACTIVATED){
                    LOGGER.info("DVA is deactivated and sessionId is not null");
                    handleBotDeactivatedAndSessionIsPresent(conversation, incomingSession);
                } else {
                    switch (incomingSession.getChannelType()){
                        case Constants.CHANNEL_TYPE_INFERENCE_BOT:
                            processBotInteraction(conversation, incomingSession);
                            break;
                        case Constants.CHANNEL_TYPE_FIVE9_AGENT:
                            processAgentInteraction(conversation, incomingSession);
                            break;
                        case Constants.CHANNEL_TYPE_SURVEY:
                            processSurveyBotInteraction(conversation, incomingSession);
                            break;
                        default:
                            throw new GenericException("No channel type session matching");
                    }
                }
            } else{
                LOGGER.info("Blocked number");
            }
        } catch (Exception ex) {
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
                    conversationService.sendConversationWhatsappResponse(conversation);
                    break;
                case TRANSFER_AGENT:
                    incomingSession.setChannelType(Constants.CHANNEL_TYPE_FIVE9_AGENT);
                    incomingSession.setSessionId(null);
                    redisService.saveWithAudit(incomingSession, null);
                    if (conversation.getTransferAgentInformation() != null) {
                        conversationService.sendConversationWhatsappResponse(conversation);
                    }
                    conversationService.sendConversationToFive9Agent(conversation, incomingSession);
                    break;
                case TERMINATED:
                    sessionRepository.delete(incomingSession);
                    conversationService.sendConversationWhatsappResponse(conversation);
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
            SendConversationMessageRequest sendConversationMessageRequest = conversationMapper.conversationToFive9SendConversationMessage(conversation);
            five9ConversationService.sendFive9MessageConversation(five9Session, sendConversationMessageRequest);
        } catch (Exception ex) {
            LOGGER.error("Exception while processing agent interaction: {}", ex.getMessage());
        } finally {
            LOGGER.info("Finish process agent interaction");
        }
    }

    private void processSurveyBotInteraction(Conversation conversation, Sessions incomingSession){
        LOGGER.info("Starting process bot survey flow interaction");
        try {
            conversation.setAgentSurvey(incomingSession.getAgentName());
            conversation = conversationService.sendConversationToInferenceSurveyBot(conversation);

            Conversation tempConversation = conversation;
            mdcAwareExecutor.execute(() -> databaseOperationsService.insertBotMetricsInteraction(tempConversation));
            switch (conversation.getStatus()) {
                case IN_PROGRESS:
                    if (incomingSession.getSessionId() == null) {
                        incomingSession.setSessionId(conversation.getSessionId());
                    }
                    redisService.saveWithAudit(incomingSession, Constants.EXPIRATION_TIME_DEFAULT);
                    break;
                case TERMINATED:
                    sessionRepository.delete(incomingSession);
                    break;
                default:
                    throw new GenericException("No conversation status matching");
            }
            conversationService.sendConversationWhatsappResponse(conversation); // if credit expired check message
        } catch (Exception ex) {
            LOGGER.error("Exception while processing bot survey flow interaction: {}", ex.getMessage());
        }  finally {
            LOGGER.info("Finish process bot survey flow interaction");
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
