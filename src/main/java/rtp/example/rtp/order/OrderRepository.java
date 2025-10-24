package rtp.example.rtp.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByPortfolioId(Long portfolioId);
    List<Order> findByStatus(OrderStatus status);
    List<Order> findByPortfolioIdAndStatus(Long portfolioId, OrderStatus status);
    List<Order> findByStatusAndPortfolioIdIn(OrderStatus status, List<Long> portfolioIds);
    List<Order> findByPortfolioIdIn(List<Long> portfolioIds);
}