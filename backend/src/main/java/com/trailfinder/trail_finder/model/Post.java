package com.trailfinder.trail_finder.model;

import jakarta.persistence.*;

@Entity
@Table(name = "posts")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String author;
    private String avatar;
    private String image;
    private int likes;
    private String caption;
    private String timestamp;
    private boolean isLiked;

    public Post() {}

    public Post(Long id, String author, String avatar, String image, int likes, String caption, String timestamp, boolean isLiked) {
        this.id = id;
        this.author = author;
        this.avatar = avatar;
        this.image = image;
        this.likes = likes;
        this.caption = caption;
        this.timestamp = timestamp;
        this.isLiked = isLiked;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public int getLikes() { return likes; }
    public void setLikes(int likes) { this.likes = likes; }
    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public boolean getIsLiked() { return isLiked; }
    public void setIsLiked(boolean isLiked) { this.isLiked = isLiked; }
}
