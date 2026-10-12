package zhedron.movie.services;

import org.springframework.web.multipart.MultipartFile;
import zhedron.movie.dto.response.SubtitleResponse;
import zhedron.movie.entity.Subtitle;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface SubtitleService {
    Subtitle findById(long id);

    SubtitleResponse uploadFile(MultipartFile file) throws IOException;

    void deleteSubtitleById(long id);

    List<Map<String, Object>> getDataFromSrtFile(Path fileName);
}
