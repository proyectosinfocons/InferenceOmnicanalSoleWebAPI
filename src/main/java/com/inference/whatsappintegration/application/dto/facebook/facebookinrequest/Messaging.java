package com.inference.whatsappintegration.application.dto.facebook.facebookinrequest;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Messaging {
    private User sender;
    private User recipient;
    private Message message;
    private Long timestamp;
}
