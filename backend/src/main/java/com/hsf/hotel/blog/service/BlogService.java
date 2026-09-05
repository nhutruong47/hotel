package com.hsf.hotel.blog.service;

import com.hsf.hotel.blog.model.Blog;
import com.hsf.hotel.blog.repository.BlogRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BlogService {

    private final BlogRepository blogRepository;

    public BlogService(BlogRepository blogRepository) {
        this.blogRepository = blogRepository;
    }

    public List<Blog> getAllBlogs() {
        return blogRepository.findAll();
    }

    public Optional<Blog> getBlogById(Integer id) {
        return blogRepository.findById(id);
    }
}
