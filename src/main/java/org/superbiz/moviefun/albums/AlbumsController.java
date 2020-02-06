package org.superbiz.moviefun.albums;

import org.apache.tika.Tika;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.superbiz.moviefun.blobstore.Blob;
import org.superbiz.moviefun.blobstore.BlobStore;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;
import static org.springframework.http.MediaType.IMAGE_JPEG_VALUE;

@Controller
@RequestMapping("/albums")
public class AlbumsController {

    private final AlbumsBean albumsBean;
    private final BlobStore blobStore;

    public AlbumsController(AlbumsBean albumsBean, BlobStore blobStore) {
        this.albumsBean = albumsBean;
        this.blobStore = blobStore;
    }


    @GetMapping
    public String index(Map<String, Object> model) {
        model.put("albums", albumsBean.getAlbums());
        return "albums";
    }

    @GetMapping("/{albumId}")
    public String details(@PathVariable long albumId, Map<String, Object> model) {
        model.put("album", albumsBean.find(albumId));
        return "albumDetails";
    }

    @PostMapping("/{albumId}/cover")
    public String uploadCover(@PathVariable long albumId, @RequestParam("file") MultipartFile uploadedFile) throws IOException {
        InputStream inputStream = uploadedFile.getInputStream();
        Blob blob = new Blob(getCoverFileName(albumId), inputStream, new Tika().detect(inputStream));
        blobStore.put(blob);

        return format("redirect:/albums/%d", albumId);
    }

    @GetMapping("/{albumId}/cover")
    public HttpEntity<byte[]> getCover(@PathVariable long albumId) throws IOException {
        Optional<Blob> maybeBlob = blobStore.get(getCoverFileName(albumId));
        Blob blob = maybeBlob.orElseGet(this::defaultBlobCover);
        byte[] imageBytes = new byte[blob.inputStream.available()];
        blob.inputStream.read(imageBytes);
        HttpHeaders headers = createImageHttpHeaders(blob, imageBytes);

        return new HttpEntity<>(imageBytes, headers);
    }

    private String getCoverFileName(long albumId) {
        return format("covers/%d", albumId);
    }

    private Blob defaultBlobCover(){
        String defaultName = "default-cover.jpg";
        InputStream inputStream = AlbumsController.class.getClassLoader().getResourceAsStream(defaultName);
        return new Blob(defaultName, inputStream, IMAGE_JPEG_VALUE);

    }

    private HttpHeaders createImageHttpHeaders(Blob blob, byte[] imageBytes) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(blob.contentType));
        headers.setContentLength(imageBytes.length);
        return headers;
    }
}