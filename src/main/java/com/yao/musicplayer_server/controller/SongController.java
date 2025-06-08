package com.yao.musicplayer_server.controller;

import com.yao.musicplayer_server.common.ApiResponse;
import com.yao.musicplayer_server.dto.SongDto;
import com.yao.musicplayer_server.entity.Song;
import com.yao.musicplayer_server.service.SongService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;

@RestController
@RequestMapping("/api/songs")
public class SongController {
    @Autowired
    private SongService songService;

    @GetMapping("")
    public ApiResponse<List<SongDto>> list() {
        List<SongDto> result = songService.findAll().stream().map(song -> {
            SongDto dto = new SongDto();
            dto.setId(song.getId());
            dto.setName(song.getName());
            dto.setArtist(song.getArtist());
            dto.setAlbum(song.getAlbum());
            dto.setDuration(song.getDuration());
            dto.setCoverUrl(song.getCoverPath());
            dto.setFileUrl("/api/songs/" + song.getId() + "/stream");
            return dto;
        }).collect(Collectors.toList());
        return ApiResponse.success(result, "获取成功");
    }

    @GetMapping("/{id}")
    public ApiResponse<SongDto> detail(@PathVariable Long id) {
        Optional<Song> songOpt = songService.findById(id);
        if (songOpt.isPresent()) {
            Song song = songOpt.get();
            SongDto dto = new SongDto();
            dto.setId(song.getId());
            dto.setName(song.getName());
            dto.setArtist(song.getArtist());
            dto.setAlbum(song.getAlbum());
            dto.setDuration(song.getDuration());
            dto.setCoverUrl(song.getCoverPath());
            dto.setFileUrl("/api/songs/" + song.getId() + "/stream");
            return ApiResponse.success(dto, "获取成功");
        } else {
            return ApiResponse.error("未找到该歌曲");
        }
    }

    @PostMapping("/upload")
    public ApiResponse<SongDto> upload(@RequestParam("file") MultipartFile file,
                                       @RequestParam("name") String name,
                                       @RequestParam("artist") String artist,
                                       @RequestParam("album") String album,
                                       @RequestParam("duration") Integer duration) {
        try {
            // 保存文件到本地
            Path musicDir = Paths.get("music");
            if (!Files.exists(musicDir)) Files.createDirectories(musicDir);
            Path filePath = musicDir.resolve(file.getOriginalFilename());
            file.transferTo(filePath);
            // 构建Song对象
            Song song = new Song();
            song.setName(name);
            song.setArtist(artist);
            song.setAlbum(album);
            song.setDuration(duration);
            song.setFilePath(filePath.toString());
            song.setCoverPath(""); // 可后续扩展
            Song saved = songService.save(song);
            SongDto dto = new SongDto();
            dto.setId(saved.getId());
            dto.setName(saved.getName());
            dto.setArtist(saved.getArtist());
            dto.setAlbum(saved.getAlbum());
            dto.setDuration(saved.getDuration());
            dto.setCoverUrl(saved.getCoverPath());
            dto.setFileUrl("/api/songs/" + saved.getId() + "/stream");
            return ApiResponse.success(dto, "上传成功");
        } catch (Exception e) {
            return ApiResponse.error("上传失败: " + e.getMessage());
        }
    }

    @GetMapping("/search")
    public ApiResponse<List<SongDto>> search(@RequestParam String keyword,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        Page<Song> songPage = songService.search(keyword, page, size);
        List<SongDto> result = songPage.getContent().stream().map(song -> {
            SongDto dto = new SongDto();
            dto.setId(song.getId());
            dto.setName(song.getName());
            dto.setArtist(song.getArtist());
            dto.setAlbum(song.getAlbum());
            dto.setDuration(song.getDuration());
            dto.setCoverUrl(song.getCoverPath());
            dto.setFileUrl("/api/songs/" + song.getId() + "/stream");
            return dto;
        }).collect(Collectors.toList());
        return ApiResponse.success(result, "搜索成功");
    }

    @PostMapping("/scan")
    public ApiResponse<?> scan() {
        int count = songService.scanLocalMusicDir();
        return ApiResponse.success(null, "扫描完成，新增" + count + "首歌曲");
    }

    @GetMapping("/{id}/stream")
    public ResponseEntity<Resource> stream(@PathVariable Long id) {
        Optional<Song> songOpt = songService.findById(id);
        if (songOpt.isPresent()) {
            Song song = songOpt.get();
            try {
                Path path = Paths.get(song.getFilePath());
                Resource resource = new UrlResource(path.toUri());
                if (resource.exists()) {
                    return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_TYPE, "audio/mpeg")
                            .body(resource);
                } else {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
                }
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/fix-cover-path")
    public ApiResponse<?> fixCoverPath() {
        songService.fixOldCoverPath();
        return ApiResponse.success(null, "批量修正完成");
    }

    @GetMapping("/fix-covers")
    public ApiResponse<?> fixCovers() {
        songService.fixOldCovers();
        return ApiResponse.success(null, "批量补全封面完成");
    }
} 