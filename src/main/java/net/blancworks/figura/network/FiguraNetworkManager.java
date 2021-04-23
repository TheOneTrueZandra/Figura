package net.blancworks.figura.network;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.blancworks.figura.Config;
import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.PlayerDataManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientLoginNetworkHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkState;
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket;
import net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import org.apache.logging.log4j.Level;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;


/**
 * Used to manage the network operations for Figura.
 * Used for sending/receiving data, managing custom packets/networking, that sort.
 */
public class FiguraNetworkManager implements IFiguraNetwork {

    //This is the key for the session the user has with figura.
    //DO NOT LET PLAYERS ACCESS THIS!!!!!!
    //I mean, you could, but it just runs too many risks of them giving their key to someone who shouldn't have it.
    public int figuraSessionKey;

    //False until the auth key has been acquired. also false if auth key becomes invalid.
    private boolean hasAuthKey = false;
    private Date lastAuthDate = null;

    public CompletableFuture currentAuthTask = null;

    //Ticks the network
    //Checks after 20 minutes, attempt to get a fresh key with our previously valid one.
    //Repeat re-grab for fresh key 10 times, or until server returns unauthorized 
    @Override
    public void tickNetwork() {
        if (lastAuthDate != null) {
            Date now = new Date();
            long diff = now.getTime() - lastAuthDate.getTime();

            //If diff (time in milliseconds) > 20 minutes
            if (diff > 1000 * 60 * 20) {
                //Set the last time we checked for an auth to be 19 minutes ago
                //This means in 1 minute, we'll check again.
                lastAuthDate = new Date(now.getTime() - 1000 * 60 * 19);

                //Attempt to refresh key.
                refreshKeyValidity(() -> {
                });
            }
        }
    }

