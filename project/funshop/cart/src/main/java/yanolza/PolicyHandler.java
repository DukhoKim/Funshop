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
    @Autowired CartRepository cartRepository;
    @Autowired CancellationRepository cancellationRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPaymentApproved_AcceptCart(@Payload PaymentApproved paymentApproved){

        if(!paymentApproved.validate()) return;

        System.out.println("\n\n##### listener AcceptCart : " + paymentApproved.toJson() + "\n\n");

     
        //System.out.println("### Listener : " + paymentApproved.toJon());
        Cart cart = new Cart();
        cart.setStatus("In your cart");
        cart.setOrderId(paymentApproved.getOrderId());
        cart.setId(paymentApproved.getOrderId());
        cartRepository.save(cart);
        
        

    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverOrderCanceled_CancelCart(@Payload OrderCanceled orderCanceled){

        if(!orderCanceled.validate()) return;

        System.out.println("\n\n##### listener CancelCart : " + orderCanceled.toJson() + "\n\n");




        Cancellation cancellation = new Cancellation();
        cancellation.setOrderId(orderCanceled.getId());
        cancellation.setStatus("Cart Canceled");
        cancellationRepository.save(cancellation);

    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}
