package com.inference.whatsappintegration.domain.service;

import com.inference.whatsappintegration.application.dto.inference.inferenceoutrequest.InferenceOutRequest;
import com.inference.whatsappintegration.application.dto.inference.inferenceoutresponse.InferenceOutResponse;

public interface InferenceBotApi {

    InferenceOutResponse sendMessageBot(InferenceOutRequest inferenceOutRequest);

}
