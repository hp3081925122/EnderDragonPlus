package com.enderdragonplus.client;

import com.enderdragonplus.EnderDragonPlus;
import com.enderdragonplus.debug.DebugOutput;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import jdk.jfr.Recording;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL33;

@EventBusSubscriber(modid = EnderDragonPlus.MODID, value = Dist.CLIENT)
public final class DebugProfiler {
    private static boolean active;
    private static boolean gpu;
    private static String skill;
    private static String run;
    private static Path output;
    private static Recording recording;
    private static Frame current;
    private static long previousFrame;
    private static long previousEpoch;
    private static int droppedGpu;
    private static int sampledFrames;
    private static final ArrayDeque<Frame> FREE = new ArrayDeque<>();
    private static final ArrayDeque<Frame> PENDING = new ArrayDeque<>();
    private static final List<String> FRAMES = new ArrayList<>();
    private static final List<String> EFFECTS = new ArrayList<>();
    private static final List<String> MARKERS = new ArrayList<>();
    private static final Map<Class<?>, String[]> NAMES = new IdentityHashMap<>();

    private static final class Frame {
        final int[] queries = new int[11];
        final Map<String, long[]> effects = new HashMap<>();
        String skill;
        long epoch;
        long previousEpoch;
        long started;
        long interval;
        long cpu;
        long worldCpu;
        long screenCpu;
        long submitCpu;
        long terrainCpu;
        long copyCpu;
        int vertices;
        int draws;
        int heightSamples;
        int copies;
        int instances;
        int culled;
        boolean world;
        boolean overlay;
        boolean worldCopy;
        boolean screenCopy;
        boolean inOverlay;
        boolean timed;

        Frame(boolean timed) {
            this.timed = timed;
            if (timed) {
                for (int i = 0; i < this.queries.length; i++) {
                    this.queries[i] = GL15.glGenQueries();
                }
            }
        }

        void reset() {
            this.cpu = this.worldCpu = this.screenCpu = this.submitCpu = this.terrainCpu = this.copyCpu = 0L;
            this.vertices = this.draws = this.heightSamples = this.copies = 0;
            this.instances = this.culled = 0;
            this.world = this.overlay = this.worldCopy = this.screenCopy = this.inOverlay = false;
            for (long[] values : this.effects.values()) {
                Arrays.fill(values, 0L);
            }
        }

        void stamp(int index) {
            if (this.timed) {
                GL33.glQueryCounter(this.queries[index], GL33.GL_TIMESTAMP);
            }
        }

        long span(int first, int last) {
            return GL33.glGetQueryObjectui64(this.queries[last], GL15.GL_QUERY_RESULT)
                    - GL33.glGetQueryObjectui64(this.queries[first], GL15.GL_QUERY_RESULT);
        }

        void delete() {
            if (this.timed) {
                for (int query : this.queries) {
                    GL15.glDeleteQueries(query);
                }
            }
        }
    }

    private DebugProfiler() {}

    public static void control(CompoundTag data) {
        String action = data.getString("action");
        if (action.equals("begin")) {
            start(data.getString("run"));
        } else if (active && data.getString("run").equals(run)) {
            if (action.equals("phase")) {
                skill = data.getString("skill");
                MARKERS.add(System.currentTimeMillis() + "," + skill);
            } else if (action.equals("end")) {
                finish(data.getString("skill"));
            }
        }
    }

