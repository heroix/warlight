package bot;

import static bot.util.ArmyPlacementUtils.findAllOwnedRegions;
import static bot.util.ArmyPlacementUtils.findBorderWithLargestEnemy;
import static bot.util.ArmyPlacementUtils.findOwnBorderRegions;
import static bot.util.ArmyPlacementUtils.findOwnRegionsBorderingEnemy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import map.Region;
import map.SuperRegion;
import move.AttackTransferMove;
import move.PlaceArmiesMove;

/**
 * Warlight AI Game Bot
 *
 * @author Mantas Sinkevicius [mantasink@gmail.com]
 * @version 0.7 [2015-09-20]
 */
public class MrRobot implements Bot {

  @Override
  public Region getStartingRegion(BotState state, Long timeOut) {
    return getStartingRegion(state.getPickableStartingRegions());
  }

  @Override
  public List<Region> getStartingRegions(BotState state, Long timeOut) {
    ArrayList<Region> result = new ArrayList<>(6);
    Set<Region> remainingPickableRegions = new HashSet<>(state.getPickableStartingRegions());

    for (int i = 0; i < 6; i++) {
      Region priorityRegion = getStartingRegion(remainingPickableRegions);
      result.add(priorityRegion);
      remainingPickableRegions.remove(priorityRegion);
    }

    return result;
  }

  private Region getStartingRegion(Collection<Region> pikcableRegions) {
    Region startingRegion = pikcableRegions.stream().findAny().get();
    for (Region pickableRegion : pikcableRegions) {
      int possibleSuperRegionSize = pickableRegion.getSuperRegion().getSubRegions().size();
      int currentSuperRegionSize = startingRegion.getSuperRegion().getSubRegions().size();
      if (possibleSuperRegionSize <= currentSuperRegionSize) {
        startingRegion = pickableRegion;
      }
    }

    return startingRegion;
  }

  private Set<Region> findHijackableRegions(BotState state, Set<Region> allOwnedRegions) {
    Set<Region> result = new HashSet<>();
    map.Map visibleMap = state.getVisibleMap();
    LinkedList<SuperRegion> superRegions = visibleMap.getSuperRegions();

    for (SuperRegion superRegion : superRegions) {
      LinkedList<Region> superSubregions = superRegion.getSubRegions();
      if (visibleMap.getRegions().containsAll(superSubregions)) {
        // if Mr. Robot can see all regions of super region

        Set<Region> notOwnedRegions = new HashSet<>(superSubregions);
        notOwnedRegions.removeAll(allOwnedRegions);
        if (notOwnedRegions.size() < 4 && notOwnedRegions.size() > 0) {
          // check possibility to hijack
          if (notOwnedRegions.stream().filter(r -> r.getArmies() > state.getStartingArmies())
              .collect(Collectors.toSet()).isEmpty()) {
            // if no high power regions in super region then add them to hijackable
            result.addAll(notOwnedRegions);
          }
        }
      }
    }
    return result;
  }

  private Region findFirstHijackableRegionBorder(Set<Region> hijackableRegions,
      Set<Region> allOwnedRegions) {

    for (Region hijackableRegion : hijackableRegions) {
      for (Region hijackableNeghbor : hijackableRegion.getNeighbors()) {
        if (allOwnedRegions.contains(hijackableNeghbor)) {
          return hijackableNeghbor;
        }
      }
    }

    return null;
  }

  @Override
  public ArrayList<PlaceArmiesMove> getPlaceArmiesMoves(BotState state, Long timeOut) {
    ArrayList<PlaceArmiesMove> placeArmiesMoves = new ArrayList<PlaceArmiesMove>();

    Set<Region> allOwnedRegions = findAllOwnedRegions(state);
    Map<Region, Integer> enemyBorderingRegions =
        findOwnRegionsBorderingEnemy(state, allOwnedRegions);
    Set<Region> hijackableRegions = findHijackableRegions(state, allOwnedRegions);
    int armiesLeft = state.getStartingArmies();

    if (!hijackableRegions.isEmpty()) {
      Region placementToHijack =
          findFirstHijackableRegionBorder(hijackableRegions, allOwnedRegions);
      addPlaceArmiesMove("place near hijackable continent", placeArmiesMoves, state,
          placementToHijack, 5);
      armiesLeft = armiesLeft - 5;
    }


    if (!enemyBorderingRegions.isEmpty() && armiesLeft != 0) {
      // place everything close to strongest enemy border
      Region borderWithLargestEnemy = findBorderWithLargestEnemy(enemyBorderingRegions);
      addPlaceArmiesMove("place near strongest enemy", placeArmiesMoves, state,
          borderWithLargestEnemy, armiesLeft);
    } else if (armiesLeft > 0) {
      placeOnRandomBorder(placeArmiesMoves, state, allOwnedRegions, armiesLeft);
    }

    return placeArmiesMoves;
  }


