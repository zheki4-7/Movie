package zhedron.movie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import zhedron.movie.entity.Subtitle;

import java.util.List;
import java.util.Optional;

public interface SubtitleRepository extends JpaRepository<Subtitle, Long> {
    @Query("SELECT s FROM Subtitle s WHERE s.released = false")
    List<Subtitle> findAll();

    Subtitle findByFilmId(long filmId);

    Optional<Subtitle> findByEpisodeId(long episodeId);
}
