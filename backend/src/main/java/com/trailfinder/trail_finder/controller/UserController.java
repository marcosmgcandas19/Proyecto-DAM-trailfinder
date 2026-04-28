package com.trailfinder.trail_finder.controller;

import com.trailfinder.trail_finder.model.User;
import com.trailfinder.trail_finder.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        // Validar email
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        if (user.getEmail() == null || !user.getEmail().matches(emailRegex)) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "El formato del email no es válido");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // Validar contraseña
        if (user.getPassword() == null || user.getPassword().length() < 7) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "La contraseña debe tener al menos 7 caracteres");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // Comprobar duplicados
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "El email ya está en uso");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "El nombre de usuario ya existe");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        user.setAvatar("https://api.realworld.io/images/avatar-" + (int)(Math.random() * 5 + 1) + ".jpg");
        User savedUser = userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody User loginRequest) {
        Optional<User> userOptional = userRepository.findByEmail(loginRequest.getEmail());
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (user.getPassword().equals(loginRequest.getPassword())) {
                return ResponseEntity.ok(user);
            }
        }
        Map<String, String> response = new HashMap<>();
        response.put("error", "Credenciales inválidas");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    // ─── GET /api/users/me?email={email} ─────────────────────────────────────
    // Devuelve el usuario actual por email (para sincronizar sesión con la BD)
    @GetMapping("/me")
    public ResponseEntity<?> getMe(@RequestParam String email) {
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Usuario no encontrado"));
        }
        return ResponseEntity.ok(user.get());
    }


    // Actualiza fullName, username, email y opcionalmente la contraseña
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Optional<User> opt = userRepository.findById(id);
        if (opt.isEmpty()) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Usuario no encontrado");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
        User user = opt.get();

        String newFullName = body.get("fullName");
        String newUsername = body.get("username");
        String newEmail    = body.get("email");
        String newPassword = body.get("password");  // puede ser null si no se cambia

        // Validar nombre completo: obligatorio y solo letras (con tildes, espacios, guiones y apóstrofes)
        if (newFullName == null || newFullName.isBlank()) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "El nombre completo es obligatorio");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        if (!newFullName.trim().matches("[A-Za-záéíóúüñÁÉÍÓÚÜÑàèìòùÀÈÌÒÙ\\s'\\-]+")) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "El nombre completo solo puede contener letras");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // Validar nombre de usuario: obligatorio
        if (newUsername == null || newUsername.isBlank()) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "El nombre de usuario es obligatorio");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // Validar email: formato correcto
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        if (newEmail == null || !newEmail.matches(emailRegex)) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "El formato del email no es válido");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // Comprobar unicidad de email excluyendo al propio usuario
        Optional<User> emailConflict = userRepository.findByEmail(newEmail);
        if (emailConflict.isPresent() && !emailConflict.get().getId().equals(id)) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "El email ya está en uso por otro usuario");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // Comprobar unicidad de username excluyendo al propio usuario
        Optional<User> usernameConflict = userRepository.findByUsername(newUsername);
        if (usernameConflict.isPresent() && !usernameConflict.get().getId().equals(id)) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "El nombre de usuario ya existe");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // Actualizar campos de perfil
        user.setFullName(newFullName.trim());
        user.setUsername(newUsername.trim());
        user.setEmail(newEmail.trim());

        // Contraseña: opcional; si se envía, debe tener al menos 7 caracteres
        if (newPassword != null && !newPassword.isBlank()) {
            if (newPassword.length() < 7) {
                Map<String, String> response = new HashMap<>();
                response.put("error", "La contraseña debe tener al menos 7 caracteres");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            user.setPassword(newPassword);
        }

        User saved = userRepository.save(user);
        return ResponseEntity.ok(saved);
    }

    // ─── PATCH /api/users/{id} ────────────────────────────────────────────────
    // Actualiza UN SOLO campo a la vez (fullName, username, email o password)
    @PatchMapping("/{id}")
    public ResponseEntity<?> patchUser(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Optional<User> opt = userRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Usuario no encontrado"));
        }
        User user = opt.get();

        // ── fullName ──
        if (body.containsKey("fullName")) {
            String value = body.get("fullName");
            if (value == null || value.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "El nombre completo es obligatorio"));
            }
            if (!value.trim().matches("[A-Za-záéíóúüñÁÉÍÓÚÜÑàèìòùÀÈÌÒÙ\\s'\\-]+")) {
                return ResponseEntity.badRequest().body(Map.of("error", "El nombre completo solo puede contener letras"));
            }
            user.setFullName(value.trim());
        }

        // ── username ──
        if (body.containsKey("username")) {
            String value = body.get("username");
            if (value == null || value.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "El nombre de usuario es obligatorio"));
            }
            Optional<User> conflict = userRepository.findByUsername(value.trim());
            if (conflict.isPresent() && !conflict.get().getId().equals(id)) {
                return ResponseEntity.badRequest().body(Map.of("error", "El nombre de usuario ya existe"));
            }
            user.setUsername(value.trim());
        }

        // ── email ──
        if (body.containsKey("email")) {
            String value = body.get("email");
            String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
            if (value == null || !value.matches(emailRegex)) {
                return ResponseEntity.badRequest().body(Map.of("error", "El formato del email no es válido"));
            }
            Optional<User> conflict = userRepository.findByEmail(value.trim());
            if (conflict.isPresent() && !conflict.get().getId().equals(id)) {
                return ResponseEntity.badRequest().body(Map.of("error", "El email ya está en uso por otro usuario"));
            }
            user.setEmail(value.trim());
        }

        // ── password ──
        if (body.containsKey("password")) {
            String value = body.get("password");
            if (value == null || value.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "La contraseña no puede estar vacía"));
            }
            if (value.length() < 7) {
                return ResponseEntity.badRequest().body(Map.of("error", "La contraseña debe tener al menos 7 caracteres"));
            }
            user.setPassword(value);
        }

        User saved = userRepository.save(user);
        return ResponseEntity.ok(saved);
    }
}