    private static void start(String id) {
        if (active) {
            finish("replaced");
        }
        try {
            output = DebugOutput.directory(id);
            run = id;
            skill = "settle";
            previousFrame = previousEpoch = 0L;
            sampledFrames = droppedGpu = 0;
            FRAMES.clear();
            EFFECTS.clear();
            MARKERS.clear();
            NAMES.clear();
            FRAMES.add("epoch_ms,previous_frame_epoch_ms,skill,interval_ns,render_cpu_ns,vfx_cpu_ns,screen_cpu_ns,draw_submit_ns,terrain_ns,copy_cpu_ns,vertices,draws,height_samples,scene_copies,gpu_frame_ns,gpu_vfx_ns,gpu_world_copy_ns,gpu_distort_ns,gpu_world_ns,gpu_screen_ns,gpu_screen_copy_ns,instances,culled_instances");
            EFFECTS.add("epoch_ms,skill,effect,cpu_ns,calls");
            MARKERS.add("epoch_ms,skill");
            MARKERS.add(System.currentTimeMillis() + ",settle");
            gpu = GL.getCapabilities().OpenGL33 || GL.getCapabilities().GL_ARB_timer_query;
            List<String> metadata = new ArrayList<>();
            metadata.add("run=" + run);
            metadata.add("java=" + System.getProperty("java.version"));
            metadata.add("game_directory=" + Minecraft.getInstance().gameDirectory.getAbsolutePath());
            metadata.add("os=" + System.getProperty("os.name"));
            metadata.add("gpu=" + GL11.glGetString(GL11.GL_RENDERER));
            metadata.add("gpu_timestamps=" + gpu);
            metadata.add("fps_limit=" + Minecraft.getInstance().options.framerateLimit().get());
            metadata.add("vsync=" + Minecraft.getInstance().options.enableVsync().get());
            metadata.add("viewport=" + Minecraft.getInstance().getMainRenderTarget().width + "x" + Minecraft.getInstance().getMainRenderTarget().height);
            metadata.add("max_heap_bytes=" + Runtime.getRuntime().maxMemory());
            metadata.add("processors=" + Runtime.getRuntime().availableProcessors());
            metadata.add("debug_logging=" + com.enderdragonplus.EdpConfig.DEBUG_LOGGING.get());
            metadata.add("optimize_heavy_effects=" + com.enderdragonplus.EdpConfig.OPTIMIZE_HEAVY_EFFECTS.get());
            for (var mod : ModList.get().getMods()) {
                metadata.add("mod=" + mod.getModId() + " " + mod.getVersion());
            }
            Files.write(output.resolve("client-environment.txt"), metadata, StandardCharsets.UTF_8);
            try {
                recording = new Recording();
                recording.setMaxSize(128L * 1024L * 1024L);
                recording.enable("jdk.ExecutionSample").withPeriod(Duration.ofMillis(20));
                recording.enable("jdk.NativeMethodSample").withPeriod(Duration.ofMillis(20));
                recording.enable("jdk.GCPhasePause").withThreshold(Duration.ZERO);
                recording.enable("jdk.GarbageCollection");
                recording.enable("jdk.ThreadPark").withThreshold(Duration.ofMillis(10)).withStackTrace();
                recording.enable("jdk.JavaMonitorEnter").withThreshold(Duration.ofMillis(10)).withStackTrace();
                recording.enable("jdk.Compilation").withThreshold(Duration.ofMillis(20));
                recording.start();
                Files.writeString(output.resolve("client-jfr-status.txt"), "status=recording\n", StandardCharsets.UTF_8);
            } catch (Exception exception) {
                EnderDragonPlus.LOGGER.warn("EDP debug JFR is unavailable; CSV sampling remains enabled", exception);
                if (recording != null) {
                    recording.close();
                    recording = null;
                }
                Files.writeString(output.resolve("client-jfr-status.txt"), "status=unavailable\nerror=" + exception + "\n", StandardCharsets.UTF_8);
            }
            active = true;
            EnderDragonPlus.LOGGER.info("EDP client debug started run={} output={} gpu={}", run, output, gpu);
        } catch (Exception exception) {
            EnderDragonPlus.LOGGER.error("EDP client debug could not start", exception);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void renderStart(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !active) {
            return;
        }
        poll();
        Frame frame = FREE.pollFirst();
        if (frame == null) {
            frame = new Frame(gpu && PENDING.size() < 64);
            if (gpu && !frame.timed) {
                droppedGpu++;
            }
        }
        frame.reset();
        frame.skill = skill;
        frame.epoch = System.currentTimeMillis();
        frame.previousEpoch = previousEpoch;
        frame.started = System.nanoTime();
        frame.interval = previousFrame == 0 ? 0 : frame.started - previousFrame;
        previousFrame = frame.started;
        previousEpoch = frame.epoch;
        current = frame;
        frame.stamp(0);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void renderEnd(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || current == null) {
            return;
        }
        Frame frame = current;
        current = null;
        frame.stamp(1);
        frame.cpu = System.nanoTime() - frame.started;
        PENDING.add(frame);
        poll();
        if (++sampledFrames >= 500000) {
            finish("frame_limit");
        }
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && active && Minecraft.getInstance().level == null) {
            finish("disconnected");
        }
    }

