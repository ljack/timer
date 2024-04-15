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

}
