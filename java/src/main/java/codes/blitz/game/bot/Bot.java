package codes.blitz.game.bot;

import java.util.*;

import codes.blitz.game.message.game.*;
import codes.blitz.game.message.game.actions.*;

public class Bot {

    public Bot() {
        System.out.println("Initializing your super duper mega bot.");
    }

    /*
     * Here is where the magic happens. I bet you can do better ;)
     */
    public List<Action> getActions(GameMessage gameMessage) {
        List<Action> actions = new ArrayList<>();
        Ship myShip = gameMessage.ships().get(gameMessage.currentTeamId());
        List<String> otherShipsIds = new ArrayList<>(gameMessage.shipsPositions().keySet());
        otherShipsIds.removeIf(shipId -> shipId.equals(gameMessage.currentTeamId()));
        // Find who's not doing anything and try to give them a job.
        List<Crewmate> idleCrewmates = new ArrayList<>(myShip.crew());
        idleCrewmates.removeIf(crewmate -> crewmate.currentStation() != null || crewmate.destination() != null);
        Map<String, String> crewmateLocations = new HashMap<>();



        for (Crewmate crewmate : idleCrewmates) {
            List<StationDistance> visitableStations = new ArrayList<>();
            visitableStations.addAll(crewmate.distanceFromStations().turrets());
            visitableStations.addAll(crewmate.distanceFromStations().radars());

            //visitableStations.sort(Comparator.comparingInt(visitableStations.distance()));


            for (StationDistance station : visitableStations) {
                if (!crewmateLocations.containsKey(station.stationId())) {
                    actions.add(new MoveCrewAction(crewmate.id(), station.stationPosition()));
                    crewmateLocations.put(station.stationId(), crewmate.id());
                    break;
                }
            }
            //StationDistance stationToMoveTo = visitableStations.get(new Random().nextInt(visitableStations.size()));

        }

        // Now crew members at stations should do something!
        List<TurretStation> operatedTurretStations = new ArrayList<>(myShip.stations().turrets());
        operatedTurretStations.removeIf(turretStation -> turretStation.operator() == null);
        for (TurretStation turretStation : operatedTurretStations) {
//            int switchAction = new Random().nextInt(3);
//            switch (switchAction) {
//                case 0:
//                    // Charge the turret
//                    actions.add(new TurretChargeAction(turretStation.id()));
//                    break;
//                case 1:
//                    // Aim at the turret itself
//                    actions.add(new TurretLookAtAction(turretStation.id(), new Vector(gameMessage.constants().world().width() * Math.random(), gameMessage.constants().world().width() * Math.random())));
//                    break;
//                case 2:
//                    // Shoot!
//                    actions.add(new TurretShootAction(turretStation.id()));
//                    break;
//            }
            actions.add(new TurretLookAtAction(turretStation.id(), gameMessage.shipsPositions().get(otherShipsIds.get(0)).toVector()));
            actions.add(new TurretShootAction(turretStation.id()));
        }

        List<HelmStation> operatedHelmStation = new ArrayList<>(myShip.stations().helms());
        operatedHelmStation.removeIf(helmStation -> helmStation.operator() == null);
        for (HelmStation helmStation : operatedHelmStation) {
            actions.add(new RotateShipAction(360 * Math.random()));
        }

        List<RadarStation> operatedRadarStations = new ArrayList<>(myShip.stations().radars());
        operatedRadarStations.removeIf(radarStation -> radarStation.operator() == null);
//        for (RadarStation radarStation : operatedRadarStations) {
//            actions.add(new RadarScanAction(radarStation.id(), otherShipsIds.get(0)));
//        }

        List<Crewmate> Crewmates = new ArrayList<>(myShip.crew());
        repareShield(gameMessage, Crewmates, actions, myShip);
        // You can clearly do better than the random actions above. Have fun!!
        return actions;
    }
    public String radarScan(GameMessage gameMessage,List<String> otherShipsIds,List<Crewmate> crewmateList, List<Action> actions, Ship myShip,Map<String, String> crewmateLocations) {
        boolean checkedPos1 = false;
        boolean checkedPos2 = false;
        boolean checkedPos3 = false;
        int shortestDistance = 1000000;
        StationDistance stationToMoveTo = null;
        Crewmate crewToMove = null;
        String targetId = null;
        if (gameMessage.currentTickNumber() % 300 == 0) {
            for (Crewmate crewmate : crewmateList) {
                for (StationDistance stationDistance : crewmate.distanceFromStations().radars()) {
                    if (stationDistance.distance() < shortestDistance) {
                        stationToMoveTo = stationDistance;
                        crewToMove = crewmate;
                        System.out.println("Moving crewmate to radar station" + crewToMove.id() + " : " + stationToMoveTo.stationPosition() + "because need information");
                    }
                }
            }
            actions.add(new MoveCrewAction(crewToMove.id(), stationToMoveTo.stationPosition()));
        }
        if (crewmateLocations.containsKey(stationToMoveTo.stationId())) {
            double health = 1000;

            if (crewmateLocations.containsKey(stationToMoveTo.stationId()) && !checkedPos1) {
                actions.add(new RadarScanAction(stationToMoveTo.stationId(), otherShipsIds.get(0)));
                double health_1 = gameMessage.ships().get(1).currentHealth();
                if (health_1<health){
                    health = health_1;
                    targetId = otherShipsIds.get(0);
                }
                checkedPos1=true;
            } else if (crewmateLocations.containsKey(stationToMoveTo.stationId()) && !checkedPos2) {
                actions.add(new RadarScanAction(stationToMoveTo.stationId(), otherShipsIds.get(1)));
                double health_2 = gameMessage.ships().get(2).currentHealth();
                if (health_2<health){
                    health = health_2;
                    targetId = otherShipsIds.get(1);
                }
                checkedPos2=true;
            } else if (crewmateLocations.containsKey(stationToMoveTo.stationId()) && !checkedPos3) {
                actions.add(new RadarScanAction(stationToMoveTo.stationId(), otherShipsIds.get(2)));
                double health_3 = gameMessage.ships().get(3).currentHealth();
                if (health_3<health){
                    health = health_3;
                    targetId=otherShipsIds.get(2);
                }
                checkedPos3=true;
            }

        }
        return targetId;
    }
    public void repareShield(GameMessage gameMessage, List<Crewmate> crewmateList, List<Action> actions, Ship myShip) {
        int shortestDistance = 1000000;
        StationDistance stationToMoveTo = null;
        Crewmate crewToMove = null;
        System.out.println("shield health is " + myShip.currentShield());
        if (myShip.currentShield() <= 0) {
            for (Crewmate crewmate : crewmateList) {
                for (StationDistance stationDistance : crewmate.distanceFromStations().shields()) {
                    if (stationDistance.distance() < shortestDistance) {
                        stationToMoveTo = stationDistance;
                        crewToMove = crewmate;
                        System.out.println("Moving crewmate to shield to shield station" + crewToMove.id() + " : " + stationToMoveTo.stationPosition() + "because health is less than 0");
                    }
                }
            }
            actions.add(new MoveCrewAction(crewToMove.id(), stationToMoveTo.stationPosition()));
        }

        if (myShip.currentShield() >= 150) {
            // ICI KEVIN
        }
    }
    
}