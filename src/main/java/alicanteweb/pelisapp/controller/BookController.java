package alicanteweb.pelisapp.controller;

import alicanteweb.pelisapp.dto.BookDetailDTO;
import alicanteweb.pelisapp.dto.BookListDTO;
import alicanteweb.pelisapp.service.BookService;
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
@RequestMapping("/api/books")
@RequiredArgsConstructor
@Slf4j
public class BookController {

    private final BookService bookService;

    @GetMapping("")
    public ResponseEntity<Page<BookListDTO>> getAllBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(bookService.getAllBooks(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookDetailDTO> getBookById(@PathVariable Long id) {
        return bookService.getBookById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<List<BookListDTO>> searchBooks(@RequestParam String query) {
        return ResponseEntity.ok(bookService.searchBooks(query));
    }

    @GetMapping("/by-category")
    public ResponseEntity<Page<BookListDTO>> getBooksByCategory(
            @RequestParam String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(bookService.getBooksByCategory(category, pageable));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> getAvailableCategories() {
        return ResponseEntity.ok(bookService.getAvailableCategories());
    }
}
