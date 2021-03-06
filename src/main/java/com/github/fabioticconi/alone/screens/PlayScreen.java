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

package com.github.fabioticconi.alone.screens;

import asciiPanel.AsciiPanel;
import com.artemis.ComponentMapper;
import com.artemis.annotations.Wire;
import com.artemis.managers.PlayerManager;
import com.artemis.utils.BitVector;
import com.github.fabioticconi.alone.Main;
import com.github.fabioticconi.alone.components.*;
import com.github.fabioticconi.alone.components.attributes.Sight;
import com.github.fabioticconi.alone.constants.DamageType;
import com.github.fabioticconi.alone.constants.Side;
import com.github.fabioticconi.alone.messages.AbstractMessage;
import com.github.fabioticconi.alone.messages.CannotMsg;
import com.github.fabioticconi.alone.messages.Msg;
import com.github.fabioticconi.alone.systems.*;
import com.github.fabioticconi.alone.utils.LongBag;
import com.github.fabioticconi.alone.utils.SingleGrid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rlforj.math.Point;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.EnumSet;
import java.util.Properties;
import java.util.Stack;

public class PlayScreen extends AbstractScreen
{
    static final Logger log = LoggerFactory.getLogger(PlayScreen.class);

    ComponentMapper<Speed>    mSpeed;
    ComponentMapper<Player>   mPlayer;
    ComponentMapper<Position> mPosition;
    ComponentMapper<Sprite>   mSprite;
    ComponentMapper<Sight>    mSight;
    ComponentMapper<Size>     mSize;
    ComponentMapper<Hunger>   mHunger;
    ComponentMapper<Health>   mHealth;
    ComponentMapper<Stamina>  mStamina;

    ActionSystem  sAction;
    BumpSystem    sBump;
    ItemSystem    sItems;
    MapSystem     map;
    MessageSystem msg;
    ScreenSystem  screen;
    TimeSystem    sTime;

    @Wire
    Properties properties;

    PlayerManager pManager;

    // FIXME only for debug..
    private float savedSpeed = -1f;

    public float handleKeys(final BitVector keys)
    {
        // FIXME: hackish, very crappy but it should work
        final int playerId = pManager.getEntitiesOfPlayer("player").get(0).getId();

        final Stamina stamina = mStamina.get(playerId);

        if (keys.get(KeyEvent.VK_UP))
        {
            if (stamina.exhausted)
            {
                msg.send(playerId, new CannotMsg("move", "while exhausted"));

                return 0f;
            }

            if (keys.get(KeyEvent.VK_LEFT))
            {
                // northwest
                return sBump.bumpAction(playerId, Side.NW);
            }
            else if (keys.get(KeyEvent.VK_RIGHT))
            {
                // northeast
                return sBump.bumpAction(playerId, Side.NE);
            }
            else
            {
                // north
                return sBump.bumpAction(playerId, Side.N);
            }
        }
        else if (keys.get(KeyEvent.VK_DOWN))
        {
            if (stamina.exhausted)
            {
                msg.send(playerId, new CannotMsg("move", "while exhausted"));

                return 0f;
            }

            if (keys.get(KeyEvent.VK_LEFT))
            {
                // southwest
                return sBump.bumpAction(playerId, Side.SW);
            }
            else if (keys.get(KeyEvent.VK_RIGHT))
            {
                // southeast
                return sBump.bumpAction(playerId, Side.SE);
            }
            else
            {
                // south
                return sBump.bumpAction(playerId, Side.S);
            }
        }
        else if (keys.get(KeyEvent.VK_RIGHT))
        {
            if (stamina.exhausted)
            {
                msg.send(playerId, new CannotMsg("move", "while exhausted"));

                return 0f;
            }

            // east
            return sBump.bumpAction(playerId, Side.E);
        }
        else if (keys.get(KeyEvent.VK_LEFT))
        {
            if (stamina.exhausted)
            {
                msg.send(playerId, new CannotMsg("move", "while exhausted"));

                return 0f;
            }

            // west
            return sBump.bumpAction(playerId, Side.W);
        }
        else if (keys.get(KeyEvent.VK_G))
        {
            keys.clear();

            return sAction.act(sItems.get(playerId));
        }
        else if (keys.get(KeyEvent.VK_D))
        {
            keys.clear();

            Main.pause();

            screen.select(DropScreen.class);

            return 0f;
        }
        else if (keys.get(KeyEvent.VK_E))
        {
            keys.clear();

            Main.pause();

            screen.select(EatScreen.class);

            return 0f;
        }
        else if (keys.get(KeyEvent.VK_L))
        {
            keys.clear();

            Main.pause();

            screen.select(LookScreen.class);

            return 0f;
        }
        else if (keys.get(KeyEvent.VK_T))
        {
            keys.clear();

            final int weaponId = sItems.getWeapon(playerId, EnumSet.allOf(DamageType.class), true);

            if (weaponId < 0)
            {
                // TODO: it would be cool if we could support proper screen chaining.
                // Eg, here we should actually select the EquipScreen AND follow it with a LookScreen.

                msg.send(playerId, new Msg("must equip a weapon first"));

                return 0f;
            }

            Main.pause();

            screen.select(LookScreen.class);
        }
        else if (keys.get(KeyEvent.VK_W))
        {
            keys.clear();

            Main.pause();

            screen.select(EquipScreen.class);

            return 0f;
        }
        else if (keys.get(KeyEvent.VK_C))
        {
            keys.clear();

            Main.pause();

            screen.select(CraftScreen.class);

            return 0f;
        }
        else if (keys.get(KeyEvent.VK_ESCAPE))
        {
            keys.clear();

            Main.pause();

            screen.select(StartScreen.class);
        }
        else if (keys.get(KeyEvent.VK_F1))
        {
            keys.clear();

            if (savedSpeed == -1f)
                savedSpeed = mSpeed.get(playerId).value;

            mSpeed.get(playerId).value = 0f; // FIXME to remove later, only for debug
        }
        else if (keys.get(KeyEvent.VK_F2))
        {
            keys.clear();

            if (savedSpeed >= 0f)
                mSpeed.create(playerId).value = savedSpeed; // FIXME to remove later, only for debug
        }
        else if (keys.get(KeyEvent.VK_SPACE))
        {
            if (keys.get(KeyEvent.VK_CONTROL))
            {
                // Ctrl+Space means we are toggling the real-time mode
                Main.realtime = !Main.realtime;
                // as well as toggling the pause, of course
                Main.paused = !Main.paused;

                keys.clear();

                return 0f;
            }

            keys.clear();

            if (Main.realtime)
            {
                // in real-time mode, SPACE just means pausing (or unpausing)

                Main.paused = !Main.paused;
            }
            else
            {
                // in turn-based mode, SPACE means "unpause", which we achieve by
                // simply setting a player action equal to the world delta.
                // this will

                return world.delta;
            }
        }

        return 0f;
    }

