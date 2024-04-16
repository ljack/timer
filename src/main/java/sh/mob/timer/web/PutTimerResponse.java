package sh.mob.timer.web;

import java.util.List;

/**
 * Generic response with updated information
 *
 * @param currentUser
 * @param nextUser
 * @param userNames
 * @param inactiveNames
 * @param roleNames
 */
public record PutTimerResponse(String currentUser,
                               String nextUser,
                               List<String> userNames,
                               List<String> inactiveNames,
                               List<String> roleNames ) {


    public static PutTimerResponse fromTimerRequest(TimerRequest tr) {
        // Assuming TimerRequest has methods to provide these values
        return new PutTimerResponse(
                tr.user(),
                tr.nextUser(),
                tr.userNames(),
                tr.inactiveNames(),
                tr.roleNames()
        );
    }

}