  private void placeOnRandomBorder(Collection<PlaceArmiesMove> placeArmiesMoves, BotState state,
      Collection<Region> allOwnedRegions, int armiesLeft) {
    Set<Region> borderRegions = findOwnBorderRegions(state, allOwnedRegions);

    if (borderRegions.size() >= 2) {
      // pick two random border regions
      int placement1 = armiesLeft / 2;
      int placement2 = armiesLeft - placement1;

      Region region1 = borderRegions.stream().findAny().get();
      borderRegions.remove(region1);
      Region region2 = borderRegions.stream().findAny().get();

      addPlaceArmiesMove("place on random border regions (1)", placeArmiesMoves, state, region1,
          placement1);
      addPlaceArmiesMove("place on random border regions (2)", placeArmiesMoves, state, region2,
          placement2);
    } else {
      // pick one random border region
      Region region = borderRegions.stream().findAny().get();
      addPlaceArmiesMove("place on random border regions (1)", placeArmiesMoves, state, region,
          armiesLeft);
    }
  }

  @Override
  public ArrayList<AttackTransferMove> getAttackTransferMoves(BotState state, Long timeOut) {
    ArrayList<AttackTransferMove> attackTransferMoves = new ArrayList<AttackTransferMove>();

    Set<Region> ownedRegions = findAllOwnedRegions(state);
    Map<Region, Integer> enemyBorderingRegions = findOwnRegionsBorderingEnemy(state, ownedRegions);
    Set<Region> ownOuterRegions = findOwnBorderRegions(state, ownedRegions);
    Set<Region> ownInnerRegions = new HashSet<>(ownedRegions);
    ownInnerRegions.removeAll(ownOuterRegions);

    moveInnerTroops(attackTransferMoves, state, ownOuterRegions, ownInnerRegions);
    moveOuterTroopsAgainstEnemy(attackTransferMoves, state, enemyBorderingRegions);
    moveOuterTroopsAgainstNeutral(attackTransferMoves, state, ownOuterRegions);

    return attackTransferMoves;
  }

  private void moveOuterTroopsAgainstNeutral(ArrayList<AttackTransferMove> attackTransferMoves,
      BotState state, Set<Region> ownOuterRegions) {

    for (Region borderRegion : ownOuterRegions) {
      if (borderRegion.getArmies() > 2) {
        for (Region neighborRegion : borderRegion.getNeighbors()) {
          if ((borderRegion.getArmies() - 1) > neighborRegion.getArmies()
              && !neighborRegion.ownedByPlayer(state.getMyPlayerName())) {
            addAttackTransferMove("attack neighbor", attackTransferMoves, state, borderRegion,
                neighborRegion);
          }
        }
      }
    }

  }

  private void moveOuterTroopsAgainstEnemy(Collection<AttackTransferMove> attackTransferMoves,
      BotState state, Map<Region, Integer> enemyBorderingRegions) {

    Map<Region, Integer> remainingRegions = new HashMap<>(enemyBorderingRegions);
    while (!remainingRegions.isEmpty()) {
      // Bordering an enemy. Make it prior target.
      Region borderWithLargestEnemy = findBorderWithLargestEnemy(remainingRegions);
      handleEnemyBorderingRegion(attackTransferMoves, borderWithLargestEnemy, state);
      remainingRegions.remove(borderWithLargestEnemy);
    }
  }

  private void handleEnemyBorderingRegion(Collection<AttackTransferMove> attackTransferMoves,
      Region borderRegion, BotState state) {
    List<Region> largestEnemyRegions = borderRegion.getNeighbors().stream()
        .sorted((n1, n2) -> Integer.compare(n1.getArmies(), n2.getArmies()))
        .filter(n -> n.getPlayerName().equals(state.getOpponentPlayerName()))
        .collect(Collectors.toList());

    for (Region enemyRegion : largestEnemyRegions) {
      if ((borderRegion.getArmies() - 1) > enemyRegion.getArmies()) {
        // attack the enemy if Mr. Robot is more powerful
        addAttackTransferMove("attack enemy", attackTransferMoves, state, borderRegion,
            enemyRegion);
        break;
      }
    }
  }

  private void moveInnerTroops(ArrayList<AttackTransferMove> attackTransferMoves, BotState state,
      Set<Region> ownOuterRegions, Set<Region> ownInnerRegions) {
    for (Region inner : ownInnerRegions) {
      // move all free inner armies to random region
      if (inner.getArmies() > 1) {
        Region moveTo = inner.getNeighbors().get(random(0, inner.getNeighbors().size() - 1));
        for (Region innerNeighbor : inner.getNeighbors()) {
          // prioritize outer regions
          if (ownOuterRegions.contains(innerNeighbor)) {
            moveTo = innerNeighbor;
            break;
          }
        }
        addAttackTransferMove("inner move", attackTransferMoves, state, inner, moveTo);
      }
    }
  }

  private void addAttackTransferMove(String message,
      Collection<AttackTransferMove> attackTransferMoves, BotState state, Region from, Region to) {
    int amount = from.getArmies() - 1;
    attackTransferMoves.add(new AttackTransferMove(state.getMyPlayerName(), from, to, amount));
    System.err.println(message + ", from: " + from + ", to: " + to + ", amount: " + amount);
  }

  private void addPlaceArmiesMove(String message, Collection<PlaceArmiesMove> placeArmiesMoves,
      BotState state, Region region, int amount) {
    placeArmiesMoves.add(new PlaceArmiesMove(state.getMyPlayerName(), region, amount));
    region.setArmies(region.getArmies() + amount);
    System.err.println(message + ", region: " + region + " (+" + amount + ")");
  }

  private static int random(int min, int max) {
    return ThreadLocalRandom.current().nextInt(min, max + 1);
  }

  public static void main(String[] args) {
    BotParser parser = new BotParser(new MrRobot());
    parser.run();
  }

}
