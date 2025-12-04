package com.ascii.web;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class AsciiController 
{
    private static final Logger logger = LoggerFactory.getLogger(AsciiController.class);

    private final AsciiService asciiService;

    public AsciiController(AsciiService asciiService)
    {
        this.asciiService = asciiService;
    }

    @PostMapping("/convertAuto")
    public ResponseEntity<?> convertAuto
    (
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "width", defaultValue = "120") int width,
        @RequestParam(value = "color", defaultValue = "false") boolean useColor,
        @RequestParam(value = "dither", defaultValue = "false") boolean useDither,
        @RequestParam(value = "ramp", defaultValue = "AUTO") String ramp
    )
    {
        if (file == null || file.isEmpty())
        {
            logger.warn("No file provided in auto-convert request");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No file provided");
        }

        try
        {
            String contentType = file.getContentType();
            String filename = file.getOriginalFilename();
            
            boolean isGif = (contentType != null && contentType.equals("image/gif")) || (filename != null && filename.toLowerCase().endsWith(".gif"));
            boolean isVideo = (contentType != null && contentType.startsWith("video/")) || (filename != null && (filename.toLowerCase().endsWith(".mp4") || filename.toLowerCase().endsWith(".mov") || filename.toLowerCase().endsWith(".avi") || filename.toLowerCase().endsWith(".mkv")));
            
            if (isVideo)
            {
                logger.info("Detected: Video");
                ArrayList<ArrayList<String>> frames = asciiService.handleVideoConversion(file, width, useColor, useDither, ramp);
                Map<String, Object> response = new HashMap<>();
                response.put("type", "video");
                response.put("frames", frames);
                return ResponseEntity.ok(response);
            }
            else if (isGif)
            {
                logger.info("Detected: GIF");
                ArrayList<ArrayList<String>> frames = asciiService.handleGifConversion(file, width, useColor, useDither, ramp);
                Map<String, Object> response = new HashMap<>();
                response.put("type", "gif");
                response.put("frames", frames);
                return ResponseEntity.ok(response);
            }
            else
            {
                logger.info("Detected: Image");
                String ascii = asciiService.handleConversion(file, width, useColor, useDither, ramp);
                Map<String, Object> response = new HashMap<>();
                response.put("type", "image");
                response.put("ascii", ascii);
                return ResponseEntity.ok(response);
            }
        }
        catch (Exception e)
        {
            logger.error("Error in auto-convert", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    @PostMapping(path = "/exportVideo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = "video/mp4")
    public ResponseEntity<Resource> exportVideo
    (
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "width", defaultValue = "120") int width,
            @RequestParam(value = "color", defaultValue = "false") boolean useColor,
            @RequestParam(value = "dither", defaultValue = "false") boolean useDither,
            @RequestParam(value = "ramp", defaultValue = "AUTO") String ramp,
            @RequestParam(value = "fps", defaultValue = "30") int fps,
            @RequestParam(value = "fontSize", defaultValue = "12") int fontSize
    )
    {
        if (file == null || file.isEmpty())
        {
            logger.warn("No video file provided for export");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        try
        {
            File videoFile = asciiService.handleVideoExport(file, width, useColor, useDither, ramp, fps, fontSize);
            Resource resource = new FileSystemResource(videoFile);
            String originalName = file.getOriginalFilename();
            String base = (originalName != null) ? originalName.replaceFirst("\\.[^.]+$", "") : "video";
            String downloadName = base + " (ASCII).mp4";
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadName + "\"")
                .contentType(MediaType.parseMediaType("video/mp4"))
                .body(resource);
        }
        catch (Exception e)
        {
            logger.error("Error exporting video", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(path = "/exportGif", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = "image/gif")
    public ResponseEntity<Resource> exportGif
    (
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "width", defaultValue = "120") int width,
            @RequestParam(value = "color", defaultValue = "false") boolean useColor,
            @RequestParam(value = "dither", defaultValue = "false") boolean useDither,
            @RequestParam(value = "ramp", defaultValue = "AUTO") String ramp,
            @RequestParam(value = "fps", defaultValue = "10") int fps,
            @RequestParam(value = "fontSize", defaultValue = "12") int fontSize
    )
    {
        if (file == null || file.isEmpty())
        {
            logger.warn("No GIF file provided for export");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        try
        {
            File gifFile = asciiService.handleGifExport(file, width, useColor, useDither, ramp, fps, fontSize);
            Resource resource = new FileSystemResource(gifFile);
            String originalName = file.getOriginalFilename();
            String base = (originalName != null) ? originalName.replaceFirst("\\.[^.]+$", "") : "gif";
            String downloadName = base + " (ASCII).gif";
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadName + "\"")
                .contentType(MediaType.parseMediaType("image/gif"))
                .body(resource);
        }
        catch (Exception e)
        {
            logger.error("Error exporting GIF", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}