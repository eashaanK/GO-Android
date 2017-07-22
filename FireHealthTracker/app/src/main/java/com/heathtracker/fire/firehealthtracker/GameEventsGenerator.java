package com.heathtracker.fire.firehealthtracker;

/**
 * Created by eashaan on 3/30/17.
 */

public class GameEventsGenerator {

    private double lastGeneratedTargetDistance = 0;
    private double delta = 20;

    public double generateNextTargetDistance(){
        lastGeneratedTargetDistance +=delta;
        return lastGeneratedTargetDistance * Math.random() + (lastGeneratedTargetDistance - delta);
    }
}
