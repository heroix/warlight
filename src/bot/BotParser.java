/**
 * Warlight AI Game Bot
 *
 * Last update: January 29, 2015
 *
 * @author Jim van Eeden
 * @version 1.1
 * @License MIT License (http://opensource.org/Licenses/MIT)
 */

package bot;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import map.Region;
import move.AttackTransferMove;
import move.PlaceArmiesMove;

public class BotParser {

  final Scanner scan;

  final Bot bot;

  BotState currentState;

  public BotParser(Bot bot) {
    this.scan = new Scanner(System.in);
    this.bot = bot;
    this.currentState = new BotState();
  }

  public void run() {
    while (scan.hasNextLine()) {
      String line = scan.nextLine().trim();
      if (line.length() == 0) {
        continue;
      }
      String[] parts = line.split(" ");
      if (parts[0].equals("pick_starting_region")) {
        // pick which regions you want to start with
        long startTime = System.currentTimeMillis();

        currentState.setPickableStartingRegions(parts);
        Region startingRegion = bot.getStartingRegion(currentState, Long.valueOf(parts[1]));
        System.out.println(startingRegion.getId());

        System.err
            .println("pick_starting_region delay: ~" + (System.currentTimeMillis() - startTime));
      } else if (parts[0].equals("pick_starting_regions")) {
        long startTime = System.currentTimeMillis();

        currentState.setPickableStartingRegions(parts);

        List<Region> startingRegions = bot.getStartingRegions(currentState, Long.valueOf(parts[1]));
        List<String> startingRegionIds = startingRegions.stream()
            .map(s -> Integer.toString(s.getId())).collect(Collectors.toList());

        System.out.println(String.join(" ", startingRegionIds));

        System.err
            .println("pick_starting_regions delay: ~" + (System.currentTimeMillis() - startTime));
      } else if (parts.length == 3 && parts[0].equals("go")) {
        long startTime = System.currentTimeMillis();
        // we need to do a move
        String output = "";
        if (parts[1].equals("place_armies")) {
          // place armies
          ArrayList<PlaceArmiesMove> placeArmiesMoves =
              bot.getPlaceArmiesMoves(currentState, Long.valueOf(parts[2]));
          for (PlaceArmiesMove move : placeArmiesMoves)
            output = output.concat(move.getString() + ",");
        } else if (parts[1].equals("attack/transfer")) {
          // attack/transfer
          ArrayList<AttackTransferMove> attackTransferMoves =
              bot.getAttackTransferMoves(currentState, Long.valueOf(parts[2]));
          for (AttackTransferMove move : attackTransferMoves)
            output = output.concat(move.getString() + ",");
        }
        if (output.length() > 0)
          System.out.println(output);
        else
          System.out.println("No moves");

        System.err.println("go delay: ~" + (System.currentTimeMillis() - startTime));
        System.err.println();
      } else if (parts[0].equals("settings")) {
        // update settings
        currentState.updateSettings(parts[1], parts);
      } else if (parts[0].equals("setup_map")) {
        // initial full map is given
        currentState.setupMap(parts);
      } else if (parts[0].equals("update_map")) {
        // all visible regions are given
        currentState.updateMap(parts);
      } else if (parts[0].equals("opponent_moves")) {
        // all visible opponent moves are given
        currentState.readOpponentMoves(parts);
      } else {
        System.err.printf("Unable to parse line \"%s\"\n", line);
      }
    }
  }

}
