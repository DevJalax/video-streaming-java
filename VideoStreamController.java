import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;
import java.nio.file.Files;
import java.util.List;

@RestController
@RequestMapping("/videos")
public class VideoStreamController {

    private static final String VIDEO_PATH = "/path/to/large-video.mp4";
    private static final int CHUNK_SIZE = 1024 * 1024; // 1MB buffer for chunks

    @GetMapping(value = "/stream", produces = "video/mp4")
    public ResponseEntity<InputStreamResource> streamVideo(@RequestHeader(value = "Range", required = false) String rangeHeader) throws IOException {
        File videoFile = new File(VIDEO_PATH);
        long fileSize = videoFile.length();

        try (RandomAccessFile file = new RandomAccessFile(videoFile, "r")) {
            if (rangeHeader == null) {
                return fullContentResponse(file, fileSize);
            }

            List<HttpRange> ranges = HttpRange.parseRanges(rangeHeader);
            HttpRange range = ranges.get(0);
            long start = range.getRangeStart(fileSize);
            long end = range.getRangeEnd(fileSize);

            return partialContentResponse(file, start, end, fileSize);
        }
    }

    private ResponseEntity<InputStreamResource> fullContentResponse(RandomAccessFile file, long fileSize) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("video/mp4"));
        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(fileSize)
                .body(new InputStreamResource(new BufferedInputStream(new FileInputStream(file.getFD()))));
    }

    private ResponseEntity<InputStreamResource> partialContentResponse(RandomAccessFile file, long start, long end, long fileSize) throws IOException {
        long contentLength = end - start + 1;
        byte[] buffer = new byte[(int) contentLength];

        file.seek(start);
        file.readFully(buffer);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Range", "bytes " + start + "-" + end + "/" + fileSize);
        headers.setContentType(MediaType.valueOf("video/mp4"));

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .headers(headers)
                .contentLength(contentLength)
                .body(new InputStreamResource(new ByteArrayInputStream(buffer)));
    }
}
