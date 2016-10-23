package com.github.fabioticconi.roguelike.map;

import org.junit.Before;
import org.junit.Test;

import com.github.fabioticconi.roguelike.utils.Coords;

import it.unimi.dsi.fastutil.longs.LongSet;

public class MapTest
{
    Map map;

    @Before
    public void setup()
    {
        map = new Map();
    }

    @Test
    public void testGetFirstFreeExit() throws Exception
    {
        throw new RuntimeException("not yet implemented");
    }

    @Test
    public void testGetFreeExitRandomised() throws Exception
    {
        throw new RuntimeException("not yet implemented");
    }

    @Test
    public void testGetFirstOfType() throws Exception
    {
        throw new RuntimeException("not yet implemented");
    }

    @Test
    public void testBasics() throws Exception
    {
        // test get, set, contains, isObstacle
        throw new RuntimeException("not yet implemented");
    }

    @Test
    public void testGetVisibleCells() throws Exception
    {
        final LongSet set = map.getVisibleCells(100, 100, 10);

        int[] coords;
        for (final long key : set)
        {
            coords = Coords.unpackCoords(key);
            System.out.println("("
                    + coords[0]
                    + ", "
                    + coords[1]
                    + ") --> "
                    + Coords.distanceBlock(100, 100, coords[0], coords[1])
                    + ", "
                    + Coords.distanceChebyshev(100, 100, coords[0], coords[1])
                    + ", "
                    + Coords.distancePseudoEuclidean(100, 100, coords[0], coords[1]));
        }

        int count = 0;
        for (final long key : set)
        {
            coords = Coords.unpackCoords(key);

            if (Coords.distancePseudoEuclidean(100, 100, coords[0], coords[1]) > 11f)
            {
                count++;
            }
        }

        System.out.println("coords: tot=" + set.size() + ", >10=" + count);
    }

    @Test
    public void testGetLineOfSight() throws Exception
    {
        throw new RuntimeException("not yet implemented");
    }

}