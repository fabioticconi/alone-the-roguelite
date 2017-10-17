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

package com.github.fabioticconi.alone;

import asciiPanel.AsciiFont;
import asciiPanel.AsciiPanel;
import com.artemis.World;
import com.artemis.WorldConfiguration;
import com.artemis.link.EntityLinkManager;
import com.artemis.managers.PlayerManager;
import com.artemis.managers.WorldSerializationManager;
import com.artemis.utils.BitVector;
import com.github.fabioticconi.alone.behaviours.*;
import com.github.fabioticconi.alone.constants.Options;
import com.github.fabioticconi.alone.map.MapSystem;
import com.github.fabioticconi.alone.map.MultipleGrid;
import com.github.fabioticconi.alone.map.SingleGrid;
import com.github.fabioticconi.alone.systems.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.util.Random;

public class Main extends JFrame implements KeyListener
{
    static final  Logger  log          = LoggerFactory.getLogger(Main.class);
    public static boolean keepRunning  = true;
    public static boolean paused       = false;
    public static boolean realtime     = false;
    private final int     fps          = 25;
    private final long    deltaNanos   = Math.round(1000000000.0d / (double) fps);
    private final float   deltaSeconds = 1.0f / (float) fps;
    private final AsciiPanel        terminal;
    private final World             world;
    private final PlayerInputSystem input;
    private final RenderSystem      render;
    // currently pressed keys
    private final BitVector         pressed;

    public Main() throws IOException
    {
        super();
        terminal = new AsciiPanel(Options.OUTPUT_SIZE_X, Options.OUTPUT_SIZE_Y, AsciiFont.TALRYTH_15_15);
        add(terminal);
        pack();

        pressed = new BitVector(255);

        // Input and render are sort of "binders" between the GUI and the logic.
        // They are both passive: the input system receives raw player commands (when in "play screen")
        // and converts it to artemis "things", then starts a player action. Should be pretty immediate.
        // The render system is called whenever the play screen is active and the map needs to be painted.
        // It needs to be a system for us to be able to leverage the components on the entities, of course.
        input = new PlayerInputSystem();
        render = new RenderSystem();

        final WorldConfiguration config;
        config = new WorldConfiguration();
        // first thing to be loaded
        config.setSystem(MapSystem.class);
        // POJOs
        config.register(new SingleGrid());
        config.register(new MultipleGrid());
        config.register(new Random());
        // passive systems, one-timers, managers etc
        config.setSystem(EntityLinkManager.class);
        config.setSystem(BootstrapSystem.class);
        config.setSystem(PlayerManager.class);
        config.setSystem(GroupSystem.class);
        config.setSystem(WorldSerializationManager.class);
        config.setSystem(ActionSystem.class);
        config.setSystem(input);
        config.setSystem(render);
        // actual game logic
        config.setSystem(new HealthSystem(5f));
        config.setSystem(new StaminaSystem(1f));
        config.setSystem(new HungerSystem(1f));
        config.setSystem(AISystem.class);
        config.setSystem(BumpSystem.class);
        config.setSystem(PathSystem.class);
        config.setSystem(MovementSystem.class);
        config.setSystem(AttackSystem.class);
        config.setSystem(ItemSystem.class);
        config.setSystem(TreeSystem.class);
        config.setSystem(PushSystem.class);
        config.setSystem(CrushSystem.class);
        config.setSystem(ThrowSystem.class);
        // ai behaviours (passive)
        config.setSystem(FleeBehaviour.class);
        config.setSystem(GrazeBehaviour.class);
        config.setSystem(ChaseBehaviour.class);
        config.setSystem(FlockBehaviour.class);
        config.setSystem(ScavengeBehaviour.class);
        config.setSystem(WanderBehaviour.class);
        config.setSystem(UnderwaterBehaviour.class);
        // last systems
        config.setSystem(DeadSystem.class);

        world = new World(config);

        addKeyListener(this);
    }

    public static void main(final String[] args) throws IOException
    {
        final Main app = new Main();
        app.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        app.setLocationRelativeTo(null);
        app.setVisible(true);

        app.loop();

        app.dispose();
    }

    public void loop()
    {
        long previousTime = System.nanoTime();
        long currentTime;

        long lag = 0L;
        long elapsed;

        // FIXME: without this the first rendering happens before the first process
        world.process();

        // FIXME: https://github.com/TomGrill/logic-render-game-loop
        // needs to modify that, so that I can divide systems in three groups:
        // input collection/processing, logic, output sending
        // this is because the first and the last will be only processed once,
        // while the logic ones can be re-processed until the lag is gone

        float pActionTime = 0f;

        while (keepRunning)
        {
            currentTime = System.nanoTime();
            elapsed = currentTime - previousTime;
            previousTime = currentTime;

            if (elapsed > 250000000L)
            {
                log.info("lagging behind: {} ms", elapsed / 1000000.0f);
                elapsed = 250000000L;
            }

            lag += elapsed;

            final float curPActionTime = input.handleKeys(pressed);

            // a new action was issued
            if (curPActionTime > 0f)
            {
                // even in real time mode a player action unpauses the game
                paused = false;
                pActionTime = curPActionTime;
            }

            // We do the actual computation in nanoseconds, using long numbers to avoid sneaky float
            // incorrectness.
            // However, artemis-odb wants a float delta representing seconds, so that's what we give.
            // Since we use fixed timestep, this is equivalent
            // FIXME: check if deltaNanos rounding affects the system with certain fps (eg, 60)
            while (lag >= deltaNanos)
            {
                world.setDelta(deltaSeconds);

                // TODO: add here, in the else, a sleep?
                if (!paused)
                    world.process();

                lag -= deltaNanos;
                pActionTime -= deltaSeconds;
            }

            if (pActionTime <= 0f && !realtime)
                Main.paused = true;

            repaint();

            // FIXME: to remove when actual rendering and input processing is implemented
            try
            {
                Thread.sleep(40);
            } catch (final InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void repaint()
    {
        terminal.clear();
        render.display(terminal);
        super.repaint();
    }

    @Override
    public void keyPressed(final KeyEvent e)
    {
        pressed.set(e.getKeyCode());
    }

    @Override
    public void keyReleased(final KeyEvent e)
    {
        pressed.clear(e.getKeyCode());
    }

    @Override
    public void keyTyped(final KeyEvent e)
    {
    }
}
