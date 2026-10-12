package zhedron.movie.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import zhedron.movie.dto.response.SubtitleResponse;
import zhedron.movie.entity.Subtitle;
import zhedron.movie.exceptions.SrtFileParsingException;
import zhedron.movie.exceptions.SubtitleNotFoundException;
import zhedron.movie.mappers.SubtitleMapper;
import zhedron.movie.repository.SubtitleRepository;
import zhedron.movie.services.SubtitleService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class SubtitleServiceImpl implements SubtitleService {
    private final SubtitleRepository subtitleRepository;

    private final SubtitleMapper subtitleMapper;

    private final String DIRECTORY = "subtitles/";

    public SubtitleServiceImpl(SubtitleRepository subtitleRepository, SubtitleMapper subtitleMapper) {
        this.subtitleRepository = subtitleRepository;
        this.subtitleMapper = subtitleMapper;
    }


    @Override
    @Cacheable(value = "subtitles", key = "#id")
    public Subtitle findById(long id) {
        return subtitleRepository.findById(id).orElseThrow(() -> new SubtitleNotFoundException("Subtitle not found with " + id));
    }

    @Override
    @CachePut(value = "subtitles", key = "#result.id()")
    public SubtitleResponse uploadFile(MultipartFile file) throws IOException {
        Path directory = Paths.get(DIRECTORY);

        if (Files.notExists(directory)) {
            Files.createDirectory(directory);
        }

        String filePath = file.getOriginalFilename();

        Path path = Paths.get(DIRECTORY).resolve(filePath).normalize();

        Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

        Subtitle subtitle = new Subtitle();

        subtitle.setSrtName(filePath);
        subtitle.setReleased(false);

        Subtitle savedSubtitle = subtitleRepository.save(subtitle);

        return subtitleMapper.toSubtitleResponse(savedSubtitle);
    }

    @Override
    @CacheEvict(value = "subtitles", key = "#id")
    public void deleteSubtitleById(long id) {
        Subtitle subtitle = findById(id);

        try {
            Path path = Paths.get(DIRECTORY).resolve(subtitle.getSrtName()).normalize();

            Files.deleteIfExists(path);

            subtitleRepository.deleteById(id);
        } catch (IOException e) {
            log.error("Error deleting file subtitle: {}", e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> getDataFromSrtFile(Path fileName) {
        try {
            List<Map<String, Object>> list = new ArrayList<>();

            String content = Files.readString(Path.of(DIRECTORY + fileName));

            String[] blocks = content.split("(\\r\\n){2,}");

            for (String block : blocks) {
                String[] lines = block.split("\\n");

                if (lines.length < 3) {
                    throw new SrtFileParsingException("Failed to read file");
                }

                Map<String, Object> map = new HashMap<>();

                int number = Integer.parseInt(lines[0].trim());

                String[] times = lines[1].trim().split("-->");

                String startTime = times[0].trim();
                String endTime = times[1].trim();

                StringBuilder textBuilder = new StringBuilder();

                for (int i = 2; i < lines.length; i++) {
                    if (i > 2) {
                        textBuilder.append(" ");
                    }

                    textBuilder.append(lines[i].trim());
                }

                map.put("text", textBuilder.toString());

                map.put("startTime", startTime);
                map.put("endTime", endTime);

                map.put("number", number);

                list.add(map);
            }

            return list;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
