package com.trailfinder.trail_finder.controller;

import com.trailfinder.trail_finder.model.Post;
import com.trailfinder.trail_finder.repository.PostRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    @Autowired
    private PostRepository postRepository;

    @PostConstruct
    public void init() {
        if (postRepository.count() == 0) {
            postRepository.save(new Post(null, "Juan García", "https://api.realworld.io/images/avatar-1.jpg", "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=500&h=600&fit=crop", 234, "Increíble atardecer en las montañas 🏔️", "Hace 2 horas", false));
            postRepository.save(new Post(null, "María López", "https://api.realworld.io/images/avatar-2.jpg", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=500&h=600&fit=crop", 567, "Disfrutando del día en la playa 🌊", "Hace 4 horas", false));
            postRepository.save(new Post(null, "Carlos Rodríguez", "https://api.realworld.io/images/avatar-3.jpg", "https://images.unsplash.com/photo-1469022563149-aa64dbd37dae?w=500&h=600&fit=crop", 123, "Explorando nuevas rutas de senderismo 🥾", "Hace 6 horas", false));
            postRepository.save(new Post(null, "Ana Martínez", "https://api.realworld.io/images/avatar-4.jpg", "https://images.unsplash.com/photo-1495521821757-a1efb6729352?w=500&h=600&fit=crop", 890, "Viaje a la ciudad de la luz ✨", "Hace 8 horas", false));
        }
    }

    @GetMapping
    public List<Post> getAllPosts() {
        return postRepository.findAll();
    }

    @PostMapping
    public Post createPost(@RequestBody Post post) {
        return postRepository.save(post);
    }

    @PutMapping("/{id}/like")
    public ResponseEntity<Post> toggleLike(@PathVariable Long id) {
        return postRepository.findById(id).map(post -> {
            post.setIsLiked(!post.getIsLiked());
            post.setLikes(post.getIsLiked() ? post.getLikes() + 1 : post.getLikes() - 1);
            return ResponseEntity.ok(postRepository.save(post));
        }).orElse(ResponseEntity.notFound().build());
    }
}
