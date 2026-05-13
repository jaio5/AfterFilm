package alicanteweb.pelisapp.controller.web;

import alicanteweb.pelisapp.constants.AppConstants;
import alicanteweb.pelisapp.entity.CategoryEntity;
import alicanteweb.pelisapp.entity.Movie;
import alicanteweb.pelisapp.repository.CategoryRepository;
import alicanteweb.pelisapp.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class HomeController {
    private final MovieRepository movieRepository;
    private final CategoryRepository categoryRepository;

    @GetMapping("/")
    public String home(Model model,
                       @RequestParam(value = "page", defaultValue = "0") int page,
                       @RequestParam(value = "size", defaultValue = "12") int size,
                       @RequestParam(value = "genre", required = false) String genre,
                       @RequestParam(value = "search", required = false) String search) {
        try {
            int validPage = Math.max(0, page);
            int validSize = Math.min(Math.max(1, size), AppConstants.MAX_PAGE_SIZE);
            Pageable pageable = PageRequest.of(validPage, validSize);
            Page<Movie> moviesPage;

            if (search != null && !search.trim().isEmpty()) {
                moviesPage = movieRepository.findByTitleContainingIgnoreCase(search.trim(), pageable);
                model.addAttribute("searchQuery", search);
            } else if (genre != null && !genre.trim().isEmpty()) {
                moviesPage = movieRepository.findByCategories_Name(genre.trim(), pageable);
            } else {
                moviesPage = movieRepository.findAll(pageable);
            }

            model.addAttribute("movies", moviesPage.getContent());
            model.addAttribute("selectedGenre", genre);
            List<CategoryEntity> categories = categoryRepository.findAll();
            model.addAttribute("categories", categories);
            model.addAttribute("currentPage", moviesPage.getNumber());
            model.addAttribute("totalPages", moviesPage.getTotalPages());
            model.addAttribute("totalElements", moviesPage.getTotalElements());
            model.addAttribute("hasNext", moviesPage.hasNext());
            model.addAttribute("hasPrevious", moviesPage.hasPrevious());
            return "index";
        } catch (Exception e) {
            log.error("Error cargando pagina principal: {}", e.getMessage());
            model.addAttribute("error", "Error cargando peliculas");
            return "error";
        }
    }
}
