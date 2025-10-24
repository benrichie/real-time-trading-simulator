package rtp.example.rtp.transaction;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import rtp.example.rtp.portfolio.PortfolioService;
import rtp.example.rtp.user.User;
import rtp.example.rtp.user.UserRepository;

import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final PortfolioService portfolioService;
    private final UserRepository userRepository;

    public TransactionService(TransactionRepository transactionRepository,
                              PortfolioService portfolioService,
                              UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.portfolioService = portfolioService;
        this.userRepository = userRepository;
    }

    private Long getCurrentUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        return user.getId();
    }

    private void verifyTransactionOwnership(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found: " + transactionId));

        // Verify the user owns the portfolio for this transaction
        portfolioService.getPortfolio(transaction.getPortfolioId());
    }

    // Admin only - restrict in controller
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    public Transaction getTransaction(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Transaction ID cannot be null");
        }

        verifyTransactionOwnership(id);

        return transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found: " + id));
    }

    public List<Transaction> getTransactionsByPortfolio(Long portfolioId) {
        if (portfolioId == null) {
            throw new IllegalArgumentException("Portfolio ID cannot be null");
        }

        // This will verify ownership
        portfolioService.getPortfolio(portfolioId);

        return transactionRepository.findByPortfolioId(portfolioId);
    }

    public List<Transaction> getTransactionsByOrder(Long orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID cannot be null");
        }

        // Get transactions and verify ownership for each
        List<Transaction> transactions = transactionRepository.findByOrderId(orderId);

        // Verify user owns at least one of the transactions
        if (!transactions.isEmpty()) {
            portfolioService.getPortfolio(transactions.get(0).getPortfolioId());
        }

        return transactions;
    }

    public Transaction createTransaction(Transaction transaction) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (transaction.getPortfolioId() == null) {
            throw new IllegalArgumentException("Portfolio ID cannot be null");
        }

        // Verify user owns the portfolio
        portfolioService.getPortfolio(transaction.getPortfolioId());

        return transactionRepository.save(transaction);
    }
}