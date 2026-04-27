package com.trailfinder.trail_finder.repository;

import com.trailfinder.trail_finder.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {
}
