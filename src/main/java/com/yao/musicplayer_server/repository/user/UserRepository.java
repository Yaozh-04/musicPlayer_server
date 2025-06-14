package com.yao.musicplayer_server.repository.user;

import com.yao.musicplayer_server.entity.user.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
 
@Repository
public interface UserRepository extends CrudRepository<User, Long> {
    User findByUsername(String username);
} 