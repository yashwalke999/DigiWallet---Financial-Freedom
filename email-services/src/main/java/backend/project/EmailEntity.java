package backend.project;

import lombok.*;
import javax.persistence.*;

@Entity
@Table(name = " emails")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String status;
    private String emailTo;
}
