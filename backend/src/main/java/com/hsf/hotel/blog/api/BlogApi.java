package com.hsf.hotel.blog.api;

import com.hsf.hotel.config.ApiResponse;
import com.hsf.hotel.exception.ResourceNotFoundException;
import com.hsf.hotel.blog.model.Blog;
import com.hsf.hotel.blog.service.BlogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/blogs")
public class BlogApi {

    private final BlogService blogService;

    public BlogApi(BlogService blogService) {
        this.blogService = blogService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse> getAllBlogs() {
        List<Blog> blogs = blogService.getAllBlogs();
        return ResponseEntity.ok(ApiResponse.ok(blogs));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getBlogDetail(@PathVariable Integer id) {
        Blog blog = blogService.getBlogById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blog", id));
        return ResponseEntity.ok(ApiResponse.ok(blog));
    }
}
