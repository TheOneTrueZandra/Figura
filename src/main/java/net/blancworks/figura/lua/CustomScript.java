package net.blancworks.figura.lua;

import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.assets.FiguraAsset;
import net.blancworks.figura.lua.api.LuaEvent;
import net.blancworks.figura.lua.api.model.VanillaModelAPI;
import net.blancworks.figura.lua.api.model.VanillaModelPartCustomization;
import net.blancworks.figura.trust.PlayerTrustManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.JseBaseLib;
import org.luaj.vm2.lib.jse.JseMathLib;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
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

    //Keep track of these because we want to apply data to them later.
    public ArrayList<VanillaModelAPI.ModelPartTable> vanillaModelPartTables = new ArrayList<>();

    public float particleSpawnCount = 0;
    public float soundSpawnCount = 0;

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
            //Load the script source.
            LuaValue chunk = FiguraLuaManager.modGlobals.load(source, "main", scriptGlobals);
            LuaThread scriptThread = new LuaThread(scriptGlobals, chunk);

            instructionCapFunction = new ZeroArgFunction() {
                public LuaValue call() {
                    // A simple lua error may be caught by the script, but a
                    // Java Error will pass through to top and stop the script.
                    loadError = true;

                    if (data == PlayerDataManager.localPlayer)
                        sendChatMessage(new LiteralText("Script overran resource limits.").setStyle(Style.EMPTY.withColor(TextColor.parse("red"))));
                    throw new RuntimeException("Script overran resource limits.");
                }
            };

            //Queue up a new task.
            currTask = CompletableFuture.runAsync(
                    () -> {
                        try {
                            setInstructionLimitPermission(PlayerTrustManager.MAX_INIT_ID);
                            scriptThread.resume(LuaValue.NIL);
                        } catch (LuaError error) {
                            loadError = true;
                            error.printStackTrace();
                        }

                        isDone = true;
                        currTask = null;
                        FiguraMod.LOGGER.warn("Script Loading Finished");
                    }
            );
        }catch (LuaError e){
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

        if (data.lastEntity != null)
            load(data, source);
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
                if (playerData == PlayerDataManager.localPlayer) {
                    FiguraMod.LOGGER.warn(arg.toString());
                    sendChatMessage(new LiteralText(arg.toString()));
                }
                return NIL;
            }
        });

        scriptGlobals.set("logTableContent", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                LuaTable table = arg.checktable();

                logTableContents(table, 0, "");

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
        if (!isDone)
            return;
        if (tickLuaEvent == null)
            return;

        setInstructionLimitPermission(PlayerTrustManager.MAX_TICK_ID);
        try {
            tickLuaEvent.call();
        } catch (Exception error) {
            loadError = true;
            tickLuaEvent = null;
            if (error instanceof LuaError)
                logLuaError((LuaError) error);
        }
        tickInstructionCount = scriptGlobals.running.state.bytecodes;
    }

    public void onRender(float deltaTime) {
        if (!isDone)
            return;
        if (renderLuaEvent == null)
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

        if (currTask == null || currTask.isDone()) {
            currTask = CompletableFuture.runAsync(task);
        } else {
            currTask = currTask.thenRun(task);
        }

        return currTask;
    }

    public String cleanScriptSource(String s) {
        String ret = "";

        boolean commentRemoveMode = false;
        boolean blockCommentMode = false;

        //Filter out comments
        for (int i = 0; i < s.length(); i++) {
            char curr = s.charAt(i);

            if (!commentRemoveMode && !blockCommentMode) {
                if (curr == '-') {
                    if (i < s.length() - 1 && s.charAt(i + 1) == '-') {
                        commentRemoveMode = true;

                        if (i < s.length() - 3 && s.charAt(i + 2) == '[' && s.charAt(i + 3) == '[') {
                            blockCommentMode = true;

                            i += 2; //Skip those 2 characters.
                        }

                        i++; //Skip the character we detected.
                        continue;
                    }
                }
            }

            if (commentRemoveMode) {
                if (blockCommentMode) {
                    if (curr == ']') {
                        if (i < s.length() - 1 && s.charAt(i + 1) == ']') {
                            blockCommentMode = false;
                            commentRemoveMode = false;

                            i += 1; //Skip those 2 characters.
                        }

                        i++; //Skip the character we detected.
                        continue;
                    }
                }

                if (curr == '\n' && !blockCommentMode) {
                    commentRemoveMode = false;
                    continue;
                }

            } else {
                ret += s.charAt(i);
            }
        }

        ret = ret.replaceAll("[\\t\\n\\r]+", " ");
        ret = ret.replaceAll("\\s+", " ");

        return ret;
    }

    //--Debugging--

    public void logLuaError(LuaError error) {
        //Never even log errors for other players, only the local player.
        if (playerData != PlayerDataManager.localPlayer) {
            error.printStackTrace();
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
        String nextDepthString = depthString;
        sendChatMessage(new LiteralText(depthString + "{"));

        for (LuaValue key : table.keys()) {
            LuaValue value = table.get(key);

            String valString = depthString + '"' + key.toString() + '"' + " : " + value.toString();

            if (value.istable()) {
                sendChatMessage(new LiteralText(valString));
                logTableContents(value.checktable(), depth + 1, nextDepthString);
            } else {
                sendChatMessage(new LiteralText(valString + ","));
            }
        }
        sendChatMessage(new LiteralText(depthString + "},"));
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
}
