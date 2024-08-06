package com.inference.whatsappintegration.application.dto.whatsapp.whatappoutrequest;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SubscriberFilter {

    private String address;
    private String type;
}
