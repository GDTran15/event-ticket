package com.duong.eventhub;

import static org.assertj.core.api.Assertions.assertThat;

import com.duong.eventhub.auth.JwtService;
import com.duong.eventhub.event.EventRepository;
import com.duong.eventhub.event.EventStatus;
import com.duong.eventhub.ticket.TicketTypeRepository;
import com.duong.eventhub.user.AppUser;
import com.duong.eventhub.user.AppUserRepository;
import com.duong.eventhub.user.Role;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "app.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
    "app.bootstrap.admin-email=", "app.bootstrap.admin-password="
})
@Testcontainers
class EventCreationIntegrationTest {
    @Container static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Value("${local.server.port}") int port;
    @Autowired AppUserRepository users;
    @Autowired EventRepository events;
    @Autowired TicketTypeRepository types;
    @Autowired
    JwtService tokens;
    @Autowired ObjectMapper json;
    @Autowired TransactionTemplate transactions;

    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private AppUser organizer;
    private AppUser otherOrganizer;
    private AppUser customer;
    private Instant startsAt;
    private Instant endsAt;

    @BeforeEach
    void setUp() {
        types.deleteAll();
        events.deleteAll();
        users.deleteAll();
        organizer = user("organizer@example.com", Role.ORGANIZER);
        otherOrganizer = user("other@example.com", Role.ORGANIZER);
        customer = user("customer@example.com", Role.CUSTOMER);
        startsAt = Instant.now().plus(10, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        endsAt = startsAt.plus(3, ChronoUnit.HOURS);
    }

    @Test
    void createsDraftEventAndMultipleTicketTypesLinkedToIt() throws Exception {
        HttpResponse<String> eventResponse = post("/api/events", tokens.create(organizer), eventBody());
        assertThat(eventResponse.statusCode()).isEqualTo(201);
        JsonNode event = json.readTree(eventResponse.body());
        UUID eventId = UUID.fromString(event.get("id").asString());
        assertThat(eventResponse.headers().firstValue("Location")).contains("/api/events/" + eventId);
        assertThat(event.get("organizerId").asString()).isEqualTo(organizer.getId().toString());
        assertThat(event.get("status").asString()).isEqualTo("DRAFT");
        assertThat(event.get("title").asString()).isEqualTo("Launch party");
        assertThat(event.get("createdAt").isNull()).isFalse();
        assertThat(event.has("organizer")).isFalse();
        assertThat(events.findById(eventId).orElseThrow().getStatus()).isEqualTo(EventStatus.DRAFT);

        HttpResponse<String> ticketResponse = post(ticketPath(eventId), tokens.create(organizer), ticketBody());
        assertThat(ticketResponse.statusCode()).isEqualTo(201);
        JsonNode ticket = json.readTree(ticketResponse.body());
        UUID typeId = UUID.fromString(ticket.get("id").asString());
        assertThat(ticketResponse.headers().firstValue("Location")).contains(ticketPath(eventId) + "/" + typeId);
        assertThat(ticket.get("eventId").asString()).isEqualTo(eventId.toString());
        assertThat(ticket.get("name").asString()).isEqualTo("General admission");
        assertThat(ticket.get("availableQuantity").asInt()).isEqualTo(100);
        transactions.executeWithoutResult(status -> {
            var persisted = types.findById(typeId).orElseThrow();
            assertThat(persisted.getEvent().getId()).isEqualTo(eventId);
            assertThat(persisted.getPrice()).isEqualByComparingTo("25.50");
            assertThat(persisted.getCapacity()).isEqualTo(100);
            assertThat(persisted.getAvailableQuantity()).isEqualTo(100);
            assertThat(persisted.getSaleEndsAt()).isEqualTo(startsAt);
        });

        String freeTicket = ticketBody().replace("General admission", "Free entry").replace("25.50", "0.00");
        assertThat(post(ticketPath(eventId), tokens.create(organizer), freeTicket).statusCode()).isEqualTo(201);
        assertThat(types.count()).isEqualTo(2);
    }

    @Test
    void rejectsCustomersAndUnassignedStaffForBothCreateEndpoints() throws Exception {
        UUID eventId = createEvent();
        AppUser staff = user("staff@example.com", Role.STAFF);
        for (AppUser user : new AppUser[] {customer, staff}) {
            assertThat(post("/api/events", tokens.create(user), eventBody()).statusCode()).isEqualTo(403);
            assertThat(post(ticketPath(eventId), tokens.create(user), ticketBody()).statusCode()).isEqualTo(403);
        }
        assertThat(events.count()).isEqualTo(1);
        assertThat(types.count()).isZero();
    }

    @Test
    void rejectsMissingAndInvalidBearerTokens() throws Exception {
        UUID eventId = createEvent();
        for (String token : new String[] {"", "invalid-token"}) {
            assertThat(post("/api/events", token, eventBody()).statusCode()).isEqualTo(401);
            assertThat(post(ticketPath(eventId), token, ticketBody()).statusCode()).isEqualTo(401);
        }
        assertThat(types.count()).isZero();
    }

    @Test
    void rejectsTicketTypeForAnotherOrganizersEvent() throws Exception {
        UUID eventId = createEvent();
        var response = post(ticketPath(eventId), tokens.create(otherOrganizer), ticketBody());
        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(types.count()).isZero();
    }

    @Test
    void rejectsTicketTypeForMissingEvent() throws Exception {
        var response = post(ticketPath(UUID.randomUUID()), tokens.create(organizer), ticketBody());
        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(types.count()).isZero();
    }

    @Test
    void duplicateNameIsScopedToItsEventAndDoesNotInsertAnotherRow() throws Exception {
        UUID firstEvent = createEvent();
        assertThat(post(ticketPath(firstEvent), tokens.create(organizer), ticketBody()).statusCode()).isEqualTo(201);
        assertThat(post(ticketPath(firstEvent), tokens.create(organizer), ticketBody()).statusCode()).isEqualTo(409);
        UUID secondEvent = createEvent();
        assertThat(post(ticketPath(secondEvent), tokens.create(organizer), ticketBody()).statusCode()).isEqualTo(201);
        assertThat(types.count()).isEqualTo(2);
    }

    @Test
    void rejectsEqualOrReversedEventDatesWithoutSaving() throws Exception {
        for (Instant end : new Instant[] {startsAt, startsAt.minusSeconds(1)}) {
            String body = eventBody().replace(endsAt.toString(), end.toString());
            var response = post("/api/events", tokens.create(organizer), body);
            assertThat(response.statusCode()).isEqualTo(400);
            assertThat(json.readTree(response.body()).get("detail").asString()).contains("endsAt");
        }
        assertThat(events.count()).isZero();
    }

    @Test
    void rejectsInvalidSaleWindowsWithoutSaving() throws Exception {
        UUID eventId = createEvent();
        Instant saleStart = startsAt.minus(1, ChronoUnit.DAYS);
        for (Instant saleEnd : new Instant[] {saleStart, saleStart.minusSeconds(1), endsAt.plusSeconds(1)}) {
            String body = ticketBody().replace(startsAt.toString(), saleEnd.toString());
            assertThat(post(ticketPath(eventId), tokens.create(organizer), body).statusCode()).isEqualTo(400);
        }
        assertThat(types.count()).isZero();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidEventFields")
    void rejectsInvalidEventFields(String field, String value) throws Exception {
        JsonNode body = json.readTree(eventBody());
        var request = (tools.jackson.databind.node.ObjectNode) body;
        request.set(field, json.readTree(value));
        var response = post("/api/events", tokens.create(organizer), json.writeValueAsString(request));
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(json.readTree(response.body()).get("errors").has(field)).isTrue();
        assertThat(events.count()).isZero();
    }

    static Stream<Arguments> invalidEventFields() {
        return Stream.of(
            Arguments.of("title", "\"   \""),
            Arguments.of("title", "\"" + "a".repeat(201) + "\""),
            Arguments.of("venue", "\"\""),
            Arguments.of("startsAt", "null"),
            Arguments.of("startsAt", "\"2020-01-01T00:00:00Z\""),
            Arguments.of("endsAt", "null")
        );
    }

    @ParameterizedTest(name = "{0}={1}")
    @MethodSource("invalidTicketFields")
    void rejectsInvalidTicketFields(String field, String value) throws Exception {
        UUID eventId = createEvent();
        var request = (tools.jackson.databind.node.ObjectNode) json.readTree(ticketBody());
        request.set(field, json.readTree(value));
        var response = post(ticketPath(eventId), tokens.create(organizer), json.writeValueAsString(request));
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(json.readTree(response.body()).get("errors").has(field)).isTrue();
        assertThat(types.count()).isZero();
    }

    static Stream<Arguments> invalidTicketFields() {
        return Stream.of(
            Arguments.of("name", "\" \""),
            Arguments.of("name", "\"" + "a".repeat(101) + "\""),
            Arguments.of("price", "-1.00"),
            Arguments.of("price", "1.234"),
            Arguments.of("price", "10000000000.00"),
            Arguments.of("price", "null"),
            Arguments.of("capacity", "0"),
            Arguments.of("capacity", "-1"),
            Arguments.of("capacity", "null"),
            Arguments.of("saleStartsAt", "null"),
            Arguments.of("saleEndsAt", "null")
        );
    }

    @Test
    void rejectsMalformedJsonAndEventId() throws Exception {
        assertThat(post("/api/events", tokens.create(organizer), "{").statusCode()).isEqualTo(400);
        assertThat(post("/api/events/not-a-uuid/ticket-types", tokens.create(organizer), ticketBody()).statusCode())
            .isEqualTo(400);
        assertThat(events.count()).isZero();
        assertThat(types.count()).isZero();
    }

    private AppUser user(String email, Role role) {
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setDisplayName(role.name());
        user.setPasswordHash("test-placeholder");
        user.setEnabled(true);
        user.getRoles().add(role);
        return users.saveAndFlush(user);
    }

    private UUID createEvent() throws Exception {
        var response = post("/api/events", tokens.create(organizer), eventBody());
        assertThat(response.statusCode()).isEqualTo(201);
        return UUID.fromString(json.readTree(response.body()).get("id").asString());
    }

    private String eventBody() {
        return """
            {"title":"  Launch party  ","description":"Our launch event","venue":"Main hall",
             "startsAt":"%s","endsAt":"%s"}
            """.formatted(startsAt, endsAt);
    }

    private String ticketBody() {
        return """
            {"name":"  General admission  ","price":25.50,"capacity":100,
             "saleStartsAt":"%s","saleEndsAt":"%s"}
            """.formatted(startsAt.minus(1, ChronoUnit.DAYS), startsAt);
    }

    private String ticketPath(UUID eventId) {
        return "/api/events/" + eventId + "/ticket-types";
    }

    private HttpResponse<String> post(String path, String token, String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .timeout(Duration.ofSeconds(15))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body));
        if (!token.isEmpty()) {
            request.header("Authorization", "Bearer " + token);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
