package com.inference.whatsappintegration.domain.service;

import com.inference.whatsappintegration.application.dto.facebook.facebookinresponse.FacebookInResponse;

public interface FacebookMessageApi {
    void sendFacebookMessage(FacebookInResponse facebookInResponse);
}
