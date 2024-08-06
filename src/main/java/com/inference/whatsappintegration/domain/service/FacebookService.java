package com.inference.whatsappintegration.domain.service;

import com.inference.whatsappintegration.application.dto.facebook.facebookinrequest.FacebookInRequest;

public interface FacebookService {
    void processReceiveInteraction(FacebookInRequest facebookInRequest);
}
