package com.trailfinder.trail_finder.controller;

import com.trailfinder.trail_finder.model.ChatMessage;
import com.trailfinder.trail_finder.model.Conversation;
import com.trailfinder.trail_finder.model.User;
import com.trailfinder.trail_finder.repository.ChatMessageRepository;
import com.trailfinder.trail_finder.repository.ConversationRepository;
import com.trailfinder.trail_finder.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private UserRepository userRepository;

    // DTO para la lista de chats (vista resumida)
    public static class ConversationSummaryDTO {
        public Long id;
        public Long otherUserId;
        public String otherUsername;
        public String otherAvatar;
        public String lastMessage;
        public String lastMessageTime;
        public int unreadCount;
        public boolean otherOnline;
    }

    // DTO para un mensaje individual
    public static class MessageDTO {
        public Long id;
        public String text;
        public String time;
        public boolean sent;        // true si lo envió el currentUser
        public boolean read;
    }

    // DTO para el detalle de una conversación
    public static class ConversationDetailDTO {
        public Long id;
        public Long otherUserId;
        public String otherUsername;
        public String otherAvatar;
        public List<MessageDTO> messages;
    }

    // ─── GET /api/conversations?userId={id} ───────────────────────────────────
    // Devuelve la lista resumida de conversaciones del usuario
    @GetMapping
    public ResponseEntity<?> getConversations(@RequestParam Long userId) {
        List<Conversation> conversations = conversationRepository.findByUserId(userId);
        List<ConversationSummaryDTO> result = new ArrayList<>();

        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM");

        for (Conversation conv : conversations) {
            ConversationSummaryDTO dto = new ConversationSummaryDTO();
            dto.id = conv.getId();

            // Determinar cuál es el "otro" usuario
            User other = conv.getUser1().getId().equals(userId)
                    ? conv.getUser2()
                    : conv.getUser1();

            dto.otherUserId = other.getId();
            dto.otherUsername = other.getUsername();
            dto.otherAvatar = other.getAvatar();
            dto.otherOnline = false; // sin WebSocket, siempre false por ahora

            // Último mensaje
            List<ChatMessage> msgs = conv.getMessages();
            if (!msgs.isEmpty()) {
                ChatMessage last = msgs.get(msgs.size() - 1);
                dto.lastMessage = last.getText();
                dto.lastMessageTime = last.getSentAt().format(timeFmt);
                // Mensajes no leídos enviados por el otro usuario
                dto.unreadCount = (int) msgs.stream()
                        .filter(m -> !m.getSender().getId().equals(userId) && !m.isReadByReceiver())
                        .count();
            } else {
                dto.lastMessage = "";
                dto.lastMessageTime = conv.getCreatedAt().format(dateFmt);
                dto.unreadCount = 0;
            }

            result.add(dto);
        }

        return ResponseEntity.ok(result);
    }

    // ─── GET /api/conversations/{id}?userId={currentUserId} ──────────────────
    // Devuelve el detalle completo de una conversación (con todos los mensajes)
    @GetMapping("/{id}")
    public ResponseEntity<?> getConversationDetail(
            @PathVariable Long id,
            @RequestParam Long userId) {

        Optional<Conversation> opt = conversationRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Conversación no encontrada"));
        }
        Conversation conv = opt.get();

        // Marcar como leídos los mensajes recibidos por el currentUser
        List<ChatMessage> msgs = chatMessageRepository
                .findByConversationIdOrderBySentAtAsc(id);
        msgs.stream()
                .filter(m -> !m.getSender().getId().equals(userId) && !m.isReadByReceiver())
                .forEach(m -> {
                    m.setReadByReceiver(true);
                    chatMessageRepository.save(m);
                });

        User other = conv.getUser1().getId().equals(userId)
                ? conv.getUser2()
                : conv.getUser1();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");

        ConversationDetailDTO dto = new ConversationDetailDTO();
        dto.id = conv.getId();
        dto.otherUserId = other.getId();
        dto.otherUsername = other.getUsername();
        dto.otherAvatar = other.getAvatar();
        dto.messages = msgs.stream().map(m -> {
            MessageDTO mdto = new MessageDTO();
            mdto.id = m.getId();
            mdto.text = m.getText();
            mdto.time = m.getSentAt().format(fmt);
            mdto.sent = m.getSender().getId().equals(userId);
            mdto.read = m.isReadByReceiver();
            return mdto;
        }).toList();

        return ResponseEntity.ok(dto);
    }

    // ─── POST /api/conversations ──────────────────────────────────────────────
    // Crea una conversación nueva entre dos usuarios (o devuelve la existente)
    @PostMapping
    public ResponseEntity<?> createConversation(@RequestBody Map<String, Long> body) {
        Long user1Id = body.get("user1Id");
        Long user2Id = body.get("user2Id");

        if (user1Id == null || user2Id == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Se requieren user1Id y user2Id"));
        }

        // Si ya existe, devolverla
        Optional<Conversation> existing = conversationRepository.findBetweenUsers(user1Id, user2Id);
        if (existing.isPresent()) {
            return ResponseEntity.ok(Map.of("id", existing.get().getId()));
        }

        Optional<User> u1 = userRepository.findById(user1Id);
        Optional<User> u2 = userRepository.findById(user2Id);
        if (u1.isEmpty() || u2.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Usuario no encontrado"));
        }

        Conversation conv = new Conversation();
        conv.setUser1(u1.get());
        conv.setUser2(u2.get());
        conversationRepository.save(conv);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", conv.getId()));
    }

    // ─── POST /api/conversations/{id}/messages ────────────────────────────────
    // Envía un mensaje en una conversación
    @PostMapping("/{id}/messages")
    public ResponseEntity<?> sendMessage(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {

        Optional<Conversation> opt = conversationRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Conversación no encontrada"));
        }

        Long senderId = Long.valueOf(body.get("senderId").toString());
        String text = body.get("text").toString();

        Optional<User> sender = userRepository.findById(senderId);
        if (sender.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Sender no encontrado"));
        }

        ChatMessage msg = new ChatMessage();
        msg.setConversation(opt.get());
        msg.setSender(sender.get());
        msg.setText(text);
        chatMessageRepository.save(msg);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", msg.getId()));
    }
}
