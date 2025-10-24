package rtp.example.rtp.user;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    private Long getCurrentUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        return user.getId();
    }

    // Admin only - restrict in controller with @PreAuthorize
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUser(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        // Users can only get their own info
        Long currentUserId = getCurrentUserId();
        if (!id.equals(currentUserId)) {
            throw new IllegalArgumentException("Access denied: You can only access your own user info");
        }

        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + id));
    }

    public User getCurrentUser() {
        Long currentUserId = getCurrentUserId();
        return userRepository.findById(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("Current user not found"));
    }

    public void deleteUser(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        // Users can only delete their own account
        Long currentUserId = getCurrentUserId();
        if (!id.equals(currentUserId)) {
            throw new IllegalArgumentException("Access denied: You can only delete your own account");
        }

        userRepository.deleteById(id);
    }

    public BigDecimal getBalance(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        // Users can only get their own balance
        Long currentUserId = getCurrentUserId();
        if (!id.equals(currentUserId)) {
            throw new IllegalArgumentException("Access denied: You can only access your own balance");
        }

        return userRepository.findById(id)
                .map(User::getInitialBalance)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + id));
    }
}