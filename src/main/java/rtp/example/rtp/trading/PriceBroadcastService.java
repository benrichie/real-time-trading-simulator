package rtp.example.rtp.trading;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class PriceBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public PriceBroadcastService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    // Broadcast live stock price to all connected clients
    public void broadcastPriceUpdate(RealTimeStockDataService.PriceUpdateMessage message) {
        messagingTemplate.convertAndSend("/topic/prices", message);
    }

    // Send a message to a specific user (optional, for later use)
    public void notifyUserOrder(Long userId, Object orderMessage) {
        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/orders",
                orderMessage
        );
    }
}
