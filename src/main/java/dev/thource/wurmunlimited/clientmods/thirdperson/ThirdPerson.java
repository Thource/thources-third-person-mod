package dev.thource.wurmunlimited.clientmods.thirdperson;

import com.wurmonline.client.game.World;
import com.wurmonline.client.renderer.RenderVector;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.Initable;
import org.gotti.wurmunlimited.modloader.interfaces.PreInitable;
import org.gotti.wurmunlimited.modloader.interfaces.WurmClientMod;
import org.gotti.wurmunlimited.modsupport.console.ConsoleListener;
import org.gotti.wurmunlimited.modsupport.console.ModConsole;


public class ThirdPerson implements WurmClientMod, Initable, PreInitable, ConsoleListener {
    private boolean tpActive = false;
    private final float dist = 4f;

    private float zoomFactor = 1f;
    private static final float ZOOM_MAX = 5f;
    private static final float ZOOM_MIN = 0f;

    private float pitch = 45f;
    private static final float PITCH_MAX = 89.9f;
    private static final float PITCH_MIN = 1f;

    private float xOffset = 0f;
    private static final float X_OFFSET_MAX = 5f;
    private static final float X_OFFSET_MIN = -X_OFFSET_MAX;

    @Override
    public void preInit() {

    }

    @Override
    public void init() {
        HookManager.getInstance().registerHook("com.wurmonline.client.renderer.WorldRender",
                "calculateCameraOffset", null,
                () -> (proxy, method, args) -> {
                    method.invoke(proxy, args);
                    if (tpActive) {
                        RenderVector cameraOffset = ReflectionUtil.getPrivateField(
                                proxy, ReflectionUtil.getField(proxy.getClass(), "cameraOffset"));
                        World world = ReflectionUtil.getPrivateField(
                                proxy, ReflectionUtil.getField(proxy.getClass(), "world"));

                        RenderVector target = new RenderVector(xOffset, 0, 0);
                        target.rotateY(Math.toRadians(world.getPlayerRotX()));
                        cameraOffset.add(target);
                    }
                    //noinspection SuspiciousInvocationHandlerImplementation
                    return null;
                });

        HookManager.getInstance().registerHook("com.wurmonline.client.renderer.WorldRender",
                "getCameraX", null,
                () -> (proxy, method, args) -> {
                    if (!tpActive)
                        return method.invoke(proxy, args);

                    RenderVector cameraOffset = ReflectionUtil.getPrivateField(
                            proxy, ReflectionUtil.getField(proxy.getClass(), "cameraOffset"));
                    World world = ReflectionUtil.getPrivateField(
                            proxy, ReflectionUtil.getField(proxy.getClass(), "world"));

                    RenderVector target = new RenderVector(xOffset, 0, 0);
                    target.rotateY(Math.toRadians(world.getPlayerRotX()));

                    float x = cameraOffset.getX() - target.getX();
                    float z = cameraOffset.getZ() - target.getZ();
                    double yaw = Math.atan2(z, x);
                    setPitch(world.getPlayerRotY());
                    return (float) method.invoke(proxy, args) + Math.cos(Math.toRadians(pitch)) * Math.cos(yaw) * zoomFactor * -dist;
                });

        HookManager.getInstance().registerHook("com.wurmonline.client.renderer.WorldRender",
                "getCameraY", null,
                () -> (proxy, method, args) -> {
                    if (!tpActive)
                        return method.invoke(proxy, args);

                    World world = ReflectionUtil.getPrivateField(
                            proxy, ReflectionUtil.getField(proxy.getClass(), "world"));

                    setPitch(world.getPlayerRotY());

                    return world.getPlayerPosH() + 1.5f + Math.sin(Math.toRadians(getClampedPitch())) * zoomFactor * dist;
                });

        HookManager.getInstance().registerHook("com.wurmonline.client.renderer.WorldRender",
                "getCameraZ", null,
                () -> (proxy, method, args) -> {
                    if (!tpActive)
                        return method.invoke(proxy, args);

                    RenderVector cameraOffset = ReflectionUtil.getPrivateField(
                            proxy, ReflectionUtil.getField(proxy.getClass(), "cameraOffset"));
                    World world = ReflectionUtil.getPrivateField(
                            proxy, ReflectionUtil.getField(proxy.getClass(), "world"));

                    RenderVector target = new RenderVector(xOffset, 0, 0);
                    target.rotateY(Math.toRadians(world.getPlayerRotX()));

                    float x = cameraOffset.getX() - target.getX();
                    float z = cameraOffset.getZ() - target.getZ();
                    double yaw = Math.atan2(z, x);
                    setPitch(world.getPlayerRotY());
                    return (float) method.invoke(proxy, args) + Math.sin(yaw) * Math.cos(Math.toRadians(pitch)) * zoomFactor * -dist;
                });
        ModConsole.addConsoleListener(this);
    }

