package rtp.example.rtp;

import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/real-time")
public class RealTimeStockDataController {
    private final RealTimeStockDataService realTimeStockDataService;

    public RealTimeStockDataController(RealTimeStockDataService realTimeStockDataService) {
        this.realTimeStockDataService = realTimeStockDataService;
    }

    @GetMapping("/{symbol}")
    public ResponseEntity<?> getCurrentStockPrice(@PathVariable String symbol){
        return ResponseEntity.ok(realTimeStockDataService.getCurrentStockPrice(symbol));
    }
}
