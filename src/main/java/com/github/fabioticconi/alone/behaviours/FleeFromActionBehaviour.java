/*
 * Copyright (C) 2015-2017 Fabio Ticconi
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

package com.github.fabioticconi.alone.behaviours;

import com.artemis.Aspect;
import com.artemis.ComponentMapper;
import com.artemis.utils.IntBag;
import com.github.fabioticconi.alone.components.Position;
import com.github.fabioticconi.alone.components.Speed;
import com.github.fabioticconi.alone.components.Underwater;
import com.github.fabioticconi.alone.components.actions.Action;
import com.github.fabioticconi.alone.components.attributes.Sight;
import com.github.fabioticconi.alone.constants.Side;
import com.github.fabioticconi.alone.systems.BumpSystem;
import com.github.fabioticconi.alone.systems.MapSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Author: Fabio Ticconi
 * Date: 21/10/17
 */
public class FleeFromActionBehaviour extends AbstractBehaviour
{
    static final Logger log = LoggerFactory.getLogger(FleeBehaviour.class);

    ComponentMapper<Sight>      mSight;
    ComponentMapper<Position>   mPosition;
    ComponentMapper<Action>     mAction;
    ComponentMapper<Underwater> mUnderWater;

    BumpSystem sBump;

    MapSystem sMap;

    Position curPos;
    Position fleeFrom;

    @Override
    protected void initialize()
    {
        aspect = Aspect.all(Position.class, Speed.class, Sight.class, Underwater.class).build(world);

        fleeFrom = new Position(0, 0);
    }

    @Override
    public float evaluate(final int entityId)
    {
        this.entityId = entityId;

        if (!interested(entityId))
            return 0f;

        curPos = mPosition.get(entityId);
        final int sight = mSight.get(entityId).value;

        final IntBag creatures = sMap.getObstacles().getEntities(sMap.getVisibleCells(curPos.x, curPos.y, sight));

        if (creatures.isEmpty())
            return 0f;

        fleeFrom.x = 0;
        fleeFrom.y = 0;

        int      count = 0;
        Position tempPos;
        for (int i = 0, size = creatures.size(); i < size; i++)
        {
            final int creatureId = creatures.get(i);

            // only avoid non-fish
            if (mAction.has(creatureId) && !mUnderWater.has(creatureId))
            {
                tempPos = mPosition.get(creatureId);

                fleeFrom.x += tempPos.x;
                fleeFrom.y += tempPos.y;

                count++;
            }
        }

        if (count == 0)
            return 0f;

        fleeFrom.x = Math.floorDiv(fleeFrom.x, count);
        fleeFrom.y = Math.floorDiv(fleeFrom.y, count);

        return 0.9f;
    }

    @Override
    public float update()
    {
        Side direction = Side.getSide(curPos.x, curPos.y, fleeFrom.x, fleeFrom.y);

        if (!sMap.isFree(curPos.x, curPos.y, direction))
        {
            // go to a random direction, whether free or not!
            // note how this could result in animals killing members of their own group and such,
            // which we take as simulating a stampede.
            // for fish in particular this might cause

            direction = Side.getRandom();
        }

        return sBump.bumpAction(entityId, direction);
    }
}
