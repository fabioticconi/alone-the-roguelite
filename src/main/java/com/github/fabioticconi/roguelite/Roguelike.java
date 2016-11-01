package com.github.fabioticconi.roguelite;

import asciiPanel.AsciiFont;
import asciiPanel.AsciiPanel;
import com.artemis.World;
import com.artemis.WorldConfiguration;
import com.artemis.managers.PlayerManager;
import com.artemis.utils.BitVector;
import com.github.fabioticconi.roguelite.behaviours.*;
import com.github.fabioticconi.roguelite.constants.Options;
import com.github.fabioticconi.roguelite.map.EntityGrid;
import com.github.fabioticconi.roguelite.map.Map;
import com.github.fabioticconi.roguelite.systems.*;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Random;

public class Roguelike extends JFrame implements KeyListener
{
    public static boolean keepRunning = true;

    private final int   fps          = 25;
    private final long  deltaNanos   = Math.round(1000000000.0d / (double) fps);
    private final float deltaSeconds = 1.0f / (float) fps;

    private final AsciiPanel        terminal;
    private final World             world;
    private final PlayerInputSystem input;
    private final RenderSystem      render;

    // currently pressed keys
    private final BitVector pressed;


    public Roguelike()
    {
        super();
        terminal = new AsciiPanel(Options.OUTPUT_SIZE_X, Options.OUTPUT_SIZE_Y, AsciiFont.CP437_12x12);
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
        // POJO
        config.register(new Map());
        config.register(new EntityGrid());
        config.register(new Random());
        // passive systems, one-timers, managers etc
        config.setSystem(BootstrapSystem.class); // once
        config.setSystem(PlayerManager.class);
        config.setSystem(GroupSystem.class);
        config.setSystem(input);
        config.setSystem(render);
        // fixed interval
        config.setSystem(new HungerSystem(5f));
        // by-entity interval
        config.setSystem(AISystem.class);
        config.setSystem(MovementSystem.class);
        // ai behaviours (passive)
        config.setSystem(FleeBehaviour.class);
        config.setSystem(GrazeBehaviour.class);
        config.setSystem(ChaseBehaviour.class);
        config.setSystem(FlockBehaviour.class);
        config.setSystem(WanderBehaviour.class);

        world = new World(config);

        addKeyListener(this);
    }

    public static void main(final String[] args)
    {
        final Roguelike app = new Roguelike();
        app.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        app.setLocationRelativeTo(null);
        app.setVisible(true);

        app.loop();

        app.dispose();
    }

    public void loop()
    {
        long previousTime = System.nanoTime();
        long currentTime;

        long lag = 0l;
        long elapsed;

        // FIXME
        world.process();

        // FIXME: https://github.com/TomGrill/logic-render-game-loop
        // needs to modify that, so that I can divide systems in three groups:
        // input collection/processing, logic, output sending
        // this is because the first and the last will be only processed once,
        // while the logic ones can be re-processed until the lag is gone

        while (keepRunning)
        {
            currentTime = System.nanoTime();
            elapsed = currentTime - previousTime;
            previousTime = currentTime;

            if (elapsed > 250000000l)
            {
                System.out.println("lagging behind: " + elapsed/1000000.0f + " ms");
                elapsed = 250000000l;
            }

            lag += elapsed;

            // TODO: we can poll the active keys from the "key map" (thread-safe!)
            // In this way, the system will not know
            input.handleKeys(pressed);

            // we do the actual computation in nanoseconds, using long numbers to avoid sneaky float
            // incorrectness.
            // however, artemis-odb wants a float delta representing seconds, so that's what we give.
            // since we use fixed timestep, this is equivalent
            // FIXME: check if deltaNanos rounding affects the system with certain fps (eg, 60)
            while (lag >= deltaNanos)
            {
                world.setDelta(deltaSeconds);
                world.process();

                lag -= deltaNanos;
            }

            repaint();

            // FIXME: to remove when actual rendering and input processing is implemented
            try
            {
                Thread.sleep(40);
            } catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    }

    @Override public void repaint()
    {
        terminal.clear();
        render.display(terminal);
        super.repaint();
    }

    @Override public void keyPressed(final KeyEvent e)
    {
        pressed.set(e.getKeyCode());

        System.out.println(e.getKeyCode() + " " + e.getKeyChar() + " (" + KeyEvent.VK_NUMBER_SIGN + ")");
        System.out.println(e.getExtendedKeyCode() + " " + e.getKeyLocation());
    }

    @Override public void keyReleased(final KeyEvent e)
    {
        // we don't check the capacity because we know the key must have been pressed before
        pressed.unsafeClear(e.getKeyCode());
    }

    @Override public void keyTyped(final KeyEvent e)
    {
    }
}
