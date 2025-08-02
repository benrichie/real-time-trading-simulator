package rtp.example.rtp.Positions;

import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/positions")
public class PositionController {

    private final PositionService positionService;

    public PositionController(PositionService positionService) {
        this.positionService = positionService;
    }

    @GetMapping
    public List<Position> getAllPositions() {
        return positionService.getAllPositions();
    }

    @GetMapping("/{id}")
    public Position getPosition(@PathVariable Long id) {
        return positionService.getPosition(id);
    }

    @GetMapping("/portfolio/{portfolioId}")
    public List<Position> getPositionsByPortfolio(@PathVariable Long portfolioId) {
        return positionService.getPositionsByPortfolio(portfolioId);
    }

    @PostMapping
    public Position createPosition(@RequestBody Position position) {
        return positionService.createPosition(position);
    }

    @PutMapping("/{id}")
    public Position updatePosition(@PathVariable Long id, @RequestBody Position position) {
        position.setId(id);
        return positionService.updatePosition(position);
    }

    @DeleteMapping("/{id}")
    public void deletePosition(@PathVariable Long id) {
        positionService.deletePosition(id);
    }
}