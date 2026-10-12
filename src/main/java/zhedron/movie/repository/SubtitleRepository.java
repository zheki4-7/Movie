package zhedron.movie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import zhedron.movie.entity.Subtitle;

import java.util.List;

public interface SubtitleRepository extends JpaRepository<Subtitle, Long> {
    @Query("SELECT s FROM Subtitle s WHERE s.released = false")
    List<Subtitle> findAll();

    Subtitle findByFilmId(long filmId);

    Subtitle findByEpisodeId(long episodeId);
}
