package sh.mob.timer.web;

import java.time.Instant;
import java.util.List;

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
        BREAKTIMER;

        public static TimerType toEnum(PutTimerRequest req) {
            if (req.timer() != null) {
                return TIMER;
            } else if (req.breaktimer() != null) {
                return BREAKTIMER;
            } else {
                throw new IllegalArgumentException("Both timer and breakTimer cannot be null");
            }
        }
    }

}
