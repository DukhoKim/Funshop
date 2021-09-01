package funshop;

import funshop.config.kafka.KafkaProcessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class PolicyHandler{
    @Autowired PaymentListRepository paymentListRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverCartCanceled_CancelPayment(@Payload CartCanceled cartCanceled){

        if(!cartCanceled.validate()) return;

        System.out.println("\n\n##### listener CancelPayment : " + cartCanceled.toJson() + "\n\n");



    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}
