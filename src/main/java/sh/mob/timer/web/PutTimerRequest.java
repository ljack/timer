package sh.mob.timer.web;

import java.util.List;

public record PutTimerRequest(Long timer,
                              Long breaktimer,
                              String user,
                              String action,
                              String nextUser,
                              List<String> userNames,
                              List<String> inactiveNames,
                              List<String> roleNames ) {
    /**
     *  PutTimerRequest is general request from room.html to update all information about room.
     *  TODO consider new name for the request.
     */

  }