package alicanteweb.pelisapp.controller.web;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class BooksHomeController {

    @GetMapping("/libros")
    public String booksIndex(Model model) {
        return "books-index";
    }
}
