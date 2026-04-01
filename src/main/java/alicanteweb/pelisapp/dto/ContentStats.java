package alicanteweb.pelisapp.dto;

public record ContentStats(int totalReviews, double averageRating, int[] starDistribution) {

    public String getAverageRatingFormatted() {
        return totalReviews > 0 ? String.format("%.1f", averageRating) : "-";
    }

    public int getStarPercentage(int star) {
        if (totalReviews == 0) return 0;
        int count = (star >= 1 && star <= 5) ? starDistribution[star - 1] : 0;
        return (int) Math.round(count * 100.0 / totalReviews);
    }
}
