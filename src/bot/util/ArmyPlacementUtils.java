package bot.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import bot.BotState;
import map.Region;

public final class ArmyPlacementUtils {

  public static Set<Region> findAllOwnedRegions(BotState state) {
    Set<Region> ownedRegions = new HashSet<>();
    for (Region region : state.getVisibleMap().getRegions()) {
      if (region.getPlayerName().equals(state.getMyPlayerName())) {
        ownedRegions.add(region);
      }
    }
    return ownedRegions;
  }

  public static Map<Region, Integer> findOwnRegionsBorderingEnemy(BotState state,
      Collection<Region> ownedRegions) {
    ArrayList<Region> enemyBorderingRegions = new ArrayList<>();
    Map<Region, Integer> enemyBorderPower = new HashMap<>();

    for (Region owned : ownedRegions) {
      // get enemy border
      for (Region neighbor : owned.getNeighbors()) {
        if (neighbor.getPlayerName().equals(state.getOpponentPlayerName())) {
          enemyBorderingRegions.add(owned);
        }
      }
    }

    for (Region enemyBorderingRegion : enemyBorderingRegions) {
      // calculate total power of enemy border
      int power = 0;
      for (Region neighbor : enemyBorderingRegion.getNeighbors()) {
        if (neighbor.ownedByPlayer(state.getOpponentPlayerName())) {
          power += neighbor.getArmies();
        }
      }
      enemyBorderPower.put(enemyBorderingRegion, power);
    }

    return enemyBorderPower;
  }

  public static Region findBorderWithLargestEnemy(Map<Region, Integer> enemyBorder) {
    // find the region with the largest neighboring enemy troop count
    Entry<Region, Integer> largest = enemyBorder.entrySet().stream().findFirst().get();
    for (Entry<Region, Integer> entry : enemyBorder.entrySet()) {
      if (entry.getValue() > largest.getValue()) {
        largest = entry;
      }
    }
    return largest.getKey();
  }

  public static Set<Region> findOwnBorderRegions(BotState state, Collection<Region> ownedRegions) {
    Set<Region> borderRegions = new HashSet<>();
    for (Region own : ownedRegions) {
      for (Region neighbor : own.getNeighbors()) {
        if (!neighbor.getPlayerName().equals(state.getMyPlayerName())) {
          borderRegions.add(own);
        }
      }
    }
    return borderRegions;
  }

}
