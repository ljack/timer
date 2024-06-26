package sh.mob.timer.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;


import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public class RoomApiController {

    private static final String SMOKETEST_ROOM_NAME = "testroom-310a9c47-515c-4ad7-a229-ae8efbab7387";
    private static final Logger log = LoggerFactory.getLogger(RoomApiController.class);
    private final RoomRepository roomRepository;
    private final Clock clock;
    private final Stats stats;

    public RoomApiController(RoomRepository roomRepository, Clock clock, Stats stats) {
        this.roomRepository = roomRepository;
        this.clock = clock;
        this.stats = stats;
    }

    /**
     * This is the websocket subcription endpoint endpoint.
     *
     * @param roomId
     * @param response
     * @return
     */

    @RequestMapping(
            method = RequestMethod.GET,
            value = "/{roomId:[A-Za-z0-9-_]+}/events",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Object>> getEventStream(
            @PathVariable String roomId, ServerHttpResponse response) {
        response
                .getHeaders()
                .setCacheControl("no-store, no-cache, must-revalidate, proxy-revalidate, max-age=0");
        response.getHeaders().add("X-Accel-Buffering", "no");
        response.getHeaders().setConnection("keep-alive");
        var room = roomRepository.get(roomId);

        var timerRequestFlux =
                room.sink()
                        .asFlux()
                        .map(
                                timerRequest ->
                                        ServerSentEvent.builder().event("TIMER_REQUEST").data(timerRequest).build());

        var keepAliveFlux =
                Flux.interval(Duration.ofSeconds(5L))
                        .map(
                                second ->
                                        ServerSentEvent.builder()
                                                .event("KEEP_ALIVE")
                                                .data(new TimerRequest(null, null, null, null, null, null, null,null))
                                                .build());

        var initialHistory =
                Flux.just(room.historyWithoutLatest())
                        .map(list -> ServerSentEvent.builder().event("INITIAL_HISTORY").data(list).build());

        return Flux.concat(initialHistory, keepAliveFlux.mergeWith(timerRequestFlux));
    }

    /**
     * This is the information update endpoint called by room.html, enterprise.html and mob cli.
     *
     * It handlers the information update requests and publises changes for the websocket to distribute.
     *
     * @param roomId
     * @param timerRequest
     * @return
     */
    @PutMapping("/{roomId:[A-Za-z0-9-_]+}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public PutTimerResponse publishEvent(@PathVariable String roomId, @RequestBody PutTimerRequest timerRequest) {

        Room room = roomRepository.get(roomId);

        log.info(" timer request: {}, room: {}", timerRequest, room);


        if (timerRequest.timer() != null) {
            // basic update where the mob timer is wanted to be started
            long timer = truncateTooLongTimers(timerRequest.timer());
            Optional<TimerRequest> tr = room.lastTimerRequest();
            if (tr.isEmpty()) {
                if( timerRequest.user() != null && timerRequest.roleNames() == null) {
                    // cli: mob start

                } else {
                    room.add(timer, timerRequest.user(), timerRequest.nextUser(), Instant.now(clock), timerRequest.userNames(), timerRequest.inactiveNames(),timerRequest.roleNames());
                }

                tr = room.lastTimerRequest();
            } else {
                if( timerRequest.user() != null && timerRequest.roleNames() == null) {
                    var putCurrentUserToFirst = tr.get().userNames();
                    putCurrentUserToFirst = putCurrentUserToFirst.stream()
                            .filter(user -> !user.equalsIgnoreCase(timerRequest.user()))
                            .collect(Collectors.toCollection(LinkedList::new));
                    putCurrentUserToFirst.addFirst(timerRequest.user());

                    String nextUser = putCurrentUserToFirst.size() > 1 ? putCurrentUserToFirst.get(1) : "";

                    room.add(timer, timerRequest.user(), nextUser, Instant.now(clock), putCurrentUserToFirst, tr.get().inactiveNames(), tr.get().roleNames());
                } else{
                    room.add(timer, timerRequest.user(), tr.get().nextUser(), Instant.now(clock), tr.get().userNames(), tr.get().inactiveNames(), tr.get().roleNames());
                }
                tr = room.lastTimerRequest();
            }

            log.info("Start timer {} by user {} for room {}", timerRequest, timerRequest.user(), room);
            incrementTimerStatsExceptForTestRoom(room, timer);
            return new PutTimerResponse(tr.get().user(), tr.get().nextUser(), tr.get().userNames(), tr.get().inactiveNames(), tr.get().roleNames());

        } else if (timerRequest.breaktimer() != null) {
            // basic update where the break timer is wanted to be started
            long breaktimer = truncateTooLongTimers(timerRequest.breaktimer());
            room.addBreaktimer(breaktimer, timerRequest);
            log.info(
                    "Add break timer {} by user {} for room {}",
                    timerRequest.breaktimer(),
                    timerRequest.user(),
                    room.name());
            incrementBreakTimerStatsExceptForTestRoom(room, breaktimer);

        } else if ("next".equalsIgnoreCase(timerRequest.action())) {
            // called either from "mob next --ping" or enterprise.html when mobNext is clicked.
            // don't touch the timer, just update user information
            Optional<TimerRequest> last = room.lastTimerRequest();

            String oldFirst = removeElementSafely( last.get().userNames(), 0,"unknown");
            last.get().userNames().addLast(oldFirst);
            String newFirst = getElementSafely( last.get().userNames(),0, "unknown");
            String nextNext = getElementSafely( last.get().userNames(),1, "unknown");
            var newTimerRequest = room.add( last.get().timer(), newFirst, nextNext, last.get().requested(),  last.get().userNames(), last.get().inactiveNames(), last.get().roleNames() );

            return PutTimerResponse.fromTimerRequest(newTimerRequest);

        } else if ("update".equalsIgnoreCase(timerRequest.action())) {
            // called from room.html when
            Optional<TimerRequest> last = room.lastTimerRequest();
            if (last.isEmpty()) {
                var names = timerRequest.userNames();
                String first = names.size() > 0 ? names.get(0) : "unknown";
                String second = names.size() > 1 ? names.get(1) : "unknown";
                room.add(10L, first, second, Instant.now(clock), timerRequest.userNames(), timerRequest.inactiveNames(), timerRequest.roleNames());
                last = room.lastTimerRequest();
            } else {
                room.add(last.get().timer(), last.get().user(), last.get().nextUser(), last.get().requested(), timerRequest.userNames(), timerRequest.inactiveNames(), timerRequest.roleNames());
            }
            return new PutTimerResponse(last.get().user(), last.get().nextUser(), last.get().userNames(), last.get().inactiveNames(), timerRequest.roleNames());

        } else {
            log.warn("Could not understand PUT request for room {}", roomId);
        }

        //
        return new PutTimerResponse(null, null, null, null,null);
    }

    private void incrementBreakTimerStatsExceptForTestRoom(Room room, long breaktimer) {
        if (!Objects.equals(room.name(), SMOKETEST_ROOM_NAME)) {
            stats.incrementBreaktimer(breaktimer);
        }
    }

    private void incrementTimerStatsExceptForTestRoom(Room room, long timer) {
        if (!Objects.equals(room.name(), SMOKETEST_ROOM_NAME)) {
            stats.incrementTimer(timer);
        }
    }

    private static long truncateTooLongTimers(Long timer) {
        return Math.min(60 * 24, Math.max(0, timer));
    }

    public static <T> T getElementSafely(List<T> list, int index, T defaultValue) {
        if (index >= 0 && index < list.size()) {
            return list.get(index);
        } else {
            return defaultValue;
        }
    }

    public static <T> T removeElementSafely(List<T> list, int index, T defaultValue) {
        if (index >= 0 && index < list.size()) {
            return list.remove(index);
        } else {
            return defaultValue;
        }
    }

}
