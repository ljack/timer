package sh.mob.timer.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class RoomController {

  private final RoomRepository roomRepository;

  public RoomController(RoomRepository roomRepository) {
    this.roomRepository = roomRepository;
  }


  @RequestMapping(
          method = RequestMethod.GET,
          value = "/{roomId:[A-Za-z0-9-_]+}")
  public String get(@PathVariable String roomId, Model model) {
    model.addAttribute("room", roomRepository.get(roomId));
    return "room";
  }
}
