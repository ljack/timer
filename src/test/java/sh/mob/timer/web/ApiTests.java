package sh.mob.timer.web;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.TEXT_EVENT_STREAM;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;


@SpringBootTest
@AutoConfigureWebTestClient
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ApiTests {

  @Autowired private WebTestClient webTestClient;

  @MockBean private Clock clock;

  @Test
  void getIndex() {
    webTestClient.get().uri("/").exchange().expectStatus().isOk();
  }

  @Test
  void postRoom() {
    webTestClient
        .post()
        .uri("/")
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .body(BodyInserters.fromFormData("room", "big-boar-37"))
        .exchange()
        .expectStatus()
        .is3xxRedirection();
  }

  @Test
  void getRoom() {
    webTestClient.get().uri("/big-boar-37").exchange().expectStatus().isOk();
  }

  List<String> userNames = List.of("alice","john","ahma");
  List<String> inactiveNames = List.of("lissu","aki");
  List<String> roleNames = List.of("Typist","Driver");

  @Test
  void putTimer() {
    Mockito.when(clock.instant()).thenReturn(Instant.parse("2020-01-24T06:00:00Z"));

    webTestClient
        .put()
        .uri("/big-boar-37")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(new PutTimerRequest(10L, null, "alice", null,"john", userNames, inactiveNames, roleNames))
        .exchange()
        .expectStatus()
        .isAccepted();

    // check the result from websocket
    var result =
        webTestClient
            .get()
            .uri("/big-boar-37/events")
            .accept(TEXT_EVENT_STREAM)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentTypeCompatibleWith(TEXT_EVENT_STREAM)
            .returnResult(Object.class).getResponseBody();

    Map<String, Object> t = new java.util.HashMap<>();
    t.put("timer", 10);
    t.put("requested", "2020-01-24T06:00:00Z");
    t.put("user", "alice");
    t.put("nextUser", "john");
    t.put("type", "TIMER");
    t.put("userNames", userNames);
    t.put("inactiveNames", inactiveNames );
    t.put("roleNames", roleNames );

    StepVerifier.create(result)
        .expectNext(List.of(), t)
        .thenCancel()
        .verify();
  }

  /**
   * Test using record type for request, maybe switch to it asap.
   * And remove the static class.
   */
  @Test()
  void putTimer2() {
    Mockito.when(clock.instant()).thenReturn(Instant.parse("2020-01-24T06:00:00Z"));

    webTestClient
        .put()
        .uri("/big-boar-37")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(new PutTimerRequest(10L, null, "alice", null,"john",userNames, inactiveNames, roleNames))
        .exchange()
        .expectStatus()
        .isAccepted();

    var result =
        webTestClient
            .get()
            .uri("/big-boar-37/events")
            .accept(TEXT_EVENT_STREAM)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentTypeCompatibleWith(TEXT_EVENT_STREAM)
            .returnResult(Object.class).getResponseBody();

    Map<String, Object> t = new java.util.HashMap<>();
    t.put("timer", 10);
    t.put("requested", "2020-01-24T06:00:00Z");
    t.put("user", "alice");
    t.put("nextUser", "john");
    t.put("type", "TIMER");
    t.put("userNames", userNames);
    t.put("inactiveNames", inactiveNames );
    t.put("roleNames", roleNames );

    StepVerifier.create(result)
        .expectNext(List.of(), t)
        .thenCancel()
        .verify();
  }

  @Test
  void putTimer_with_action() {
    Mockito.when(clock.instant()).thenReturn(Instant.parse("2020-01-24T06:00:00Z"));

    // first initialize the room
    webTestClient
            .put()
            .uri("/big-boar-37")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(new PutTimerRequest(10L, null, "alice", null,"john",userNames, inactiveNames, roleNames))
            .exchange()
            .expectStatus()
            .isAccepted();

    var result =
        webTestClient
            .get()
            .uri("/big-boar-37/events")
            .accept(TEXT_EVENT_STREAM)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentTypeCompatibleWith(TEXT_EVENT_STREAM)
            .returnResult(Object.class).getResponseBody();

    Map<String, Object> t = new java.util.HashMap<>();
    t.put("timer", 10);
    t.put("requested", "2020-01-24T06:00:00Z");
    t.put("user", "alice");
    t.put("nextUser", "john");
    t.put("type", "TIMER");
    t.put("userNames", userNames);
    t.put("inactiveNames", inactiveNames );
    t.put("roleNames", roleNames );
    StepVerifier.create(result)
        .expectNext(List.of(), t)
        .thenCancel()
        .verify();


    // mob next
    // TODO yes i know we're reusing the same api...
    var response = webTestClient
            .put()
            .uri("/big-boar-37")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(new PutTimerRequest(null, null, "alice", "next", null,null,null,null)) // Assuming PutTimerRequest2 class is defined
            .exchange()
            .expectStatus().isAccepted()
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
            .returnResult(PutTimerResponse.class) // Changed from Object.class to String.class
            .getResponseBody()
            .single()
            .block();


    Map<String, Object> putResponseObject = new java.util.HashMap<>();

    putResponseObject.put("nextUser", "john");
    PutTimerResponse putTimerResponse = response;
    Assertions.assertEquals(putResponseObject.get("nextUser"), "john");

  }
}
