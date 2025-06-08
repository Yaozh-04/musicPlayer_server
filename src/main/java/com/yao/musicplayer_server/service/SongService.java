package com.yao.musicplayer_server.service;

import com.yao.musicplayer_server.entity.Song;
import com.yao.musicplayer_server.repository.SongRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;
import org.jaudiotagger.tag.FieldKey;

@Service
public class SongService {
    @Autowired
    private SongRepository songRepository;

    public List<Song> findAll() {
        return songRepository.findAll();
    }

    public Optional<Song> findById(Long id) {
        return songRepository.findById(id);
    }

    public Song save(Song song) {
        return songRepository.save(song);
    }

    public void deleteById(Long id) {
        songRepository.deleteById(id);
    }

    public Page<Song> search(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return songRepository.findByNameContainingOrArtistContainingOrAlbumContaining(keyword, keyword, keyword, pageable);
    }

    /**
     * 递归扫描目录下所有mp3/flac文件
     */
    private void scanDir(File dir, List<File> musicFiles) {
        if (dir == null || !dir.exists()) {
            System.out.println("目录不存在: " + dir);
            return;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            System.out.println("无法读取目录: " + dir);
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                System.out.println("进入子目录: " + file.getAbsolutePath());
                scanDir(file, musicFiles);
            } else if (file.isFile()) {
                System.out.println("发现文件: " + file.getAbsolutePath());
                String lowerName = file.getName().toLowerCase();
                if (lowerName.endsWith(".mp3") || lowerName.endsWith(".flac")) {
                    musicFiles.add(file);
                }
            }
        }
    }

    /**
     * 扫描服务器本地music目录及所有子目录下的mp3/flac文件，自动入库
     * @return 新增歌曲数量
     */
    public int scanLocalMusicDir() {
        File musicDir = new File("D:\\ProgramData\\music");
        System.out.println("开始扫描目录: " + musicDir.getAbsolutePath());
        List<File> musicFiles = new ArrayList<>();
        scanDir(musicDir, musicFiles);
        System.out.println("共发现音频文件: " + musicFiles.size());
        int newCount = 0;
        for (File file : musicFiles) {
            // 用文件绝对路径做唯一性判断
            if (!songRepository.findByFilePath(file.getAbsolutePath()).isPresent()) {
                Song song = new Song();
                try {
                    AudioFile audioFile = AudioFileIO.read(file);
                    Tag tag = audioFile.getTag();
                    song.setName(tag != null && tag.getFirst(FieldKey.TITLE) != null && !tag.getFirst(FieldKey.TITLE).isEmpty() ? tag.getFirst(FieldKey.TITLE) : file.getName());
                    song.setArtist(tag != null ? tag.getFirst(FieldKey.ARTIST) : "");
                    song.setAlbum(tag != null ? tag.getFirst(FieldKey.ALBUM) : "");
                    song.setDuration(audioFile.getAudioHeader().getTrackLength());
                    // 提取封面
                    if (tag != null && tag.getFirstArtwork() != null) {
                        Artwork artwork = tag.getFirstArtwork();
                        byte[] imageData = artwork.getBinaryData();
                        File coverDir = new File("D:/ProgramData/music/covers");
                        if (!coverDir.exists()) coverDir.mkdirs();
                        String coverFileName = file.getName() + ".jpg";
                        File coverFile = new File(coverDir, coverFileName);
                        try (FileOutputStream fos = new FileOutputStream(coverFile)) {
                            fos.write(imageData);
                            // 只保存相对路径
                            song.setCoverPath("covers/" + coverFileName);
                        } catch (Exception e) {
                            song.setCoverPath("");
                        }
                    } else {
                        song.setCoverPath("");
                    }
                } catch (Exception e) {
                    System.out.println("元数据解析失败: " + file.getAbsolutePath() + " 错误: " + e.getMessage());
                    song.setName(file.getName());
                    song.setArtist("");
                    song.setAlbum("");
                    song.setDuration(0);
                    song.setCoverPath("");
                }
                song.setFilePath(file.getAbsolutePath());
                songRepository.save(song);
                newCount++;
                System.out.println("新增入库: " + file.getAbsolutePath());
            } else {
                System.out.println("已存在不入库: " + file.getAbsolutePath());
            }
        }
        System.out.println("本次新增歌曲数: " + newCount);
        return newCount;
    }

    /**
     * 批量修正老数据，将coverPath绝对路径改为相对路径（covers/xxx.jpg）
     */
    public void fixOldCoverPath() {
        List<Song> allSongs = songRepository.findAll();
        int fixCount = 0;
        for (Song song : allSongs) {
            String coverPath = song.getCoverPath();
            if (coverPath != null && coverPath.contains("covers") && (coverPath.contains(":") || coverPath.startsWith("/"))) {
                // 提取文件名
                String fileName = coverPath.substring(coverPath.lastIndexOf("covers") + 7);
                String newPath = "covers/" + fileName;
                if (!newPath.equals(coverPath)) {
                    song.setCoverPath(newPath);
                    songRepository.save(song);
                    fixCount++;
                }
            }
        }
        System.out.println("已修正老数据coverPath数量: " + fixCount);
    }

    /**
     * 批量为已入库但coverPath为空或图片文件不存在的歌曲提取封面（递归支持子目录）
     */
    public void fixOldCovers() {
        List<Song> allSongs = songRepository.findAll();
        int fixCount = 0;
        File coverDir = new File("D:/ProgramData/music/covers");
        if (!coverDir.exists()) coverDir.mkdirs();
        for (Song song : allSongs) {
            String coverPath = song.getCoverPath();
            String filePath = song.getFilePath();
            boolean needFix = (coverPath == null || coverPath.isEmpty());
            if (!needFix && coverPath.startsWith("covers/")) {
                File coverFile = new File("D:/ProgramData/music/" + coverPath);
                if (!coverFile.exists()) needFix = true;
            }
            System.out.println("[封面修复] 歌曲: " + song.getName() + ", filePath: " + filePath + ", coverPath: " + coverPath + ", needFix: " + needFix);
            if (needFix) {
                try {
                    File musicFile = new File(filePath);
                    System.out.println("  - 检查音频文件是否存在: " + musicFile.exists() + ", 路径: " + musicFile.getAbsolutePath());
                    boolean coverSaved = false;
                    if (musicFile.exists()) {
                        AudioFile audioFile = AudioFileIO.read(musicFile);
                        Tag tag = audioFile.getTag();
                        if (tag != null && tag.getFirstArtwork() != null) {
                            Artwork artwork = tag.getFirstArtwork();
                            byte[] imageData = artwork.getBinaryData();
                            String coverFileName = musicFile.getName() + ".jpg";
                            File coverFile = new File(coverDir, coverFileName);
                            try (FileOutputStream fos = new FileOutputStream(coverFile)) {
                                fos.write(imageData);
                                song.setCoverPath("covers/" + coverFileName);
                                songRepository.save(song);
                                fixCount++;
                                coverSaved = true;
                                System.out.println("  - 封面提取成功，保存为: " + coverFile.getAbsolutePath());
                            }
                        } else {
                            System.out.println("  - 未找到内嵌封面，尝试目录图片兜底");
                            // 兜底：查找同目录下图片文件
                            File parentDir = musicFile.getParentFile();
                            if (parentDir != null && parentDir.exists()) {
                                File[] images = parentDir.listFiles(f -> {
                                    String name = f.getName().toLowerCase();
                                    return f.isFile() && (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp"));
                                });
                                if (images != null && images.length > 0) {
                                    // 选第一张图片作为封面
                                    File img = images[0];
                                    String coverFileName = musicFile.getName() + ".jpg";
                                    File coverFile = new File(coverDir, coverFileName);
                                    try (FileOutputStream fos = new FileOutputStream(coverFile); java.io.FileInputStream fis = new java.io.FileInputStream(img)) {
                                        byte[] buf = new byte[4096];
                                        int len;
                                        while ((len = fis.read(buf)) != -1) {
                                            fos.write(buf, 0, len);
                                        }
                                        song.setCoverPath("covers/" + coverFileName);
                                        songRepository.save(song);
                                        fixCount++;
                                        coverSaved = true;
                                        System.out.println("  - 目录图片兜底成功，保存为: " + coverFile.getAbsolutePath());
                                    }
                                } else {
                                    System.out.println("  - 目录下未找到可用图片兜底");
                                }
                            }
                        }
                    } else {
                        System.out.println("  - 音频文件不存在，跳过");
                    }
                } catch (Exception e) {
                    System.out.println("  - 封面提取异常: " + e.getMessage());
                }
            }
        }
        System.out.println("已补全封面数量: " + fixCount);
    }

    // 可扩展：搜索、分页、扫描本地、上传等
} 