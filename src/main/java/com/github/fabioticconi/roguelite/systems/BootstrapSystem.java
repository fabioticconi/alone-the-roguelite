/*
 * Copyright (C) 2017 Fabio Ticconi
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.github.fabioticconi.roguelite.systems;

import com.artemis.BaseSystem;
import com.artemis.EntityEdit;
import com.artemis.annotations.Wire;
import com.artemis.managers.PlayerManager;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.fabioticconi.roguelite.behaviours.*;
import com.github.fabioticconi.roguelite.components.*;
import com.github.fabioticconi.roguelite.components.attributes.*;
import com.github.fabioticconi.roguelite.constants.Cell;
import com.github.fabioticconi.roguelite.constants.Options;
import com.github.fabioticconi.roguelite.map.Map;
import com.github.fabioticconi.roguelite.map.SingleGrid;
import com.github.fabioticconi.roguelite.utils.Util;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Random;

/**
 * @author Fabio Ticconi
 */
public class BootstrapSystem extends BaseSystem
{
    @Wire
    Map        map;
    @Wire
    SingleGrid grid;
    @Wire
    Random     r;

    GroupSystem sGroup;

    PlayerManager pManager;

    /*
     * (non-Javadoc)
     *
     * @see com.artemis.BaseSystem#processSystem()
     */
    @Override
    protected void processSystem()
    {
        // this must be only run once
        setEnabled(false);

        int x;
        int y;

        // add player
        int        id   = world.create();
        EntityEdit edit = world.edit(id);

        // load the player's data
        try
        {
            loadBody("data/creatures/player.yaml", edit);
        } catch (final IOException e)
        {
            e.printStackTrace();
            System.exit(1);
        }

        edit.create(Player.class);
        x = Options.MAP_SIZE_X / 2;
        y = Options.MAP_SIZE_Y / 2;
        edit.create(Position.class).set(x, y);
        edit.create(Sprite.class).set('@', Color.WHITE);
        grid.set(x, y, id);
        pManager.setPlayer(world.getEntity(id), "player");
        System.out.println("setPlayer");
        edit.create(Speed.class).value = 0f; // FIXME to remove later, only for debug

        // add a herd of buffalos
        int    groupId = sGroup.createGroup();
        IntSet group   = sGroup.getGroup(groupId);
        for (int i = 0; i < 5; i++)
        {
            id = world.create();
            edit = world.edit(id);

            try
            {
                loadBody("data/creatures/buffalo.yaml", edit);
            } catch (final IOException e)
            {
                e.printStackTrace();
                System.exit(1);
            }

            final AI ai = new AI(r.nextFloat() * AISystem.BASE_TICKTIME + 1.0f);
            ai.behaviours.add(world.getSystem(FleeBehaviour.class));
            ai.behaviours.add(world.getSystem(GrazeBehaviour.class));
            ai.behaviours.add(world.getSystem(FlockBehaviour.class));
            ai.behaviours.add(world.getSystem(WanderBehaviour.class));
            edit.add(ai);
            x = (Options.MAP_SIZE_X / 2) + r.nextInt(10) - 5;
            y = (Options.MAP_SIZE_Y / 2) + r.nextInt(10) - 5;
            edit.create(Position.class).set(x, y);
            edit.create(Group.class).groupId = groupId;
            group.add(id);
            edit.create(Alertness.class).value = 0.0f;
            edit.create(Sprite.class).set('b', Util.BROWN);

            grid.set(x, y, id);
        }

        // add small, independent rabbits/hares
        for (int i = 0; i < 3; i++)
        {
            id = world.create();
            edit = world.edit(id);

            try
            {
                loadBody("data/creatures/rabbit.yaml", edit);
            } catch (final IOException e)
            {
                e.printStackTrace();
                System.exit(1);
            }

            final AI ai = new AI(r.nextFloat() * AISystem.BASE_TICKTIME + 1.0f);
            ai.behaviours.add(world.getSystem(FleeBehaviour.class));
            ai.behaviours.add(world.getSystem(GrazeBehaviour.class));
            ai.behaviours.add(world.getSystem(WanderBehaviour.class));
            edit.add(ai);
            x = (Options.MAP_SIZE_X / 2) + r.nextInt(10) - 5;
            y = (Options.MAP_SIZE_Y / 2) + r.nextInt(10) - 5;
            edit.create(Position.class).set(x, y);
            edit.create(Alertness.class).value = 0.0f;
            edit.create(Sprite.class).set('r', Color.LIGHT_GRAY);

            grid.set(x, y, id);
        }

        // add a pack of wolves
        groupId = sGroup.createGroup();
        group = sGroup.getGroup(groupId);
        for (int i = 0; i < 5; i++)
        {
            id = world.create();
            edit = world.edit(id);

            try
            {
                loadBody("data/creatures/wolf.yaml", edit);
            } catch (final IOException e)
            {
                e.printStackTrace();
                System.exit(1);
            }

            final AI ai = new AI(r.nextFloat() * AISystem.BASE_TICKTIME + 1.0f);
            ai.behaviours.add(world.getSystem(ChaseBehaviour.class));
            ai.behaviours.add(world.getSystem(FlockBehaviour.class));
            ai.behaviours.add(world.getSystem(WanderBehaviour.class));
            edit.add(ai);
            x = (Options.MAP_SIZE_X / 2) + r.nextInt(10) - 5;
            y = (Options.MAP_SIZE_Y / 2) + r.nextInt(10) - 5;
            edit.create(Position.class).set(x, y);
            edit.create(Group.class).groupId = groupId;
            group.add(id);
            edit.create(Alertness.class).value = 0.0f;
            edit.create(Sprite.class).set('w', Color.DARK_GRAY);

            grid.set(x, y, id);
        }

        // add solitary pumas
        for (int i = 0; i < 3; i++)
        {
            id = world.create();
            edit = world.edit(id);

            try
            {
                loadBody("data/creatures/puma.yaml", edit);
            } catch (final IOException e)
            {
                e.printStackTrace();
                System.exit(1);
            }

            final AI ai = new AI(r.nextFloat() * AISystem.BASE_TICKTIME + 1.0f);
            ai.behaviours.add(world.getSystem(ChaseBehaviour.class));
            ai.behaviours.add(world.getSystem(WanderBehaviour.class));
            edit.add(ai);
            x = (Options.MAP_SIZE_X / 2) + r.nextInt(10) - 5;
            y = (Options.MAP_SIZE_Y / 2) + r.nextInt(10) - 5;
            edit.create(Position.class).set(x, y);
            edit.create(Alertness.class).value = 0.0f;
            edit.create(Sprite.class).set('p', Util.BROWN.darker());

            grid.set(x, y, id);
        }

        // add random trees?
        for (x = 0; x < Options.MAP_SIZE_X; x++)
        {
            for (y = 0; y < Options.MAP_SIZE_Y; y++)
            {
                final Cell cell = map.get(x, y);

                if ((cell.equals(Cell.GRASS) && r.nextGaussian() > 3f) ||
                    (cell.equals(Cell.HILL_GRASS) && r.nextGaussian() > 2f) ||
                    (cell.equals(Cell.HILL) && r.nextGaussian() > 3f))
                {
                    id = world.create();
                    edit = world.edit(id);

                    edit.create(Position.class).set(x, y);
                    edit.create(Sprite.class).set('T', Color.GREEN.brighter());
                    edit.create(Obstacle.class);

                    // FIXME: we should only need one or the other to determine if obstacle.
                    // ie, the map should be able to get the Obstacle component from the SingleGrid
                    // to determine if the cell is obstructed or not.

                    grid.set(x, y, id);
                    map.setObstacle(x, y);
                }
            }
        }

        System.out.println("Bootstrap done");
    }