    @Override
    public CompletableFuture<CompoundTag> getAvatarData(UUID id) {
        return CompletableFuture.supplyAsync(()-> {
            HttpURLConnection httpURLConnection = null;

            
            try {
                //Object to fill data with
                JsonObject targetObject = null;

                //Url for this asset file.
                URL url = new URL(String.format("%s/api/avatar/%s", getServerURL(), id));

                //Open connection and set things up.
                httpURLConnection = (HttpURLConnection) (url).openConnection(MinecraftClient.getInstance().getNetworkProxy());
                httpURLConnection.setDoInput(true);
                httpURLConnection.setDoOutput(false);
                httpURLConnection.connect();

                //If response code is OK, we know we got a valid avatar, so load it.
                if (httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    //Put JSON into string
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(httpURLConnection.getInputStream()));
                    String inputLine;
                    StringBuilder content = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        content.append(inputLine);
                    }
                    in.close();
                    //Parse JSON
                    JsonParser parser = new JsonParser();
                    targetObject = parser.parse(content.toString()).getAsJsonObject();
                }

                httpURLConnection.disconnect();


                //Attempt to load data from the JSON we just got from the server
                if (targetObject != null) {
                    String dataString = targetObject.get("data").getAsString();
                    if (dataString.length() != 0) {
                        byte[] dataAsBytes = Base64.getDecoder().decode(dataString);
                        InputStream dataAsStream = new ByteArrayInputStream(dataAsBytes);
                        DataInputStream receivedDataToStream = new DataInputStream(dataAsStream);
                        receivedDataToStream.reset();
                        CompoundTag nbt = NbtIo.readCompressed(receivedDataToStream);
                        
                        return nbt;
                    }
                }
            } catch (Exception e){
                e.printStackTrace();
            }

            httpURLConnection.disconnect();
            return null;
        });
    }

    @Override
    public CompletableFuture<UUID> postAvatar() {
        return CompletableFuture.supplyAsync(()->{
            postModel();
            return PlayerDataManager.localPlayer.playerId; 
        });
    }

    //Does nothing for this.
    @Override
    public CompletableFuture setAvatarCurr(UUID avatarID) {
        return CompletableFuture.runAsync(() -> {
        });
    }

    @Override
    public CompletableFuture deleteAvatar() {
        return CompletableFuture.runAsync(this::deleteModel);
    }

    @Override
    public CompletableFuture<String> asyncGetAvatarHash(UUID avatarID) {
        return asyncGetAvatarHash(avatarID);
    }

    @Override
    public void parseKickAuthMessage(Text reason) {
        parseAuthKeyFromDisconnectMessage(reason);
    }


    //--AUTH--

    //Spawns async method for authenticating a user. 
    public CompletableFuture authUser() {
        if (currentAuthTask != null)
            return currentAuthTask;

        return CompletableFuture.runAsync(
                this::asyncAuthUser,
                Util.getMainWorkerExecutor()
        );
    }

    //Asynchronously authenticates the user using the Figura server.
    private void asyncAuthUser() {
        try {
            String address = getMinecraftAuthServerAddress();
            InetAddress inetAddress = InetAddress.getByName(address);
            ClientConnection connection = ClientConnection.connect(inetAddress, 25565, true);
            connection.setPacketListener(new ClientLoginNetworkHandler(connection, MinecraftClient.getInstance(), null, (text) -> {
                FiguraMod.LOGGER.log(Level.ERROR, text.toString());
            }));
            connection.send(new HandshakeC2SPacket(address, 25565, NetworkState.LOGIN));
            connection.send(new LoginHelloC2SPacket(MinecraftClient.getInstance().getSession().getProfile()));

            while (connection.isOpen())
                Thread.sleep(1);

            Text dcReason = connection.getDisconnectReason();

            if (dcReason instanceof Text) {
                Text tc = dcReason;
                parseAuthKeyFromDisconnectMessage(tc);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //Auth all done :D
        currentAuthTask = null;
    }

    public void parseAuthKeyFromDisconnectMessage(Text reason) {
        try {
            if (reason.asString().equals("This is the Figura Auth Server!\n")) {

                Text keyText = reason.getSiblings().get(1);
                figuraSessionKey = Integer.parseInt(keyText.asString());
                hasAuthKey = true;
                lastAuthDate = new Date();

                LiteralText garbleText = new LiteralText("-------------------------\n\n\n");
                garbleText.setStyle(Style.EMPTY.withFormatting(Formatting.OBFUSCATED));

                reason.getSiblings().set(1, garbleText);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean hasAuthKey() {
        return hasAuthKey;
    }

    //Attempts to refresh a key using the figura auth server.
    public CompletableFuture refreshKeyValidity(Runnable onFinished) {
        return CompletableFuture.runAsync(
                () -> {
                    //Don't do anything if we know we have no valid key already.
                    if (!hasAuthKey) {
                        if (onFinished != null)
                            onFinished.run();
                        return;
                    }
                    try {
                        String uuidString = MinecraftClient.getInstance().player.getUuid().toString();
                        String url = String.format("%s/api/auth/refresh?key=%d&id=%s", getServerURL(), figuraSessionKey, uuidString);
                        HttpURLConnection connection = createReadConnection(url);
                        connection.connect();
                        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                            String getResult = readStringFromStream(connection.getInputStream());

                            int result = Integer.parseInt(getResult);
                            hasAuthKey = true;
                            lastAuthDate = new Date();
                            figuraSessionKey = result;
                        } else {
                            hasAuthKey = false;
                        }
                        connection.disconnect();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (onFinished != null)
                        onFinished.run();
                }, Util.getMainWorkerExecutor()
        );
    }

    public CompletableFuture<Boolean> isKeyValid(Runnable onFinished) {
        return CompletableFuture.supplyAsync(() -> {
                    try {
                        String uuidString = MinecraftClient.getInstance().player.getUuid().toString();
                        String url = String.format("%s/api/auth/check?key=%d&id=%s", getServerURL(), figuraSessionKey, uuidString);
                        HttpURLConnection connection = createReadConnection(url);
                        connection.connect();
                        connection.disconnect();
                        return connection.getResponseCode() == HttpURLConnection.HTTP_OK;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return false;
                }, Util.getMainWorkerExecutor()
        );
    }

    //--AVATAR STATUS--

    public void postModel() {
        String uuidString = MinecraftClient.getInstance().player.getUuid().toString();

        try {
            CompletableFuture.runAsync(() -> {
                HttpURLConnection httpURLConnection;

                refreshKeyValidity(null).join();

                //If not authed, auth.
                if (!hasAuthKey) {
                    CompletableFuture future = authUser();
                    future.join();
                }

                try {
                    URL url = new URL(String.format("%s/api/avatar/%s?key=%d", getServerURL(), uuidString, figuraSessionKey));
                    PlayerData data = PlayerDataManager.localPlayer;

                    CompoundTag infoNbt = new CompoundTag();
                    data.writeNbt(infoNbt);


                    httpURLConnection = (HttpURLConnection) url.openConnection();
                    httpURLConnection.setRequestMethod("PUT");
                    httpURLConnection.setRequestProperty("Content-Type", "application/octet-stream");

                    httpURLConnection.setDoOutput(true);
                    httpURLConnection.setDoInput(true);


                    OutputStream outStream = httpURLConnection.getOutputStream();
                    DataOutputStream nbtDataStream = new DataOutputStream(outStream);

                    NbtIo.writeCompressed(infoNbt, nbtDataStream);

                    outStream.close();

                    FiguraMod.LOGGER.log(Level.DEBUG, httpURLConnection.getResponseMessage());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, Util.getMainWorkerExecutor());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Deletes the player model off of the server.
    public void deleteModel() {
        String uuidString = MinecraftClient.getInstance().player.getUuid().toString();

        try {
            CompletableFuture.runAsync(() -> {
                HttpURLConnection httpURLConnection;

                refreshKeyValidity(null).join();

                //If not authed, auth.
                if (!hasAuthKey) {
                    CompletableFuture future = authUser();
                    future.join();
                }

                try {
                    URL url = new URL(String.format("%s/api/avatar/%s?key=%d", getServerURL(), uuidString, figuraSessionKey));
                    PlayerData data = PlayerDataManager.localPlayer;

                    CompoundTag infoNbt = new CompoundTag();
                    data.writeNbt(infoNbt);


                    httpURLConnection = (HttpURLConnection) url.openConnection();
                    httpURLConnection.setRequestMethod("DELETE");
                    httpURLConnection.setRequestProperty("Content-Type", "application/json");

                    httpURLConnection.connect();
                    httpURLConnection.disconnect();

                    PlayerDataManager.clearLocalPlayer();

                    FiguraMod.LOGGER.log(Level.DEBUG, httpURLConnection.getResponseMessage());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, Util.getMainWorkerExecutor());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public CompletableFuture<String> getAvatarHash(UUID uuid) {
        return CompletableFuture.supplyAsync(
                () -> getAvatarHashSync(uuid), Util.getMainWorkerExecutor()
        );
    }

    public String getAvatarHashSync(UUID id) {

        try {
            String uuidString = id.toString();
            HttpURLConnection httpURLConnection = createReadConnection(String.format("%s/api/avatar/hash/%s", getServerURL(), uuidString));
            httpURLConnection.connect();

            //Only continue if response was OK.
            if (httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                return readStringFromStream(httpURLConnection.getInputStream());
            }
            return "";
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }


    //Player data stuff
    //Right now just gets avatar, but will eventually get other stuff like report data.

    public CompletableFuture<PlayerData> getPlayerData(UUID uuid) {
        return null;
    }

    private PlayerData getData(UUID id) {

        return null;
    }


    //--UTILITY FUNCTIONS--


    //localhost for local testing
    //figura.blancworks.org for proper online use.
    //TODO - Add support for a server list later for people who want to have their own avatar servers
    private String getServerAddress() {
        if (Config.useLocalServer.value) {
            return "localhost:5001";
        }
        return "figura.blancworks.org";
    }

    private String getMinecraftAuthServerAddress() {
        if (Config.useLocalServer.value) {
            return "localhost";
        }
        return "mc.blancworks.org";
    }

    //This is set to 
    public String getServerURL() {
        return String.format("http://%s", getServerAddress());
    }

    public String readStringFromStream(InputStream stream) throws IOException {
        //Put JSON into string
        BufferedReader in = new BufferedReader(
                new InputStreamReader(stream));
        String inputLine;
        StringBuffer content = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();

        return content.toString();
    }

    public HttpURLConnection createConnection(String destination) throws Exception {
        URL dst = new URL(destination);
        return (HttpURLConnection) dst.openConnection(MinecraftClient.getInstance().getNetworkProxy());
    }

    public HttpURLConnection createReadConnection(String destination) throws Exception {
        HttpURLConnection c = createConnection(destination);
        c.setDoInput(true);
        c.setDoOutput(false);
        return c;
    }
}
