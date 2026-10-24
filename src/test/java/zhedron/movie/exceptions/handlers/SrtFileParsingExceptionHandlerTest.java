package zhedron.movie.exceptions.handlers;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import zhedron.movie.exceptions.SrtFileParsingException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SrtFileParsingExceptionHandlerTest {
    private final SrtFileParsingExceptionHandler handler = new SrtFileParsingExceptionHandler();

    @Test
    void returnsInternalServerErrorWithParsingMessage() {
        var response = handler.handleSrtFileParsingException(
                new SrtFileParsingException("Failed to read file"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Failed to read file", response.getBody().get("message"));
    }
}
