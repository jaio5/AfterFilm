package alicanteweb.pelisapp.controller.web;

import alicanteweb.pelisapp.dto.ChatConversationDTO;
import alicanteweb.pelisapp.dto.UserPublicDTO;
import alicanteweb.pelisapp.entity.Review;
import alicanteweb.pelisapp.controller.EndpointSanitizer;
import alicanteweb.pelisapp.service.ChatService;
import alicanteweb.pelisapp.service.SocialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class SocialViewController {

    private final SocialService socialService;
    private final ChatService chatService;

    // ── User discovery ────────────────────────────────────────────────────────

    @GetMapping("/usuarios")
    public String usuarios(Model model, Authentication auth,
                           @RequestParam(required = false) String q) {
        String currentUsername = auth != null && auth.isAuthenticated() ? auth.getName() : null;
        List<UserPublicDTO> users = null;
        String safeQuery = EndpointSanitizer.optionalText(q, 50);
        boolean hasSearch = safeQuery != null;
        if (hasSearch) {
            users = socialService.searchUsers(safeQuery, currentUsername);
        } else {
            users = socialService.getSuggestedUsers(currentUsername);
        }
        model.addAttribute("q", safeQuery);
        model.addAttribute("users", users);
        model.addAttribute("hasSearch", hasSearch);
        return "social/usuarios";
    }

    // ── Public user profile ───────────────────────────────────────────────────

    @GetMapping("/usuario/{username}")
    public String publicProfile(@PathVariable String username, Model model, Authentication auth) {
        String currentUsername = auth != null && auth.isAuthenticated() ? auth.getName() : null;
        try {
            String safeUsername = EndpointSanitizer.username(username);
            UserPublicDTO profile = socialService.getPublicProfile(safeUsername, currentUsername);
            List<UserPublicDTO> followers = socialService.getFollowers(safeUsername, currentUsername);
            List<UserPublicDTO> following = socialService.getFollowing(safeUsername, currentUsername);
            List<Review> reviews = socialService.getUserReviews(safeUsername);
            model.addAttribute("profile", profile);
            model.addAttribute("followers", followers);
            model.addAttribute("following", following);
            model.addAttribute("reviews", reviews);
            model.addAttribute("currentUsername", currentUsername);
            model.addAttribute("isOwnProfile", safeUsername.equals(currentUsername));
            return "social/perfil-usuario";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", "Usuario no encontrado");
            return "error";
        }
    }

    // ── Social feed ───────────────────────────────────────────────────────────

    @GetMapping("/feed")
    public String feed(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";
        List<Review> feedReviews = socialService.getFeed(principal.getName(), 0, 30);
        List<UserPublicDTO> following = socialService.getFollowing(principal.getName(), principal.getName());
        model.addAttribute("feedReviews", feedReviews);
        model.addAttribute("following", following);
        model.addAttribute("hasFollowing", !following.isEmpty());
        return "social/feed";
    }

    // ── Chat ──────────────────────────────────────────────────────────────────

    @GetMapping("/chat")
    public String chatInbox(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";
        List<ChatConversationDTO> partners = chatService.getChatPartners(principal.getName());
        long unreadCount = chatService.getUnreadCount(principal.getName());
        model.addAttribute("partners", partners);
        model.addAttribute("unreadCount", unreadCount);
        model.addAttribute("activePartner", null);
        return "social/chat";
    }

    @GetMapping("/chat/{username}")
    public String chatWith(@PathVariable String username, Model model, Principal principal) {
        if (principal == null) return "redirect:/login";
        try {
            String safeUsername = EndpointSanitizer.username(username);
            UserPublicDTO otherUser = socialService.getPublicProfile(safeUsername, principal.getName());
            List<ChatConversationDTO> partners = chatService.getChatPartners(principal.getName());
            long unreadCount = chatService.getUnreadCount(principal.getName());
            model.addAttribute("otherUser", otherUser);
            model.addAttribute("partners", partners);
            model.addAttribute("unreadCount", unreadCount);
            model.addAttribute("activePartner", safeUsername);
            model.addAttribute("activeStarred", partners.stream()
                    .anyMatch(p -> p.username().equals(safeUsername) && p.starred()));
            model.addAttribute("currentUsername", principal.getName());
            return "social/chat";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", "Usuario no encontrado");
            return "error";
        }
    }
}
