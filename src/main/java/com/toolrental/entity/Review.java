package com.toolrental.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Отзыв по завершённой аренде. По одной аренде каждый участник пишет не больше одного отзыва:
 * - арендатор оценивает инструмент и арендодателя (tool заполнен, target = арендодатель);
 * - арендодатель оценивает арендатора (tool = null, target = арендатор).
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"booking_id", "author_id"}))
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Booking booking;

    // Кто написал отзыв
    @ManyToOne(optional = false)
    private User author;

    // О ком отзыв
    @ManyToOne(optional = false)
    private User target;

    // Об инструменте (только в отзыве арендатора)
    @ManyToOne
    private Tool tool;

    @Column(nullable = false)
    private int rating;

    @Column(name = "review_text", length = 2000)
    private String comment;

    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReviewPhoto> photos = new ArrayList<>();

    // Нужно только для того, чтобы жалобы удалялись вместе с отзывом
    @OneToMany(mappedBy = "review", cascade = CascadeType.REMOVE)
    private List<ReviewReport> reports = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public User getTarget() {
        return target;
    }

    public void setTarget(User target) {
        this.target = target;
    }

    public Tool getTool() {
        return tool;
    }

    public void setTool(Tool tool) {
        this.tool = tool;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<ReviewPhoto> getPhotos() {
        return photos;
    }
}
