package net.blancworks.figura.lua;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import net.blancworks.figura.*;
import net.blancworks.figura.assets.FiguraAsset;
import net.blancworks.figura.lua.api.LuaEvent;
import net.blancworks.figura.lua.api.camera.CameraCustomization;
import net.blancworks.figura.lua.api.actionWheel.ActionWheelCustomization;
import net.blancworks.figura.lua.api.nameplate.NamePlateCustomization;
import net.blancworks.figura.lua.api.model.VanillaModelAPI;
import net.blancworks.figura.lua.api.model.VanillaModelPartCustomization;
import net.blancworks.figura.network.NewFiguraNetworkManager;
import net.blancworks.figura.trust.PlayerTrustManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.JseBaseLib;
import org.luaj.vm2.lib.jse.JseMathLib;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class CustomScript extends FiguraAsset {

    public PlayerData playerData;
    public String source;
    public boolean loadError = false;

    //Global script values
    public Globals scriptGlobals = new Globals();
    //setHook, used for setting instruction count/other debug stuff
    public LuaValue setHook;
    //This is what's called when the instruction cap is hit.
    public LuaValue instructionCapFunction;


    //The currently running task.
    //Updated as things are added to it.
    public CompletableFuture currTask;


    //How many instructions the last tick/render event used.
    public int tickInstructionCount = 0;
    public int renderInstructionCount = 0;

    //References to the tick and render functions for easy use elsewhere.
    private LuaEvent tickLuaEvent = null;
    private LuaEvent renderLuaEvent = null;

    private CompletableFuture lastTickFunction = null;

    public Map<String, LuaEvent> allEvents = new HashMap<>();

    //Vanilla model part customizations made via this script
    public Map<String, VanillaModelPartCustomization> allCustomizations = new HashMap<>();

    //Nameplate customizations
    public Map<String, NamePlateCustomization> nameplateCustomizations = new HashMap<>();

    //Camera customizations
    public Map<String, CameraCustomization> cameraCustomizations = new HashMap<>();

    //Action Wheel customizations
    public Map<String, ActionWheelCustomization> actionWheelCustomizations = new HashMap<>();
    public int actionWheelLeftSize = 4;
    public int actionWheelRightSize = 4;

    //scripting custom keybinds
    public ArrayList<KeyBinding> keyBindings = new ArrayList<>();

    //Keep track of these because we want to apply data to them later.
    public ArrayList<VanillaModelAPI.ModelPartTable> vanillaModelPartTables = new ArrayList<>();

    public float particleSpawnCount = 0;
    public float soundSpawnCount = 0;

    public Float customShadowSize = null;

    public boolean hasPlayer = false;

    //TODO maybe remove this out from scripting
    public DamageSource lastDamageSource;

    //----PINGS!----

    //Maps functions from lua to shorts for data saving.
    public BiMap<Short, String> functionIDMap = HashBiMap.create();

    private short lastPingID = Short.MIN_VALUE;

    public Queue<LuaPing> incomingPingQueue = new LinkedList<>();

    public Queue<LuaPing> outgoingPingQueue = new LinkedList<>();

    public CustomScript() {
        source = "";
    }

    public CustomScript(PlayerData data, String content) {
        load(data, content);
    }


    //--Setup--
    //Loads the script using the targeted playerData and source code.
    public void load(PlayerData data, String src) {
        //Set the player data so we have something to target.
        playerData = data;

        //Loads the source into this string variable for later use.
        source = src;

        //Load up the default libraries we wanna include.0
        scriptGlobals.load(new JseBaseLib());
        scriptGlobals.load(new PackageLib());
        scriptGlobals.load(new Bit32Lib());
        scriptGlobals.load(new TableLib());
        scriptGlobals.load(new StringLib());
        scriptGlobals.load(new JseMathLib());

        //Set up debug in this environment, but never allow any users to access it.
        scriptGlobals.load(new DebugLib());
        //Yoink sethook from debug so we can use it later.
        setHook = scriptGlobals.get("debug").get("sethook");
        //Yeet debug library so nobody can access it.
        scriptGlobals.set("debug", LuaValue.NIL);

        //Sets up events!
        setupEvents();
        //Sets up the global values for the API and such in the script.
        setupGlobals();

        try {
            //Load the script source, name defaults to "main" for scripts for other players.
            String scriptName = (data == PlayerDataManager.localPlayer && (PlayerDataManager.localPlayer != null && PlayerDataManager.localPlayer.loadedName != null))
                    ? PlayerDataManager.localPlayer.loadedName
                    : "main";
            LuaValue chunk = FiguraLuaManager.modGlobals.load(source, scriptName, scriptGlobals);

            instructionCapFunction = new ZeroArgFunction() {
                public LuaValue call() {
                    // A simple lua error may be caught by the script, but a
                    // Java Error will pass through to top and stop the script.
                    loadError = true;

                    if (data == PlayerDataManager.localPlayer || (boolean) Config.entries.get("logOthers").value) {
                        sendChatMessage(new LiteralText("[lua] ").formatted(Formatting.BLUE, Formatting.ITALIC)
                                .append((data.playerName.copy()).setStyle(Style.EMPTY).formatted(Formatting.DARK_RED, Formatting.BOLD)
                                        .append(new LiteralText(" > Script overran resource limits"))
                                )
                        );
                    }
                    throw new RuntimeException("Script overran resource limits");
                }
            };

            //Queue up a new task.
            currTask = CompletableFuture.runAsync(
                    () -> {
                        try {
                            setInstructionLimitPermission(PlayerTrustManager.MAX_INIT_ID);
                            data.lastEntity = null;
                            chunk.call();
                        } catch (Exception error) {
                            loadError = true;
                            if (error instanceof LuaError)
                                logLuaError((LuaError) error);
                            else
                                error.printStackTrace();
                        }

                        isDone = true;
                        currTask = null;
                        FiguraMod.LOGGER.info("Script Loading Finished");
                    }
            );
        } catch (LuaError e) {
            logLuaError(e);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void toNBT(CompoundTag tag) {
        tag.putString("src", cleanScriptSource(source));
    }

    public void fromNBT(PlayerData data, CompoundTag tag) {
        source = tag.getString("src");

        load(data, source);
    }

    public void setPlayerEntity(PlayerEntity ent) {
        if(!isDone)
            return;
        
        if (!hasPlayer) {
            hasPlayer = true;
            queueTask(() -> {
                try {
                    allEvents.get("player_init").call();
                } catch(Exception error){
                    error.printStackTrace();
                }
            });
        }
    }


    //Sets up and creates all the LuaEvents for this script
    public void setupEvents() {
        //Foreach event
        for (Map.Entry<String, Function<String, LuaEvent>> entry : FiguraLuaManager.registeredEvents.entrySet()) {
            //Add a new event created from the name here
            allEvents.put(entry.getKey(), entry.getValue().apply(entry.getKey()));
        }

        tickLuaEvent = allEvents.get("tick");
        renderLuaEvent = allEvents.get("render");
    }

    //Sets up global variables
    public void setupGlobals() {
        //Log! Only for local player.
        scriptGlobals.set("log", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                try {
                    if (playerData == PlayerDataManager.localPlayer || (boolean) Config.entries.get("logOthers").value) {
                        int config = (int) Config.entries.get("scriptLog").value;
                        if (config != 2) {
                            FiguraMod.LOGGER.info("[lua] " + playerData.playerName.copy().getString() + " > " + arg.toString());
                        }
                        if (config != 1) {
                            sendChatMessage(new LiteralText("[lua] ").formatted(Formatting.BLUE, Formatting.ITALIC)
                                    .append((playerData.playerName.copy()).setStyle(Style.EMPTY).formatted(Formatting.WHITE)
                                            .append(new LiteralText(" > " + arg.toString()))
                                    )
                            );
                        }
                    }
                } catch (Exception e){
                    e.printStackTrace();
                }
                return NIL;
            }
        });

        //Re-map print to log.
        scriptGlobals.set("print", scriptGlobals.get("log"));

        scriptGlobals.set("logTableContent", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                try {
                    LuaTable table = arg.checktable();

                    if (playerData == PlayerDataManager.localPlayer || (boolean) Config.entries.get("logOthers").value) {
                        int config = (int) Config.entries.get("scriptLog").value;
                        if (config != 2) {
                            FiguraMod.LOGGER.info("[lua] " + playerData.playerName.copy().getString() + " >");
                        }
                        if (config != 1) {
                            sendChatMessage(new LiteralText("[lua] ").formatted(Formatting.BLUE, Formatting.ITALIC)
                                    .append((playerData.playerName.copy()).setStyle(Style.EMPTY).formatted(Formatting.WHITE)
                                            .append(new LiteralText(" >"))
                                    )
                            );
                        }
    
                        logTableContents(table, 1, "");
                    }
                } catch (Throwable e){
                    e.printStackTrace();
                }

                return NIL;
            }
        });

        LuaTable globalMetaTable = new LuaTable();

        //When creating a new variable.
        globalMetaTable.set("__newindex", new ThreeArgFunction() {
            @Override
            public LuaValue call(LuaValue table, LuaValue key, LuaValue value) {
                if (table != scriptGlobals) {
                    loadError = true;
                    error("Can't use global table metatable on other tables!");
                }

                if (value.isfunction() && key.isstring()) {
                    String funcName = key.checkjstring();
                    LuaFunction func = value.checkfunction();

                    LuaEvent possibleEvent = allEvents.get(funcName);

                    if (possibleEvent != null) {
                        possibleEvent.subscribe(func);
                        return NIL;
                    }
                }
                table.rawset(key, value);

                return NIL;
            }
        });

        scriptGlobals.setmetatable(globalMetaTable);

        FiguraLuaManager.setupScriptAPI(this);
    }

    //--Instruction Limits--

    //Sets the instruction limit of the next function we'll call, and resets the bytecode count to 0
    //Uses the permission at permissionID to set it.
    public void setInstructionLimitPermission(Identifier permissionID) {
        int count = playerData.getTrustContainer().getIntSetting(permissionID);
        setInstructionLimit(count);
    }

    //Sets the instruction limit of the next function we'll call, and resets the bytecode count to 0.
    public void setInstructionLimit(int count) {
        scriptGlobals.running.state.bytecodes = 0;
        setHook.invoke(
                LuaValue.varargsOf(
                        new LuaValue[]{
                                instructionCapFunction,
                                LuaValue.EMPTYSTRING, LuaValue.valueOf(count)
                        }
                )
        );
    }


    //--Events--

    //Called whenever the global tick event happens
    public void tick() {
        if (particleSpawnCount > 0)
            particleSpawnCount = MathHelper.clamp(particleSpawnCount - ((1 / 20f) * playerData.getTrustContainer().getIntSetting(PlayerTrustManager.MAX_PARTICLES_ID)), 0, 999);
        if (soundSpawnCount > 0)
            soundSpawnCount = MathHelper.clamp(soundSpawnCount - ((1 / 20f) * playerData.getTrustContainer().getIntSetting(PlayerTrustManager.MAX_SOUND_EFFECTS_ID)), 0, 999);

        //If the tick function exists, call it.
        if (tickLuaEvent != null) {
            if (lastTickFunction != null && !lastTickFunction.isDone())
                return;
            lastTickFunction = queueTask(this::onTick);
        }
    }

    //Called whenever the game renders a new frame with this avatar in view
    public void render(float deltaTime) {
        //Don't render if the script is doing something else still
        //Prevents threading memory errors and also ensures that "long" ticks and events and such are penalized.
        if (tickLuaEvent == null || currTask == null || !currTask.isDone())
            return;

        onRender(deltaTime);
    }

    public void onTick() {
        if (!isDone || tickLuaEvent == null || !hasPlayer || playerData.lastEntity == null)
            return;

        setInstructionLimitPermission(PlayerTrustManager.MAX_TICK_ID);
        try {
            tickLuaEvent.call();

            //Process all pings.
            while (incomingPingQueue.size() > 0) {
                LuaPing p = incomingPingQueue.poll();

                p.function.call(p.args);
            }

            //Batch-send pings.
            if (outgoingPingQueue.size() > 0)
                ((NewFiguraNetworkManager) FiguraMod.networkManager).sendPing(outgoingPingQueue);
        } catch (Exception error) {
            loadError = true;
            tickLuaEvent = null;
            if (error instanceof LuaError)
                logLuaError((LuaError) error);
        }
        tickInstructionCount = scriptGlobals.running.state.bytecodes;
    }

    public void onRender(float deltaTime) {
        if (!isDone || renderLuaEvent == null || !hasPlayer || playerData.lastEntity == null)
            return;

        setInstructionLimitPermission(PlayerTrustManager.MAX_RENDER_ID);
        try {
            renderLuaEvent.call(LuaNumber.valueOf(deltaTime));
        } catch (Exception error) {
            loadError = true;
            renderLuaEvent = null;
            if (error instanceof LuaError)
                logLuaError((LuaError) error);
        }
        renderInstructionCount = scriptGlobals.running.state.bytecodes;
    }

    //--Tasks--

    public CompletableFuture queueTask(Runnable task) {
        synchronized (this) {
            if (currTask == null || currTask.isDone()) {
                currTask = CompletableFuture.runAsync(task);
            } else {
                currTask = currTask.thenRun(task);
            }

            return currTask;
        }
    }

    public String cleanScriptSource(String s) {
        if (!(boolean) Config.entries.get("formatScript").value)
            return s;

        StringBuilder ret = new StringBuilder();

        boolean inString = false;
        boolean inChar = false;
        boolean inBlockString = false;
        boolean inComment = false;
        boolean inBlock = false;

        StringBuilder queue = new StringBuilder();

        for (int i = 0; i < s.length(); i++) {
            char curr = s.charAt(i);

            if (!inString && !inChar && !inBlockString && !inComment && !inBlock) {
                //check for string
                if (curr == '"') {
                    inString = true;
                    queue.append(curr);
                }
                //check for char (lua allows strings surrounded with '')
                else if (curr == '\'') {
                    inChar = true;
                    queue.append(curr);
                }
                //check for block strings
                else if (curr == '[' && i < s.length() - 1 && s.charAt(i + 1) == '[') {
                    inBlockString = true;
                    queue.append(curr);
                }
                //check single line comments
                else if (curr == '-' && i < s.length() - 1 && s.charAt(i + 1) == '-') {
                    inComment = true;
                    i++;

                    //check for comment block
                    if (i < s.length() - 2 && s.charAt(i + 1) == '[' && s.charAt(i + 2) == '[') {
                        inBlock = true;
                        i += 2;
                    }
                }
                else {
                    queue.append(curr);
                }

                //dont continue on the last iteration
                if (i < s.length() - 1)
                    continue;
            }

            //format then append queue
            queue = new StringBuilder(queue.toString().replaceAll("[\\t\\n\\r]+", " "));
            queue = new StringBuilder(queue.toString().replaceAll("\\s+", " "));

            ret.append(queue);
            queue = new StringBuilder();

            //add string contents
            if (inString) {
                //check for end of string and append
                inString = !(curr == '"' && s.charAt(i - 1) != '\\');
                ret.append(curr);
            }
            //add char contents
            else if (inChar) {
                //check for end of char and append
                inChar = !(curr == '\'' && s.charAt(i - 1) != '\\');
                ret.append(curr);
            }
            //add block string contents
            else if (inBlockString) {
                //check for end of block and append
                inBlockString = !(curr == ']' && i < s.length() - 1 && s.charAt(i + 1) == ']');
                ret.append(curr);
            }
            //skip block comments
            else if (inBlock) {
                //check for end of block
                inBlock = !(curr == ']' && i < s.length() - 1 && s.charAt(i + 1) == ']');

                //if block ended
                if (!inBlock) {
                    queue.append(" ");
                    inComment = false;
                    i++;
                }
            }
            //skip comments
            else if (curr == '\n') {
                queue.append(" ");
                inComment = false;
            }
        }

        return ret.toString();
    }

    //--Debugging--

    public void logLuaError(LuaError error) {
        //Never even log errors for other players, only the local player.
        if (playerData != PlayerDataManager.localPlayer) {
            return;
        }

        loadError = true;
        String msg = error.getMessage();
        msg = msg.replace("\t", "   ");
        String[] messageParts = msg.split("\n");

        for (String part : messageParts) {
            sendChatMessage(new LiteralText(part).setStyle(Style.EMPTY.withColor(TextColor.parse("red"))));
        }

        error.printStackTrace();
    }

    public void logTableContents(LuaTable table, int depth, String depthString) {
        String spacing = "  ";
        depthString = spacing.substring(2) + depthString;

        int config = (int) Config.entries.get("scriptLog").value;
        if (config != 2) {
            FiguraMod.LOGGER.info(depthString + "{");
        }
        if (config != 1) {
            sendChatMessage(new LiteralText(depthString + "{").formatted(Formatting.ITALIC));
        }

        for (LuaValue key : table.keys()) {
            LuaValue value = table.get(key);

            String valString = spacing + depthString + '"' + key.toString() + '"' + " : " + value.toString();

            if (value.istable()) {
                if (config != 2) {
                    FiguraMod.LOGGER.info(valString);
                }
                if (config != 1) {
                    sendChatMessage(new LiteralText(valString).formatted(Formatting.ITALIC));
                }

                logTableContents(value.checktable(), depth + 1, spacing + depthString);
            } else {
                if (config != 2) {
                    FiguraMod.LOGGER.info(valString + ",");
                }
                if (config != 1) {
                    sendChatMessage(new LiteralText(valString + ",").formatted(Formatting.ITALIC));
                }
            }
        }
        if (config != 2) {
            FiguraMod.LOGGER.info(depthString + "},");
        }
        if (config != 1) {
            sendChatMessage(new LiteralText(depthString + "},").formatted(Formatting.ITALIC));
        }
    }

    public static void sendChatMessage(Text text) {
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(text);
    }

    //--Vanilla Modifications--

    public VanillaModelPartCustomization getOrMakePartCustomization(String accessor) {
        VanillaModelPartCustomization currCustomization = getPartCustomization(accessor);

        if (currCustomization == null) {
            currCustomization = new VanillaModelPartCustomization();
            allCustomizations.put(accessor, currCustomization);
        }
        return currCustomization;
    }

    public VanillaModelPartCustomization getPartCustomization(String accessor) {
        return allCustomizations.get(accessor);
    }

    //--Nameplate Modifications--

    public NamePlateCustomization getOrMakeNameplateCustomization(String accessor) {
        NamePlateCustomization currCustomization = getNameplateCustomization(accessor);

        if (currCustomization == null) {
            currCustomization = new NamePlateCustomization();
            nameplateCustomizations.put(accessor, currCustomization);
        }
        return currCustomization;
    }

    public NamePlateCustomization getNameplateCustomization(String accessor) {
        return nameplateCustomizations.get(accessor);
    }

    //--Camera Modifications--

    public CameraCustomization getOrMakeCameraCustomization(String accessor) {
        CameraCustomization currCustomization = getCameraCustomization(accessor);

        if (currCustomization == null) {
            currCustomization = new CameraCustomization();
            cameraCustomizations.put(accessor, currCustomization);
        }
        return currCustomization;
    }

    public CameraCustomization getCameraCustomization(String accessor) {
        return cameraCustomizations.get(accessor);
    }

    //--ActionWheel Modifications--

    public ActionWheelCustomization getOrMakeActionWheelCustomization(String accessor) {
        ActionWheelCustomization currCustomization = getActionWheelCustomization(accessor);

        if (currCustomization == null) {
            currCustomization = new ActionWheelCustomization();
            actionWheelCustomizations.put(accessor, currCustomization);
        }
        return currCustomization;
    }

    public ActionWheelCustomization getActionWheelCustomization(String accessor) {
        return actionWheelCustomizations.get(accessor);
    }

    //--Pings--
    public void registerPingName(String s) {
        functionIDMap.put(lastPingID++, s);
    }

    public void handlePing(short id, LuaValue args) {
        try {
            String functionName = functionIDMap.get(id);

            LuaPing p = new LuaPing();
            p.function = scriptGlobals.get(functionName).checkfunction();
            p.args = args;
            p.functionID = id;

            incomingPingQueue.add(p);
        } catch (Exception error) {
            loadError = true;
            if (error instanceof LuaError)
                logLuaError((LuaError) error);
        }
    }

    public static class LuaPing {
        public short functionID;
        public LuaFunction function;
        public LuaValue args;
    }
}