    public void addZoom(float factor) {
        setZoom(zoomFactor + factor);
    }

    public void setZoom(float factor) {
        zoomFactor = Math.max(ZOOM_MIN, Math.min(factor, ZOOM_MAX));
    }

    public void setPitch(float factor) {
        pitch = factor;
    }

    public void setXOffset(float factor) {
        xOffset = Math.max(X_OFFSET_MIN, Math.min(factor, X_OFFSET_MAX));
    }

    public float getClampedPitch() {
        return Math.max(PITCH_MIN, Math.min(pitch, PITCH_MAX));
    }

    @Override
    public boolean handleInput(String string, Boolean aBoolean) {
        if (string == null) return false;

        String[] args = string.split("\\s+");
        if (!args[0].equals("tp")) return false;

        if (args.length > 1) {
            String command = args[1];
            switch (command) {
                case "on":
                case "enable":
                    tpActive = true;
                    System.out.println("[ThirdPerson] Enabled");
                    return true;
                case "off":
                case "disable":
                    tpActive = false;
                    System.out.println("[ThirdPerson] Disabled");
                    return true;
                case "toggle":
                    tpActive = !tpActive;
                    System.out.printf("[ThirdPerson] %s%n", tpActive ? "Enabled" : "Disabled");
                    return true;
                case "zoom-in":
                    addZoom(-0.1f);
                    System.out.println("[ThirdPerson] Zoomed in");
                    return true;
                case "zoom-out":
                    addZoom(0.1f);
                    System.out.println("[ThirdPerson] Zoomed out");
                    return true;
                case "set-zoom":
                    if (args.length <= 2) {
                        System.out.println("[ThirdPerson] No zoom specified");
                        return true;
                    }

                    try {
                        setZoom(Float.parseFloat(args[2]));
                        System.out.printf("[ThirdPerson] Zoom set to %f%n", zoomFactor);
                    } catch (NumberFormatException e) {
                        System.out.println("[ThirdPerson] Zoom must be a number");
                    }
                    return true;
                case "get-zoom":
                    System.out.printf("[ThirdPerson] Zoom = %f%n", zoomFactor);
                    return true;
                case "set-xoffset":
                    if (args.length <= 2) {
                        System.out.println("[ThirdPerson] No offset specified");
                        return true;
                    }

                    try {
                        setXOffset(Float.parseFloat(args[2]));
                        System.out.printf("[ThirdPerson] XOffset set to %f%n", xOffset);
                    } catch (NumberFormatException e) {
                        System.out.println("[ThirdPerson] Offset must be a number");
                    }
                    return true;
                case "get-xoffset":
                    System.out.printf("[ThirdPerson] XOffset = %f%n", xOffset);
                    return true;
            }
        }

        System.out.println("[ThirdPerson] Valid commands are: enable/on, disable/off, toggle, zoom-in, zoom-out, set-zoom ZOOM, get-zoom, set-xoffset OFFSET, get-xoffset");
        return true;
    }
}
