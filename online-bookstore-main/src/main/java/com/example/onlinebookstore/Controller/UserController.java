package com.example.onlinebookstore.Controller;

import com.example.onlinebookstore.Repository.OrderDetailsRepository;
import com.example.onlinebookstore.Repository.UserRepository;
import org.springframework.ui.Model;
import org.bookhaven.OrderDetails;
import org.bookhaven.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/")
public class UserController {

    @Autowired
    private final UserRepository userRepository;

    @Autowired
    private final OrderDetailsRepository orderDetailsRepository;
    private final PasswordEncoder passwordEncoder;

    private static final Set<String> allowedUsers = new HashSet<>();

    static{
        allowedUsers.add("shukrah");
        allowedUsers.add("nour");
        allowedUsers.add("mia");
        allowedUsers.add("amber");
        allowedUsers.add("soukaina");
    }

    /**
     * Constructor for UserController.
     *
     * @param userRepository          Repository for managing user data.
     * @param orderDetailsRepository  Repository for managing order details.
     * @param passwordEncoder         Encoder for encrypting and verifying passwords.
     */
    public UserController(UserRepository userRepository, OrderDetailsRepository orderDetailsRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.orderDetailsRepository = orderDetailsRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Serve CSS files from the static resources.
     *
     * @param fileName The name of the CSS file.
     * @return ResponseEntity with the CSS file as a resource.
     */
    @GetMapping("/css/{fileName:.+}")
    public ResponseEntity<Resource> serveCss(@PathVariable String fileName) {
        Resource resource = new ClassPathResource("static/css/" + fileName);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/css")
                .body(resource);
    }

    /**
     * Serve image files from the static resources.
     *
     * @param fileName The name of the image file.
     * @return ResponseEntity with the image file as a resource.
     */
    @GetMapping("/images/{fileName:.+}")
    public ResponseEntity<Resource> serveImage(@PathVariable String fileName) {
        Resource resource = new ClassPathResource("static/images/" + fileName);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "image/png") // Change the content type as needed
                .body(resource);
    }

    /**
     * Serve JavaScript files from the static resources.
     *
     * @param fileName The name of the JavaScript file.
     * @return ResponseEntity with the JavaScript file as a resource.
     */
    @GetMapping("/js/{fileName:.+}")
    public ResponseEntity<Resource> serveJs(@PathVariable String fileName) {
        Resource resource = new ClassPathResource("static/js/" + fileName);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/javascript")
                .body(resource);
    }

    /* REGISTRATION,LOGIN AND AUTHORISATION */

    /**
     * Handle user registration.
     *
     * @param email    User's email address.
     * @param username User's chosen username.
     * @param password User's chosen password.
     * @param session  HttpSession for managing user sessions.
     * @return ResponseEntity indicating the success or failure of the registration.
     */
    @PostMapping("/register")
    public ResponseEntity<String> registerUser(
            @RequestParam("email") String email,
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            HttpSession session
    ) {
        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setPassword(password);

        if (userRepository.existsByUsername(username)) {
            return ResponseEntity.badRequest().body("Username already exists");
        }
        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.badRequest().body("Email already exists");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);

        session.setAttribute("loggedIn", true);
        session.setAttribute("username", username);

