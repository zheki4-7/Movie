package zhedron.movie.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import zhedron.movie.entity.Film;
import zhedron.movie.entity.MediaContent;

import java.time.LocalDate;

public interface MediaContentRepository extends JpaRepository<MediaContent, Long> {
    Page<MediaContent> findAll(Pageable pageable);

    boolean existsByIdAndFilmIsNotNullOrSeasonsIsNotEmpty(long id);

    @Query("SELECT m from MediaContent m WHERE m.releaseDate BETWEEN :start AND :end")
    Page<MediaContent> findByReleaseDateBetween(Pageable pageable, LocalDate start, LocalDate end);

    MediaContent findByFilm(Film film);

    Page<MediaContent> findByTitleContaining(String title, Pageable pageable);
}