    public void display(final AsciiPanel terminal)
    {
        // FIXME: hackish, very crappy
        final int playerId = pManager.getEntitiesOfPlayer("player").get(0).getId();

        final Player   player = mPlayer.get(playerId);
        final Position pos    = mPosition.get(playerId);

        // use for distance calculations
        final Point p = new Point(0, 0);

        final int xmin = 0;
        final int ymin = 6;
        final int xmax = terminal.getWidthInCharacters();
        final int ymax = terminal.getHeightInCharacters();

        final int panelSize = 8;

        final int halfcols = xmax / 2;
        final int halfrows = (ymax - panelSize + ymin) / 2;

        Sprite sprite;
        Size   size;

        final SingleGrid obstacles = map.getObstacles();
        final SingleGrid items     = map.getItems();

        int sight = mSight.get(playerId).value;

        final float hours     = sTime.getHoursFromMidnight();
        final int   darkTimes = Math.max((int) (5f - hours), 0);

        if (hours < 8)
            sight = Math.max((int) ((hours / 7f) * sight), Math.min(sight, 3));

        final LongBag cells = map.getVisibleCells(pos.x, pos.y, sight);

        // clearing everything
        terminal.clear(' ');

        // title:
        drawHeader(terminal);

        for (int x = xmin; x < xmax; x++)
        {
            for (int y = ymin; y < ymax - panelSize; y++)
            {
                p.x = pos.x + x - halfcols;
                p.y = pos.y + y - halfrows;

                final long key = p.x | ((long) p.y << 32);

                if (map.contains(p.x, p.y))
                {
                    // render terrain
                    final MapSystem.Cell cell = map.get(p.x, p.y);

                    char        c = cell.c;
                    Color       tileFg;
                    final Color tileBg;

                    // if visible, draw terrain and item, if present
                    if (cells.contains(key))
                    {
                        // terrain graphics
                        tileFg = darken(cell.col, 1);
                        tileBg = cell.col;

                        // if there's an item, we paint that instead (keeping terrain's tileBg)
                        if (!items.isEmpty(p.x, p.y))
                        {
                            final int itemId = items.get(p.x, p.y);

                            sprite = mSprite.get(itemId);

                            // we keep the terrain's background
                            if (sprite != null)
                            {
                                c = sprite.c;
                                tileFg = sprite.col;
                            }
                        }
                    }
                    else
                    {
                        // if not visible, items are never visible and terrain is "shaded"

                        tileFg = darken(cell.col, 4);
                        tileBg = darken(cell.col, 3);
                    }

                    // if there's an obstacle, we paint that both in the light and in the shadow
                    if (!obstacles.isEmpty(p.x, p.y))
                    {
                        final int entityId = obstacles.get(p.x, p.y);

                        sprite = mSprite.get(entityId);

                        if (sprite != null)
                        {
                            size = mSize.get(entityId);

                            // bigger obstacles letters are upper-cased (eg, B instead of b for buffalos)
                            final char tempC = (size != null && size.value > 0) ?
                                                   Character.toUpperCase(sprite.c) :
                                                   sprite.c;

                            if (cells.contains(key))
                            {
                                tileFg = sprite.col;
                                c = tempC;
                            }
                            else if (sprite.shadowView)
                            {
                                // shadowed obstacles are darker than normal

                                tileFg = darken(sprite.col, 3);
                                c = tempC;
                            }
                        }
                    }

                    // finally, we actually write this to terminal

                    terminal.write(c, x, y, darken(tileFg, darkTimes), darken(tileBg, darkTimes));
                }
                else
                {
                    // pure black outside boundaries
                    terminal.write(' ', x, y);
                }
            }
        }

        final Hunger  hunger  = mHunger.get(playerId);
        final Health  health  = mHealth.get(playerId);
        final Stamina stamina = mStamina.get(playerId);

        int x;

        final int yoff = ymin - 3;

        // health bar
        terminal.write('[', 0, yoff, Color.RED);
        for (x = 1; x < 11; x++)
        {
            if (x <= health.value * 10f / health.maxValue)
                terminal.write('=', x, yoff, Color.RED);
            else
                terminal.write(' ', x, yoff, Color.RED);
        }
        terminal.write(']', x, yoff, Color.RED);

        // stamina bar
        terminal.write('[', 0, yoff + 1, Color.YELLOW);
        for (x = 1; x < 11; x++)
        {
            if (x <= stamina.value * 10f / stamina.maxValue)
                terminal.write('=', x, yoff + 1, Color.YELLOW);
            else
                terminal.write(' ', x, yoff + 1, Color.YELLOW);
        }
        terminal.write(']', x, yoff + 1, Color.YELLOW);

        // hunger bar
        terminal.write(']', xmax - 1, yoff, Color.ORANGE.darker());
        for (x = 1; x < 11; x++)
        {
            if (x <= hunger.value * 10f / hunger.maxValue)
                terminal.write('=', xmax - x - 1, yoff, Color.ORANGE.darker());
            else
                terminal.write(' ', xmax - x - 1, yoff, Color.ORANGE.darker());
        }
        terminal.write('[', xmax - x - 1, yoff, Color.ORANGE.darker());

        // small panel: combat log
        final Stack<AbstractMessage> messages = player.messages;
        for (int i = 1; i <= panelSize; i++)
        {
            if (messages.size() < i)
            {
                terminal.clear(' ', 0, ymax - i, xmax, 1, Color.WHITE, Color.BLACK);

                continue;
            }

            final AbstractMessage msg = messages.get(messages.size() - i);

            if (msg.distance > sight)
            {
                terminal.clear(' ', 0, ymax - i, xmax, 1, Color.WHITE, Color.BLACK);

                continue;
            }

            String smsg = msg.format();

            if (smsg.length() >= xmax)
                smsg = smsg.substring(0, xmax - 1);

            terminal.write(smsg, 0, ymax - i, msg.fgCol, msg.bgCol);

            if (smsg.length() < xmax)
                terminal.clear(' ', smsg.length(), ymax - i, xmax - smsg.length(), 1, Color.WHITE, Color.BLACK);
        }

        // FIXME: this is crap, inefficient, horrible. Must change the Stack with a better data structure.
        for (int i = 0; i < messages.size() - panelSize; i++)
        {
            messages.remove(i);
        }

        // player.messages.clear();
    }

    @Override
    public String header()
    {
        return properties.getProperty("name");
    }

    // FIXME: horribly inefficient, we should be using HSB values here
    public static Color darken(final Color col, final int times)
    {
        if (times <= 0)
            return col;

        Color newCol = col;

        for (int i = 0; i < times; i++)
        {
            newCol = newCol.darker();
        }

        return newCol;
    }
}
