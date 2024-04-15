package sh.mob.timer.web;

import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@RestController
public class IndexApiController {

  private final RoomRepository roomRepository;
  private final Sinks.Many<Long> numberOfActiveUsersSink =
      Sinks.many().replay().latestOrDefault(0L);
  private final Sinks.Many<Long> numberOfActiveTimersSink =
      Sinks.many().replay().latestOrDefault(0L);

  public IndexApiController(RoomRepository roomRepository) {
    this.roomRepository = roomRepository;
  }

  @RequestMapping(
      method = RequestMethod.GET,
      value = {"/events"},
      produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<ServerSentEvent<Long>> events(ServerHttpResponse response) {
    response
        .getHeaders()
        .setCacheControl("no-store, no-cache, must-revalidate, proxy-revalidate, max-age=0");
    response.getHeaders().add("X-Accel-Buffering", "no");
    response.getHeaders().setConnection("keep-alive");

    return numberOfActiveUsersSink
        .asFlux()
        .map(numberOfActiveUsers -> createEvent(numberOfActiveUsers, "ACTIVE_USERS_UPDATE"))
        .mergeWith(
            numberOfActiveTimersSink
                .asFlux()
                .map(
                    numberOfActiveUsers ->
                        createEvent(numberOfActiveUsers, "ACTIVE_TIMERS_UPDATE")));
  }

  private static ServerSentEvent<Long> createEvent(Long numberOfActiveUsers, String event) {
    return ServerSentEvent.<Long>builder().event(event).data(numberOfActiveUsers).build();
  }

  @Scheduled(fixedRateString = "PT1S")
  public void update() {
    numberOfActiveUsersSink.tryEmitNext(roomRepository.countConnections());
    numberOfActiveTimersSink.tryEmitNext(roomRepository.countActiveTimers());
  }
}
