package com.gluonhq.devoxx.serverless.feedback;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.*;

public class FeedbackLambda implements RequestHandler<FeedbackMail, String> {

    private static final String SOURCE = "devoxx@gluonhq.com";
    private static final String TO = "info@devoxx.com";
    private static final String SUBJECT = "Feedback for Devoxx app";

    @Override
    public String handleRequest(FeedbackMail input, Context context) {
        try {
            AmazonSimpleEmailService client =
                    AmazonSimpleEmailServiceClientBuilder.standard()
                            .withRegion(Regions.EU_WEST_1)
                            .build();
            SendEmailRequest request = new SendEmailRequest()
                    .withDestination(new Destination().withToAddresses(TO))
                    .withMessage(new Message()
                            .withBody(new Body()
                                    .withText(new Content()
                                            .withCharset("UTF-8").withData(
                                                    input.getName() + " <" + input.getEmail() + "> has submitted the following feedback for the Devoxx Mobile app:" +
                                                    "\n\n" + input.getMessage())))
                            .withSubject(new Content()
                                    .withCharset("UTF-8").withData(SUBJECT)))
                    .withSource(SOURCE);
            client.sendEmail(request);
            System.out.println("Email sent!");
        } catch (Exception ex) {
            System.out.println("The email was not sent. Error message: " + ex.getMessage());
        }
        return "";
    }

    public static void main(String[] args) {
        FeedbackMail input = new FeedbackMail("Abhinay Agarwal", "abhinay.agarwal@gluonhq.com","test email");
        new FeedbackLambda().handleRequest(input, null);
    }
}
