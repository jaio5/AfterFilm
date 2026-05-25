package alicanteweb.pelisapp.service;

import alicanteweb.pelisapp.dto.UserReviewDTO;
import alicanteweb.pelisapp.entity.Review;
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
        if (review.getMovie() != null) {
            return new ContentInfo("movie", review.getMovie().getId(), review.getMovie().getTitle(),
                    imageUrlService.reviewContentPosterUrl(review, "w500"));
        }

        if (review.getSeries() != null) {
            return new ContentInfo("series", review.getSeries().getId(), review.getSeries().getTitle(),
                    imageUrlService.reviewContentPosterUrl(review, "w500"));
        }

        if (review.getBook() != null) {
            return new ContentInfo("book", review.getBook().getId(), review.getBook().getTitle(),
                    imageUrlService.reviewContentPosterUrl(review, "w500"));
        }

        return new ContentInfo(null, null, null, null);
    }

    private record ContentInfo(String contentType, Long contentId, String contentTitle, String contentPoster) {}
}