    public void loadBody(final String filename, final EntityEdit edit) throws IOException
    {
        final YAMLFactory factory = new YAMLFactory();
        final JsonParser  parser  = factory.createParser(new File(filename));

        int     str       = 0, agi = 0, con = 0, skin = 0, sight = 0;
        boolean herbivore = false, carnivore = false;

        parser.nextToken(); // START_OBJECT

        while (parser.nextToken() != null)
        {
            final String name = parser.getCurrentName();

            if (name == null)
                break;

            parser.nextToken(); // get in value

            System.out.println(name);

            if (name.equals("strength"))
            {
                str = parser.getIntValue();
                edit.create(Strength.class).value = Util.ensureRange(str, -2, 2);
            }
            else if (name.equals("agility"))
            {
                agi = parser.getIntValue();
                edit.create(Agility.class).value = Util.ensureRange(agi, -2, 2);
            }
            else if (name.equals("constitution"))
            {
                con = parser.getIntValue();
                edit.create(Constitution.class).value = Util.ensureRange(con, -2, 2);
            }
            else if (name.equals("skin"))
            {
                skin = parser.getIntValue();
                edit.create(Skin.class).value = Util.ensureRange(skin, -2, 2);
            }
            else if (name.equals("sight"))
            {
                sight = parser.getIntValue();
                edit.create(Sight.class).value = Util.ensureRange(sight, 1, 18);
            }
            else if (name.equals("herbivore"))
            {
                herbivore = parser.getBooleanValue();

                if (herbivore)
                    edit.create(Herbivore.class);
            }
            else if (name.equals("carnivore"))
            {
                carnivore = parser.getBooleanValue();

                if (carnivore)
                    edit.create(Carnivore.class);
            }
        }

        // TODO check if neither herbivore nor carnivore? player is currently as such, for testing

        // Secondary Attributes
        final int size = Math.round((con - agi) / 2f);
        edit.create(Size.class).value = size;
        edit.create(Stamina.class).value = 5 + str + con;
        edit.create(Speed.class).value = (con - str - agi + 6) / 12f;

        // Tertiary Attributes
        edit.create(Hunger.class).value = (size / 2f) + 2f;
    }
}