    public static long begin() {
        return active && current != null ? System.nanoTime() : 0L;
    }

    public static long worldBegin() {
        if (!active || current == null) {
            return 0L;
        }
        current.world = true;
        current.stamp(2);
        return System.nanoTime();
    }

    public static void afterDistortion() {
        if (active && current != null && current.world) {
            current.stamp(5);
        }
    }

    public static void worldEnd(long started) {
        if (started != 0 && current != null) {
            current.worldCpu += System.nanoTime() - started;
            current.stamp(6);
        }
    }

    public static long screenBegin() {
        if (!active || current == null) {
            return 0L;
        }
        current.overlay = current.inOverlay = true;
        current.stamp(7);
        return System.nanoTime();
    }

    public static void screenEnd(long started) {
        if (started != 0 && current != null) {
            current.screenCpu += System.nanoTime() - started;
            current.stamp(10);
            current.inOverlay = false;
        }
    }

    public static void beforeBlit() {
        if (active && current != null) {
            if (current.inOverlay) {
                current.screenCopy = true;
                current.stamp(8);
            } else {
                current.worldCopy = true;
                current.stamp(3);
            }
        }
    }

    public static void afterBlit() {
        if (active && current != null) {
            current.stamp(current.inOverlay ? 9 : 4);
        }
    }

    public static void copyEnd(long started) {
        if (started != 0 && current != null) {
            current.copyCpu += System.nanoTime() - started;
            current.copies++;
        }
    }

    public static void submit(long started, int vertices) {
        if (started != 0 && current != null) {
            current.submitCpu += System.nanoTime() - started;
            current.vertices += vertices;
            current.draws++;
        }
    }

    public static void terrain(long started) {
        if (started != 0 && current != null) {
            current.terrainCpu += System.nanoTime() - started;
        }
    }

    public static void heightSample() {
        if (active && current != null) {
            current.heightSamples++;
        }
    }

    public static void visibility(int total, int visible) {
        if (active && current != null) {
            current.instances += total;
            current.culled += total - visible;
        }
    }

    public static void effect(VfxInstance instance, boolean distortion, long started) {
        if (started != 0 && current != null) {
            long elapsed = System.nanoTime() - started;
            String[] names = NAMES.computeIfAbsent(instance.getClass(), type -> new String[]{type.getSimpleName(), type.getSimpleName() + ".distort"});
            long[] values = current.effects.computeIfAbsent(names[distortion ? 1 : 0], key -> new long[2]);
            values[0] += elapsed;
            values[1]++;
        }
    }

    private static void poll() {
        while (!PENDING.isEmpty()) {
            Frame f = PENDING.peekFirst();
            if (f.timed && GL15.glGetQueryObjecti(f.queries[1], GL15.GL_QUERY_RESULT_AVAILABLE) == 0) {
                return;
            }
            PENDING.removeFirst();
            appendFrame(f, true);
            if (f.timed == gpu) {
                FREE.add(f);
            } else {
                f.delete();
            }
        }
    }

    private static void appendFrame(Frame f, boolean gpuReady) {
        long frame = -1, world = -1, worldCopy = -1, distortion = -1, normal = -1, screen = -1, screenCopy = -1;
        if (gpuReady && f.timed) {
            frame = f.span(0, 1);
            world = f.world ? f.span(2, 6) : 0;
            worldCopy = f.worldCopy ? f.span(3, 4) : 0;
            distortion = f.worldCopy ? f.span(4, 5) : 0;
            normal = f.world ? f.span(5, 6) : 0;
            screen = f.overlay ? f.span(7, 10) : 0;
            screenCopy = f.screenCopy ? f.span(8, 9) : 0;
        }
        FRAMES.add(f.epoch + "," + f.previousEpoch + "," + f.skill + "," + f.interval + "," + f.cpu + ","
                + f.worldCpu + "," + f.screenCpu + "," + f.submitCpu + "," + f.terrainCpu + "," + f.copyCpu + ","
                + f.vertices + "," + f.draws + "," + f.heightSamples + "," + f.copies + "," + frame + "," + world
                + "," + worldCopy + "," + distortion + "," + normal + "," + screen + "," + screenCopy + "," + f.instances + "," + f.culled);
        for (var entry : f.effects.entrySet()) {
            long[] values = entry.getValue();
            if (values[1] > 0) {
                EFFECTS.add(f.epoch + "," + f.skill + "," + entry.getKey() + "," + values[0] + "," + values[1]);
            }
        }
    }

