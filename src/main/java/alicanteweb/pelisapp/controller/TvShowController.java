package alicanteweb.pelisapp.controller;

import alicanteweb.pelisapp.dto.TvShowDetailDTO;
import alicanteweb.pelisapp.dto.TvShowListDTO;
import alicanteweb.pelisapp.service.TvShowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/series")
@RequiredArgsConstructor
@Slf4j
public class TvShowController {

    private final TvShowService tvShowService;

    @GetMapping("")
    public ResponseEntity<Page<TvShowListDTO>> getAllSeries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(tvShowService.getAllSeries(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TvShowDetailDTO> getSeriesById(@PathVariable Long id) {
        return tvShowService.getSeriesById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<List<TvShowListDTO>> searchSeries(@RequestParam String query) {
        return ResponseEntity.ok(tvShowService.searchSeries(query));
    }

    @GetMapping("/top-rated-this-month")
    public ResponseEntity<TvShowListDTO> getTopRatedThisMonth() {
        return tvShowService.getTopRatedThisMonth()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/by-genre")
    public ResponseEntity<Page<TvShowListDTO>> getSeriesByGenre(
            @RequestParam String genre,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(tvShowService.getSeriesByGenre(genre, pageable));
    }

    @GetMapping("/genres")
    public ResponseEntity<List<String>> getAvailableGenres() {
        return ResponseEntity.ok(tvShowService.getAvailableGenres());
    }
}
