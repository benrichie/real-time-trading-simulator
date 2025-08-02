package rtp.example.rtp.Portfolio;

import jakarta.persistence.Entity;
import jakarta.persistence.*;

@Entity
@Table(name = "positions")
public class Positions {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


}
