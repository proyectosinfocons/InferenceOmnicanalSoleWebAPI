package com.inference.whatsappintegration.application.service.imp;

import com.inference.whatsappintegration.domain.service.RedisService;
import com.inference.whatsappintegration.infrastructure.persistence.entity.Sessions;
import com.inference.whatsappintegration.infrastructure.persistence.repository.SessionRepository;
import com.inference.whatsappintegration.util.Constants;
import com.inference.whatsappintegration.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class RedisServiceImp implements RedisService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisServiceImp.class);

    private SessionRepository sessionRepository;

    public RedisServiceImp(SessionRepository sessionRepository){
        this.sessionRepository = sessionRepository;
    }

    public void saveWithAudit(Sessions entity, Long expirationTime) {
        entity.setExpiration(expirationTime);
        sessionRepository.save(entity);
    }

    public Sessions getSessionFromRequest(Sessions incomingSession) {
        Optional<Sessions> session = sessionRepository.findById(incomingSession.getIdentifier());
        if (session.isEmpty()) {
            LOGGER.info("Session not found generating one");
            incomingSession.setConversationId(Utils.generateConversationId());
            incomingSession.setChannelType(Constants.CHANNEL_TYPE_INFERENCE_BOT);
            saveWithAudit(incomingSession, Constants.EXPIRATION_TIME_DEFAULT);
        } else {
            LOGGER.info("Session found");
            incomingSession = session.get();
        }
        Utils.setMDCParameters(incomingSession);
        return incomingSession;
    }
}
