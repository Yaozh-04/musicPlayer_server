package com.yao.musicplayer_server.repository;

import com.yao.musicplayer_server.entity.Song;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SongRepository extends JpaRepository<Song, Long> {
    // 可扩展自定义查询方法
    Page<Song> findByNameContainingOrArtistContainingOrAlbumContaining(String name, String artist, String album, Pageable pageable);

    Optional<Song> findByFilePath(String filePath);
} 