        return ResponseEntity.ok("User registered successfully");
    }

    /**
     * Handle user login.
     *
     * @param username User's username for login.
     * @param password User's password for login.
     * @param session  HttpSession for managing user sessions.
     * @return ResponseEntity indicating the success or failure of the login.
     */
    @PostMapping("/login")
    public ResponseEntity<String> loginUser(
            @RequestParam("loginUsername") String username,
            @RequestParam("loginPassword") String password,
            HttpSession session
    ) {
        // Retrieve the user from the database by username
        User user = userRepository.findByUsername(username);

        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity.badRequest().body("Invalid username or password");
        }

        session.setAttribute("loggedIn", true);
        session.setAttribute("username", username);

        return ResponseEntity.ok("User logged in successfully");
    }

    /**
     * Handle user logout.
     *
     * @param request HttpServletRequest for handling the session.
     * @return String representing the redirect URL after logout.
     */
    @GetMapping("/logout")
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate(); // Invalidate the session
        }
        return "redirect:/"; // Redirect to the welcome page or any other desired page
    }

    /**
     * Display the unauthorized page.
     *
     * @return The unauthorised page view name.
     */
    @GetMapping("/unauthorised")
    public String unauthorised() {
        return "unauthorised";
    }

    @GetMapping("/notloggedin")
    public String notloggedin() {
        return "notloggedin";
    }

    /* INDEX PAGES */

    /**
     * Model attribute method to provide a User instance to views.
     *
     * @return A new User instance.
     */
    @ModelAttribute("user")
    public User user() {
        return new User();
    }

    /**
     * Display the welcome page.
     *
     * @return The welcome page view name.
     */
    @GetMapping("/")
    public String welcome() {
        return "welcomepage";
    }


    /**
     * Display the user index page.
     *
     * @return The user index page view name.
     */
    @GetMapping("/index")
    public String userIndex() {
        return "index";
    }


    /**
     * Display the user profile page.
     *
     * @param model   Model for adding attributes.
     * @param session HttpSession for retrieving user information.
     * @return The user profile page view name.
     */
    @GetMapping("/index/my-profile")
    public String userProfile(Model model, HttpSession session) {

        Boolean loggedIn = (Boolean) session.getAttribute("loggedIn");
        String username = (String) session.getAttribute("username");
        if (loggedIn != null && loggedIn) {
            User user = userRepository.findByUsername(username);

            model.addAttribute("fullName", user.getFullName());
            model.addAttribute("address", user.getAddress());
            model.addAttribute("country", user.getCountry());
            model.addAttribute("email", user.getEmail());
            model.addAttribute("phoneNumber", user.getPhoneNumber());

            return "profile/profile-info";
        } else {
            return "redirect:/notloggedin";
        }


    }


    /**
     * Display the user orders page.
     *
     * @param model   Model for adding attributes.
     * @param session HttpSession for retrieving user information.
     * @return The user orders page view name.
     */
    @GetMapping("/index/my-orders")
    public String userOrders(Model model, HttpSession session) {

        Boolean loggedIn = (Boolean) session.getAttribute("loggedIn");
        if (loggedIn != null && loggedIn) {
            String username = (String) session.getAttribute("username");
            User user = userRepository.findByUsername(username);

            List<OrderDetails> userOrders = orderDetailsRepository.findByUsername(username);

            userOrders.sort(Comparator.comparing(OrderDetails::getCreatedAt).reversed());
            model.addAttribute("userOrders", userOrders);
            return "profile/orders";
        } else {
            return "redirect:/notloggedin";
        }
    }

    /**
     * Display the user wishlist page.
     *
     * @return The user wishlist page view name.
     */
    @GetMapping("/index/my-wishlist")
    public String userWishList(HttpSession session) {

        Boolean loggedIn = (Boolean) session.getAttribute("loggedIn");
        if (loggedIn != null && loggedIn) {
            System.out.println("SHOWING WISHLIST PAGE");
            return "profile/wishlist";
        } else {
            return "redirect:/notloggedin";
        }
    }

    /**
     * Display the user settings page.
     *
     * @return The user settings page view name.
     */
    @GetMapping("/index/settings")
    public String userSettings(HttpSession session) {

        Boolean loggedIn = (Boolean) session.getAttribute("loggedIn");
        if (loggedIn != null && loggedIn) {
            System.out.println("SHOWING SETTINGS PAGE");
            return "profile/settings";
        } else {
            return "redirect:/notloggedin";
        }
    }

    /**
     * Display the shipping and delivery information page.
     *
     * @return String representing the view name.
     */
    @GetMapping("/shipping-and-delivery")
    public String shippingAndDelivery() {
        return "footer/shipping";
    }

    /**
     * Display the about us information page.
     *
     * @return String representing the view name.
     */
    @GetMapping("/about-us")
    public String aboutUs() {
        return "footer/about";
    }

    /**
     * Display the privacy policy page.
     *
     * @return String representing the view name.
     */
    @GetMapping("/privacy-policy")
    public String privacyPolicy() {
        return "footer/privacy";
    }

    /**
     * Display the terms of use page.
     *
     * @return String representing the view name.
     */
    @GetMapping("/terms-of-use")
    public String termsOfUse() {
        return "footer/terms";
    }

    /* ORDER-MANAGEMENT */

    /**
     * Display the order management page.
     *
     * @param model   Model for adding attributes.
     * @param session HttpSession for checking user roles.
     * @return The order management page view name.
     */
    @GetMapping("/order-management")
    public String orderManagement(Model model, HttpSession session) {
        Boolean loggedIn = (Boolean) session.getAttribute("loggedIn");
        String username = (String) session.getAttribute("username");

        if (loggedIn != null && loggedIn && allowedUsers.contains(username)) {
            List<OrderDetails> orderList = orderDetailsRepository.findAll();
            model.addAttribute("orderList", orderList);
            System.out.println("SHOWING ORDER MANAGEMENT PAGE");
            return "order-management";
        } else {
            return "redirect:/unauthorised";
        }
    }

    /**
     * Get a list of all orders.
     *
     * @return List of OrderDetails representing all orders.
     */
    @GetMapping("/getOrders")
    @ResponseBody
    public List<OrderDetails> getOrders() {
        List<OrderDetails> orderDetailsList = orderDetailsRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        orderDetailsList.forEach(orderDetails -> orderDetails.setStatus(orderDetails.getStatus()));

        return orderDetailsList;
    }

    /**
     * Retrieve order details by order number.
     *
     * @param orderNumber Order number to identify the order.
     * @return ResponseEntity containing the order details or an error response.
     */
    @GetMapping("/getOrderDetails")
    @ResponseBody
    public ResponseEntity<OrderDetails> getOrderDetails(@RequestParam String orderNumber) {
        try {

            OrderDetails orderDetails = orderDetailsRepository.findByOrderNumber(orderNumber);
            return ResponseEntity.ok(orderDetails);
        } catch (Exception e) {

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * Save order details to the database.
     *
     * @param orderDetails OrderDetails object containing order information.
     * @param session      HttpSession for managing user sessions.
     * @return ResponseEntity indicating the success or failure of saving order details.
     */
    @PostMapping( "/saveOrder")
    @ResponseBody
    public ResponseEntity<String> saveOrderDetails(@RequestBody OrderDetails orderDetails, HttpSession session) {

        try {
            String username = (String) session.getAttribute("username");

            if (username == null) {
                username = "GUEST";
            }

            orderDetails.setUsername(username);
            orderDetails.setCreatedAt(LocalDateTime.now());

            if (username.equals("GUEST")) {
                // For non-logged-in users (guests), set email and phone directly from orderDetails
                orderDetails.setEmail(orderDetails.getEmail());
                orderDetails.setPhoneNumber(orderDetails.getPhoneNumber());

            } else {
                // If the user is logged in, retrieve user details
                User user = userRepository.findByUsername(username);
                orderDetails.setEmail(user.getEmail());
                orderDetails.setPhoneNumber(user.getPhoneNumber());
            }
            orderDetailsRepository.save(orderDetails);
            return ResponseEntity.ok("Order details saved successfully");
        } catch (Exception e) {

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error saving order details");
        }

    }

    /**
     * Update the status of an order.
     *
     * @param orderNumber Order number to identify the order.
     * @param status      New status for the order.
     * @return ResponseEntity indicating the success or failure of updating the order status.
     */
    @PutMapping("/updateOrderStatus")
    public ResponseEntity<String> updateOrderStatus(@RequestParam String orderNumber, @RequestParam String status) {

        OrderDetails order = orderDetailsRepository.findByOrderNumber(orderNumber);
        if (order != null) {
            order.setStatus(status);
            orderDetailsRepository.save(order);
            return ResponseEntity.ok("Order status updated successfully");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Order not found");
        }
    }

    /**
     * Update user profile information.
     *
     * @param updatedUser User object containing updated user information.
     * @param session     HttpSession for managing user sessions.
     * @return String representing the redirect URL after updating the profile.
     */
    @PostMapping("/updateProfile")
    public String updateProfile(@ModelAttribute User updatedUser, HttpSession session) {
        String username = (String) session.getAttribute("username");
        User currentUser = userRepository.findByUsername(username);


        if (updatedUser.getFullName() != null && !updatedUser.getFullName().isEmpty()) {
            currentUser.setFullName(updatedUser.getFullName());
        }

        if (updatedUser.getAddress() != null && !updatedUser.getAddress().isEmpty()) {
            currentUser.setAddress(updatedUser.getAddress());
        }

        if (updatedUser.getCountry() != null && !updatedUser.getCountry().isEmpty()) {
            currentUser.setCountry(updatedUser.getCountry());
        }

        if (updatedUser.getPhoneNumber() != null && !updatedUser.getPhoneNumber().isEmpty()) {
            currentUser.setPhoneNumber(updatedUser.getPhoneNumber());
        }

        userRepository.save(currentUser);

        session.setAttribute("username", currentUser.getUsername());

        return "redirect:/index/my-profile";
    }

    /**
     * Update user email address.
     *
     * @param newEmail           New email address for the user.
     * @param session            HttpSession for managing user sessions.
     * @param redirectAttributes RedirectAttributes for adding flash attributes.
     * @return String representing the redirect URL after updating the email.
     */
    @PostMapping("/updateEmail")
    public String updateEmail(
            @RequestParam("newEmail") String newEmail,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        String username = (String) session.getAttribute("username");
        User currentUser = userRepository.findByUsername(username);

        // Check if the new email is not empty
        if (newEmail != null && !newEmail.isEmpty()) {
            currentUser.setEmail(newEmail);
            userRepository.save(currentUser);
            redirectAttributes.addFlashAttribute("successMessage", "Email updated successfully");
            return "redirect:/index/my-profile";
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "New email cannot be empty");
            return "redirect:/index/settings#emailForm";
        }
    }

    /**
     * Update user password.
     *
     * @param currentPassword     Current password for verification.
     * @param newPassword         New password for the user.
     * @param session             HttpSession for managing user sessions.
     * @param redirectAttributes  RedirectAttributes for adding flash attributes.
     * @return String representing the redirect URL after updating the password.
     */
    @PostMapping("/updatePassword")
    public String updatePassword(
            @RequestParam("currentPassword") String currentPassword,
            @RequestParam("newPassword") String newPassword,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        String username = (String) session.getAttribute("username");
        User currentUser = userRepository.findByUsername(username);

        if (passwordEncoder.matches(currentPassword, currentUser.getPassword())) {

            currentUser.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(currentUser);

            redirectAttributes.addFlashAttribute("successMessage", "Password updated successfully");
            return "redirect:/index/my-profile";
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Current password is incorrect");
            return "redirect:/index/settings#passwordForm";
        }
    }

    /**
     * Delete user account.
     *
     * @param session            HttpSession for managing user sessions.
     * @param redirectAttributes RedirectAttributes for adding flash attributes.
     * @return ResponseEntity indicating the success or failure of deleting the account.
     */
    @PostMapping("/deleteAccount")
    public ResponseEntity<String> deleteAccount(HttpSession session, RedirectAttributes redirectAttributes) {
        String username = (String) session.getAttribute("username");

        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not logged in");
        }

        User user = userRepository.findByUsername(username);

        if (user != null) {
            userRepository.delete(user);

            session.invalidate();

            return ResponseEntity.ok("Account deleted successfully");
        } else {

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Account not found");
        }
    }


}
