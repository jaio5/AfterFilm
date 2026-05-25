package alicanteweb.pelisapp.service;

import alicanteweb.pelisapp.dto.UserReviewDTO;
import alicanteweb.pelisapp.entity.Book;
import alicanteweb.pelisapp.entity.Movie;
import alicanteweb.pelisapp.entity.Review;
import alicanteweb.pelisapp.entity.TvShow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserProfileDtoMapper {

    private final ImageUrlService imageUrlService;

    public UserReviewDTO toReviewDto(Review review) {
        ContentInfo content = contentInfo(review);
        return new UserReviewDTO(
                review.getId(),
                review.getText(),
                review.getStars(),
                review.getCreatedAt(),
                review.getUpdatedAt(),
                review.getLikesCount(),
                content.contentType(),
                content.contentId(),
                content.contentTitle(),
                content.contentPoster());
    }

    private ContentInfo contentInfo(Review review) {
        Movie movie = review.getMovie();
        if (movie != null) {
            return new ContentInfo("movie", movie.getId(), movie.getTitle(), imageUrlService.moviePosterUrl(movie, "w500"));
        }

        TvShow series = review.getSeries();
        if (series != null) {
            return new ContentInfo("series", series.getId(), series.getTitle(), imageUrlService.seriesPosterUrl(series, "w500"));
        }

        Book book = review.getBook();
        if (book != null) {
            return new ContentInfo("book", book.getId(), book.getTitle(), book.getCoverUrl());
        }

        return new ContentInfo(null, null, null, null);
    }

    private record ContentInfo(String contentType, Long contentId, String contentTitle, String contentPoster) {}
}
