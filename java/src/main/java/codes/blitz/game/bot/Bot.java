package codes.blitz.game.bot;

import java.util.*;

import codes.blitz.game.message.game.*;
import codes.blitz.game.message.game.actions.*;

public class Bot {
    public Bot() {
        System.out.println("Initializing your super duper mega bot.");
    }

    private boolean helmOperated = false;
    private Integer moveableWeaponsCount = 0;
    private Integer immobileWeaponsCount = 0;
    /*
     * Here is where the magic happens. I bet you can do better ;)
     */
    public List<Action> getActions(GameMessage gameMessage) {
        List<Action> actions = new ArrayList<>();

        Ship myShip = gameMessage.ships().get(gameMessage.currentTeamId());
        List<String> otherShipsIds = new ArrayList<>(gameMessage.shipsPositions().keySet());
        Map<String, TurretStation> shipWeapons = new HashMap<>();
        Map<String, TurretStation> moveableWeapons = new HashMap<>();
        Map<String, TurretStation> immobileWeapons = new HashMap<>();
        Map<String, Double> immobileCannonId = new HashMap<>();

        otherShipsIds.removeIf(shipId -> shipId.equals(gameMessage.currentTeamId()));

        for (TurretStation turret : myShip.stations().turrets()){
            shipWeapons.put(turret.id(), turret);
            System.out.println("turret type: " + turret.turretType() + "turret orientation: " + turret.orientationDegrees() + "\n");
            if (turret.turretType() == TurretType.NORMAL || turret.turretType() == TurretType.EMP){
                moveableWeapons.put(turret.id(), turret);
                moveableWeaponsCount++;
            } else {
                immobileWeapons.put(turret.id(), turret);
                immobileWeaponsCount++;
                if (turret.turretType() == TurretType.CANNON){
                    immobileCannonId.put(turret.id(), turret.orientationDegrees());
                }
            }
        }

        if (otherShipsIds.size() <= 1) {
            // Find who's not doing anything and try to give them a job.
            List<Crewmate> idleCrewmates = new ArrayList<>(myShip.crew());
            idleCrewmates.removeIf(crewmate -> crewmate.currentStation() != null || crewmate.destination() != null);
            Map<String, String> crewmateLocations = new HashMap<>();
            boolean moveToHelm = false;
            boolean enemyLeft = false;
            double gunAngle = 0;

            enemyLeft = myShip.worldPosition().x() > gameMessage.shipsPositions().get(otherShipsIds.get(0)).toVector().x();

            for (Crewmate crewmate : idleCrewmates) {

                List<StationDistance> visitableStations = new ArrayList<>();
                visitableStations.addAll(crewmate.distanceFromStations().turrets());
                visitableStations.addAll(crewmate.distanceFromStations().radars());
                visitableStations.addAll(crewmate.distanceFromStations().helms());
                visitableStations.addAll(crewmate.distanceFromStations().shields());



                for (StationDistance station : visitableStations) {
                    if (moveToHelm) {
                        actions.add(new MoveCrewAction(crewmate.id(), station.stationPosition()));
                        moveToHelm = false;
                        break;
                    }
                    if (
                            !crewmateLocations.containsKey(station.stationId()) &&
                                    (moveableWeapons.containsKey(station.stationId()) ||(
                                    immobileWeapons.get(station.stationId()).orientationDegrees() <= 190 &&
                                    immobileWeapons.get(station.stationId()).orientationDegrees() >= 170) &&
                                    enemyLeft)
                    )
                    {
                        actions.add(new MoveCrewAction(crewmate.id(), station.stationPosition()));
                        crewmateLocations.put(station.stationId(), crewmate.id());
                        break;
                    } else if (
                            !crewmateLocations.containsKey(station.stationId()) &&
                                    (moveableWeapons.containsKey(station.stationId()) || (
                                    immobileWeapons.get(station.stationId()).orientationDegrees() <= 10 &&
                                    immobileWeapons.get(station.stationId()).orientationDegrees() >= -10) &&
                                    !enemyLeft)
                    )
                    {
                        actions.add(new MoveCrewAction(crewmate.id(), station.stationPosition()));
                        crewmateLocations.put(station.stationId(), crewmate.id());
                        break;
                    } else if (
                            !crewmateLocations.containsKey(station.stationId()) &&
                            moveableWeaponsCount <= 1 &&
                            immobileWeapons.containsKey(station.stationId()) &&
                            immobileWeapons.get(station.stationId()).turretType() == TurretType.CANNON)
                    {
                        actions.add(new MoveCrewAction(crewmate.id(), station.stationPosition()));
                        crewmateLocations.put(station.stationId(), crewmate.id());
                        moveToHelm = true;
                        gunAngle = immobileWeapons.get(station.stationId()).orientationDegrees();
                        break;
                    }
                }
            }

            // Now crew members at stations should do something!
            List<TurretStation> operatedTurretStations = new ArrayList<>(myShip.stations().turrets());
            operatedTurretStations.removeIf(turretStation -> turretStation.operator() == null);
            for (TurretStation turretStation : operatedTurretStations) {
                actions.add(new TurretLookAtAction(turretStation.id(), gameMessage.shipsPositions().get(otherShipsIds.get(0)).toVector()));
                actions.add(new TurretShootAction(turretStation.id()));
            }

            List<HelmStation> operatedHelmStation = new ArrayList<>(myShip.stations().helms());
            operatedHelmStation.removeIf(helmStation -> helmStation.operator() == null);
            for (HelmStation helmStation : operatedHelmStation) {
                actions.add(new RotateShipAction(gunAngle));
            }

            List<RadarStation> operatedRadarStations = new ArrayList<>(myShip.stations().radars());
            operatedRadarStations.removeIf(radarStation -> radarStation.operator() == null);
            for (RadarStation radarStation : operatedRadarStations) {
                actions.add(new RadarScanAction(radarStation.id(), otherShipsIds.get(0)));
            }
            List<Crewmate> Crewmates = new ArrayList<>(myShip.crew());
            repareShield(gameMessage, Crewmates, actions, myShip, crewmateLocations);
            // You can clearly do better than the random actions above. Have fun!!
            return actions;
        } else {
            // Find who's not doing anything and try to give them a job.
            List<Crewmate> idleCrewmates = new ArrayList<>(myShip.crew());
            idleCrewmates.removeIf(crewmate -> crewmate.currentStation() != null || crewmate.destination() != null);
            Map<String, String> crewmateLocations = new HashMap<>();
            boolean moveToHelm = false;
            boolean enemyLeft = false;
            double gunAngle = 0;

            enemyLeft = myShip.worldPosition().x() > gameMessage.shipsPositions().get(otherShipsIds.get(0)).toVector().x();

            for (Crewmate crewmate : idleCrewmates) {

                List<StationDistance> visitableStations = new ArrayList<>();
                visitableStations.addAll(crewmate.distanceFromStations().turrets());
                visitableStations.addAll(crewmate.distanceFromStations().radars());
                visitableStations.addAll(crewmate.distanceFromStations().helms());
                visitableStations.addAll(crewmate.distanceFromStations().shields());



                for (StationDistance station : visitableStations) {
                    if (moveToHelm) {
                        actions.add(new MoveCrewAction(crewmate.id(), station.stationPosition()));
                        moveToHelm = false;
                        break;
                    }
                    if (
                            !crewmateLocations.containsKey(station.stationId()) &&
                                    (moveableWeapons.containsKey(station.stationId()) ||(
                                            immobileWeapons.get(station.stationId()).orientationDegrees() <= 190 &&
                                                    immobileWeapons.get(station.stationId()).orientationDegrees() >= 170) &&
                                            enemyLeft)
                    )
                    {
                        actions.add(new MoveCrewAction(crewmate.id(), station.stationPosition()));
                        crewmateLocations.put(station.stationId(), crewmate.id());
                        break;
                    } else if (
                            !crewmateLocations.containsKey(station.stationId()) &&
                                    (moveableWeapons.containsKey(station.stationId()) || (
                                            immobileWeapons.get(station.stationId()).orientationDegrees() <= 10 &&
                                                    immobileWeapons.get(station.stationId()).orientationDegrees() >= -10) &&
                                            !enemyLeft)
                    )
                    {
                        actions.add(new MoveCrewAction(crewmate.id(), station.stationPosition()));
                        crewmateLocations.put(station.stationId(), crewmate.id());
                        break;
                    } else if (
                            !crewmateLocations.containsKey(station.stationId()) &&
                                    moveableWeaponsCount <= 1 &&
                                    immobileWeapons.containsKey(station.stationId()) &&
                                    immobileWeapons.get(station.stationId()).turretType() == TurretType.CANNON)
                    {
                        actions.add(new MoveCrewAction(crewmate.id(), station.stationPosition()));
                        crewmateLocations.put(station.stationId(), crewmate.id());
                        moveToHelm = true;
                        gunAngle = immobileWeapons.get(station.stationId()).orientationDegrees();
                        break;
                    }
                }
            }

            // Now crew members at stations should do something!
            List<TurretStation> operatedTurretStations = new ArrayList<>(myShip.stations().turrets());
            operatedTurretStations.removeIf(turretStation -> turretStation.operator() == null);
            for (TurretStation turretStation : operatedTurretStations) {
                actions.add(new TurretLookAtAction(turretStation.id(), gameMessage.shipsPositions().get(otherShipsIds.get(0)).toVector()));
                actions.add(new TurretShootAction(turretStation.id()));
            }

            List<HelmStation> operatedHelmStation = new ArrayList<>(myShip.stations().helms());
            operatedHelmStation.removeIf(helmStation -> helmStation.operator() == null);
            for (HelmStation helmStation : operatedHelmStation) {
                actions.add(new RotateShipAction(gunAngle));
            }

            List<RadarStation> operatedRadarStations = new ArrayList<>(myShip.stations().radars());
            operatedRadarStations.removeIf(radarStation -> radarStation.operator() == null);
            for (RadarStation radarStation : operatedRadarStations) {
                actions.add(new RadarScanAction(radarStation.id(), otherShipsIds.get(0)));
            }
            List<Crewmate> Crewmates = new ArrayList<>(myShip.crew());
            repareShield(gameMessage, Crewmates, actions, myShip, crewmateLocations);
            // You can clearly do better than the random actions above. Have fun!!
            return actions;
        }
    }
    
    public void repareShield(GameMessage gameMessage, List<Crewmate> crewmateList, List<Action> actions, Ship myShip, Map<String, String> crewmateLocations) {
        int shortestDistance = 1000000;
        StationDistance stationToMoveTo = null;
        Crewmate crewToMove = null;
        System.out.println("shield health is " + myShip.currentShield());
        if (myShip.currentShield() <= 10) {
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
            System.out.println("Current station is " + crewToMove.currentStation());
        }

        if (myShip.currentShield() >= 150) {
            // ICI KEVIN
            for (Crewmate crewmate : crewmateList) {
                if (Objects.equals(crewmate.currentStation(), "10758")) {
                    List<StationDistance> visitableStations = new ArrayList<>();
                    visitableStations.addAll(crewmate.distanceFromStations().turrets());


                    for (StationDistance station : visitableStations) {
                        if (!crewmateLocations.containsKey(station.stationId())) {
                            actions.add(new MoveCrewAction(crewmate.id(), station.stationPosition()));
                            crewmateLocations.put(station.stationId(), crewmate.id());
                            break;
                        }
                    }
                }
            }
        }
    }

}