package sh.mob.timer.web;

import static java.time.temporal.ChronoUnit.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Sinks;
import sh.mob.timer.web.Room.TimerRequest.TimerType;

final class Room {

  private static final Logger log = LoggerFactory.getLogger(Room.class);

  public static final TimerRequest NULL_TIMER_REQUEST =
      new TimerRequest(0L, null, null, null, null,null, null,null);

  private final String name;
  private final List<TimerRequest> timerRequests = new CopyOnWriteArrayList<>();
  private final Sinks.Many<TimerRequest> sink =
      Sinks.many().replay().latestOrDefault(NULL_TIMER_REQUEST);

  Room(String name) {
    this.name = name;
  }

  public void add(Long timer, String user, String nextUserRequested, Instant requested,
                  List<String> userNames,
                  List<String> inactiveNames,
                  List<String> roleNames ) {

    String nextUser = nextUserRequested;

    var timerRequest = new TimerRequest(timer, requested, user, nextUser, TimerType.TIMER, userNames, inactiveNames, roleNames);
    timerRequests.add(timerRequest);
    sink.tryEmitNext(timerRequest);
  }


    public TimerRequest getLastTimerRequest() {
      if (timerRequests.isEmpty()) {
        return null;
      }

      var last = timerRequests.getLast();
      return last;
    }

    public TimerRequest findNextUser(String user) {
    if (timerRequests.isEmpty()) {
      return null;
    }

    var last = timerRequests.getLast();
    if( last == null )
      return null;

    var nextUser = last.nextUser();
    if( nextUser != null)
      return last;

    return null;
  }

  public void addBreaktimer(Long breaktimer, PutTimerRequest putTimerRequest) {
    TimerRequest timerRequest =
        new TimerRequest(
            breaktimer,
            Instant.now(), putTimerRequest.user(),
            lastTimerRequest().map(TimerRequest::nextUser).orElse(null),
            TimerType.BREAKTIMER,
            putTimerRequest.userNames(),
            putTimerRequest.inactiveNames(),
            putTimerRequest.roleNames());
    timerRequests.add(timerRequest);
    sink.tryEmitNext(timerRequest);
  }

  public Sinks.Many<TimerRequest> sink() {
    return sink;
  }

  Optional<TimerRequest> lastTimerRequest() {
    if (timerRequests.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(timerRequests.get(timerRequests.size() - 1));
  }

  public void removeOldTimerRequests() {
    var now = Instant.now();
    this.timerRequests.removeIf(
        timerRequest -> now.minus(24, HOURS).isAfter(timerRequest.requested()));
    if (timerRequests.isEmpty()) {
      sink.tryEmitNext(NULL_TIMER_REQUEST);
      log.info("Emptied room {}", name);
    }
  }

  public String name() {
    return name;
  }

  public List<TimerRequest> historyWithoutLatest() {
    if (timerRequests.isEmpty()) {
      return List.of();
    }

    return timerRequests.subList(0, timerRequests.size() - 1);
  }

  public boolean isTimerActive(Instant now) {
    return lastTimerRequest().filter(timerRequest -> isTimerActive(timerRequest, now)).isPresent();
  }

  private static boolean isTimerActive(TimerRequest timerRequest, Instant now) {
    return timerRequest.timer() != null
        && timerRequest.timer() > 0
        && timerRequest.requested() != null
        && timerRequest.requested().plus(timerRequest.timer(), MINUTES).isAfter(now);
  }

  public record TimerRequest(Long timer,
                             Instant requested,
                             String user,
                             String nextUser,
                             TimerType type,
                             List<String> userNames,
                             List<String> inactiveNames,
                             List<String> roleNames
  ) {
    enum TimerType {
      TIMER,
      BREAKTIMER
    }
  }


}
