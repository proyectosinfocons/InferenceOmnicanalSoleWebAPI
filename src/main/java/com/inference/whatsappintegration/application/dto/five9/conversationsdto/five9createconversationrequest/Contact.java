package com.inference.whatsappintegration.application.dto.five9.conversationsdto.five9createconversationrequest;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Contact {

    private String number1;
    private String firstName;
    private String lastName;
    private String gender;
    private String email;
    private String socialAccountHandle;
    private String socialAccountName;
    private String socialAccountImageUrl;
    private String socialAccountProfileUrl;

}
