package com.inference.whatsappintegration.infrastructure.adapter;

import com.inference.whatsappintegration.application.dto.facebook.facebookinresponse.FacebookInResponse;
import com.inference.whatsappintegration.application.dto.facebook.facebookoutresponse.FacebookOutResponse;
import com.inference.whatsappintegration.application.dto.whatsapp.whatsappoutresponse.WhatsappOutResponse;
import com.inference.whatsappintegration.domain.service.FacebookMessageApi;
import com.inference.whatsappintegration.util.Constants;
import com.inference.whatsappintegration.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;


@Component
public class FacebookMessageApiImp implements FacebookMessageApi {
    private static final Logger LOGGER = LoggerFactory.getLogger(FacebookMessageApiImp.class);

    @Value("${property.facebook.access.token}")
    private String accessToken;

    @Value("${property.facebook.url.messages}")
    private String messageUrl;

    @Override
    public void sendFacebookMessage(FacebookInResponse facebookInResponse) {
        LOGGER.info("Starting sending facebook response");
        try{
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(messageUrl)
                    .queryParam("access_token", accessToken);

            Utils.logAsJson(LOGGER, facebookInResponse, "FacebookOutResponse");

            HttpEntity<FacebookInResponse> entity = new HttpEntity<>(facebookInResponse, headers);

            ResponseEntity<FacebookOutResponse> whatsappOutResponseResponse = restTemplate.postForEntity(
                    builder.build().toUri(), entity, FacebookOutResponse.class
            );
            if (Constants.SUCCESS_STATUS_CODES.contains(whatsappOutResponseResponse.getStatusCode().value())){
                Utils.logAsJson(LOGGER, whatsappOutResponseResponse.getBody(), "WhatsappMessageResponse");
                LOGGER.info("Success in sending facebook message");
            } else {
                LOGGER.error("Error while sending facebook message");
            }
        } catch (Exception ex) {
            LOGGER.error("Exception while sending facebook message response: {}", ex.getMessage());
        }  finally {
            LOGGER.info("Finish sending facebook response");
        }
    }
}