    private static void writeSummary() throws java.io.IOException {
        Map<String, List<Long>> groups = new java.util.TreeMap<>();
        String[] headers = FRAMES.get(0).split(",");
        int[] columns = {3, 4, 5, 6, 7, 8, 9, 14, 15, 16, 17, 18, 19, 20};
        for (int i = 1; i < FRAMES.size(); i++) {
            String[] fields = FRAMES.get(i).split(",");
            for (int column : columns) {
                long value = Long.parseLong(fields[column]);
                if (value >= 0 && (column != 3 || value != 0)) {
                    groups.computeIfAbsent(fields[2] + "," + headers[column], key -> new ArrayList<>()).add(value);
                }
            }
        }
        List<String> rows = new ArrayList<>();
        rows.add("skill,metric,samples,mean_ms,p50_ms,p95_ms,max_ms");
        for (var entry : groups.entrySet()) {
            long[] values = entry.getValue().stream().mapToLong(Long::longValue).sorted().toArray();
            double sum = 0.0;
            for (long value : values) {
                sum += value;
            }
            rows.add(String.format(java.util.Locale.ROOT, "%s,%d,%.6f,%.6f,%.6f,%.6f", entry.getKey(), values.length,
                    sum / values.length / 1.0E6, values[values.length / 2] / 1.0E6,
                    values[(int) ((values.length - 1) * 0.95)] / 1.0E6, values[values.length - 1] / 1.0E6));
        }
        Files.write(output.resolve("client-summary.csv"), rows, StandardCharsets.UTF_8);
    }

    private static void finish(String reason) {
        if (!active) {
            return;
        }
        active = false;
        try {
            if (recording != null) {
                try {
                    recording.stop();
                } catch (IllegalStateException exception) {
                    EnderDragonPlus.LOGGER.warn("EDP debug recording was already stopped", exception);
                }
            }
            poll();
            for (Frame pending : PENDING) {
                appendFrame(pending, false);
            }
            MARKERS.add(System.currentTimeMillis() + ",finish_" + reason);
            Files.write(output.resolve("client-frames.csv"), FRAMES, StandardCharsets.UTF_8);
            Files.write(output.resolve("client-effects.csv"), EFFECTS, StandardCharsets.UTF_8);
            Files.write(output.resolve("client-phases.csv"), MARKERS, StandardCharsets.UTF_8);
            writeSummary();
            Files.writeString(output.resolve("client-result.txt"), "reason=" + reason + "\nframes=" + sampledFrames
                    + "\ngpu_backlog_drops=" + droppedGpu + "\npending_at_finish=" + PENDING.size()
                    + "\nFrame interval belongs to the previous frame; use previous_frame_epoch_ms for correlation.\nGPU timestamps are asynchronous spans, not exclusive shader busy time.\n", StandardCharsets.UTF_8);
            if (recording != null) {
                try {
                    recording.dump(output.resolve("client.jfr"));
                    Files.writeString(output.resolve("client-jfr-status.txt"), "status=saved\nbytes=" + Files.size(output.resolve("client.jfr")) + "\n", StandardCharsets.UTF_8);
                } catch (Exception exception) {
                    EnderDragonPlus.LOGGER.error("EDP debug JFR could not be saved; CSV results remain available", exception);
                    Files.writeString(output.resolve("client-jfr-status.txt"), "status=save_failed\nerror=" + exception + "\n", StandardCharsets.UTF_8);
                }
            }
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.sendSystemMessage(Component.translatable("enderdragonplus.debug.client_saved", output.toString()));
            }
            EnderDragonPlus.LOGGER.info("EDP client debug saved run={} reason={} frames={} output={}", run, reason, sampledFrames, output);
        } catch (Exception exception) {
            EnderDragonPlus.LOGGER.error("EDP client debug could not save results", exception);
        } finally {
            if (recording != null) {
                recording.close();
                recording = null;
            }
            if (current != null) {
                current.delete();
                current = null;
            }
            for (Frame f : PENDING) {
                f.delete();
            }
            for (Frame f : FREE) {
                f.delete();
            }
            PENDING.clear();
            FREE.clear();
            FRAMES.clear();
            EFFECTS.clear();
            NAMES.clear();
        }
    }
}
