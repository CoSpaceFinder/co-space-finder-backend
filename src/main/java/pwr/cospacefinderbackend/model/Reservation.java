package pwr.cospacefinderbackend.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "reservation")
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unique identifier of the reservation.", example = "1")
    private Long id;

    @ManyToOne
    @Schema(description = "User that made the reservation.", example = "1")
    private User user;

    @Column(nullable = false)
    @Schema(description = "Start date of the reservation.", example = "2024-06-01")
    private LocalDate startDate;

    @Column(nullable = false)
    @Schema(description = "End date of the reservation.", example = "2024-06-05")
    private LocalDate endDate;

    @ManyToOne
    @Schema(description = "Room that reservation is made for.", example = "1")
    private Room room;

    @Column(nullable = false)
    @Schema(description = "Desk that reservation is made for.", example = "1")
    private Integer desk;

    @Column(nullable = false)
    @Schema(description = "Price of the reservation.", example = "100.00")
    private Double price;
}
