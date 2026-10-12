package zhedron.movie.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "subtitles")
@Data
public class Subtitle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String srtName;

    private boolean released;

    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private Film film;

    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.MERGE})
    private Episode episode;
}
