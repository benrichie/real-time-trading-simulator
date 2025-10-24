package rtp.example.rtp.transaction;

import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public List<Transaction> getAllTransactions() {
        return transactionService.getAllTransactions();
    }

    @GetMapping("/{id}")
    public Transaction getTransaction(@PathVariable Long id) {
        return transactionService.getTransaction(id);
    }

    @GetMapping("/portfolio/{portfolioId}")
    public List<Transaction> getTransactionsByPortfolio(@PathVariable Long portfolioId) {
        return transactionService.getTransactionsByPortfolio(portfolioId);
    }

    @GetMapping("/order/{orderId}")
    public List<Transaction> getTransactionsByOrder(@PathVariable Long orderId) {
        return transactionService.getTransactionsByOrder(orderId);
    }

    @PostMapping
    public Transaction createTransaction(@RequestBody Transaction transaction) {
        return transactionService.createTransaction(transaction);
    }
}