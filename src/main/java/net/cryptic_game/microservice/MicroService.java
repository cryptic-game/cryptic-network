package net.cryptic_game.microservice;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import net.cryptic_game.microservice.config.Config;
import net.cryptic_game.microservice.config.DefaultConfig;
import net.cryptic_game.microservice.endpoint.MicroServiceEndpoint;
import net.cryptic_game.microservice.endpoint.UserEndpoint;
import net.cryptic_game.microservice.sql.SqlService;
import net.cryptic_game.microservice.utils.JSON;
import net.cryptic_game.microservice.utils.JSONBuilder;
import net.cryptic_game.microservice.utils.Tuple;
import net.cryptic_game.microservice.wrapper.User;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.reflections.Reflections;
import org.reflections.ReflectionsException;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Entity;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static net.cryptic_game.microservice.error.ServerError.INTERNAL_ERROR;
import static net.cryptic_game.microservice.error.ServerError.MISSING_PARAMETERS;
import static net.cryptic_game.microservice.error.ServerError.UNKNOWN_SERVICE;
import static net.cryptic_game.microservice.error.ServerError.UNSUPPORTED_FORMAT;
import static net.cryptic_game.microservice.utils.JSONUtils.checkData;
import static net.cryptic_game.microservice.utils.SocketUtils.send;
import static net.cryptic_game.microservice.utils.SocketUtils.sendError;

@ChannelHandler.Sharable
public abstract class MicroService extends SimpleChannelInboundHandler<String> {

    private static final boolean E_POLL = Epoll.isAvailable();
    private static final Logger LOG = LoggerFactory.getLogger(MicroService.class);
    private static final EventLoopGroup EVENT_LOOP_GROUP = E_POLL ? new EpollEventLoopGroup() : new NioEventLoopGroup();

    private static MicroService instance;

    private final Map<UUID, JSONObject> waitingForResponse = new HashMap<>();

    private final Map<List<String>, Tuple<UserEndpoint, Method>> userEndpoints = new HashMap<>();
    private final Map<List<String>, Tuple<MicroServiceEndpoint, Method>> microServiceEndpoints = new HashMap<>();

    private final String name;
    private Channel channel;

    public MicroService(String name) {
        setLoglevel(Level.getLevel(Config.get(DefaultConfig.LOG_LEVEL)));
        this.name = name;

        instance = this;

        final SqlService instance = SqlService.getInstance();
        new Reflections("net.cryptic_game.microservice").getTypesAnnotatedWith(Entity.class).forEach(instance::addEntity);
        instance.start();

        init();
        start();
    }

    private static void setLoglevel(final Level level) {
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        final LoggerConfig loggerConfig = ctx.getConfiguration().getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
        loggerConfig.setLevel(level);
        ctx.updateLoggers();
    }

    public static MicroService getInstance() {
        return instance;
    }

    private void init() {
        try {
            Reflections reflections = new Reflections("net.cryptic_game.microservice", new MethodAnnotationsScanner());

            {
                Set<Method> methods = reflections.getMethodsAnnotatedWith(UserEndpoint.class);
                for (Method method : methods) {
                    UserEndpoint methodEndpoint = method.getAnnotation(UserEndpoint.class);

                    userEndpoints.put(Arrays.asList(methodEndpoint.path()),
                            new Tuple<>(methodEndpoint, method));
                }
            }
            {
                Set<Method> methods = reflections.getMethodsAnnotatedWith(MicroServiceEndpoint.class);
                for (Method method : methods) {
                    MicroServiceEndpoint methodEndpoint = method.getAnnotation(MicroServiceEndpoint.class);

                    microServiceEndpoints.put(Arrays.asList(methodEndpoint.path()),
                            new Tuple<>(methodEndpoint, method));
                }
            }
        } catch (ReflectionsException ignored) {
        }
    }

    private void start() {
        final JSONObject registration = JSONBuilder.anJSON()
                .add("action", "register")
                .add("name", name)
                .build();
        Channel channel = null;

        try {
            channel = new Bootstrap()
                    .group(EVENT_LOOP_GROUP)
                    .channel(E_POLL ? EpollSocketChannel.class : NioSocketChannel.class)
                    .handler(new MicroServiceInitializer(this))
                    .connect(Config.get(DefaultConfig.MSSOCKET_HOST), Config.getInteger(DefaultConfig.MSSOCKET_PORT))
                    .sync().channel();

            this.channel = channel;

            channel.writeAndFlush(registration.toString());
            channel.closeFuture().syncUninterruptibly();

        } catch (Exception e) {
            LOG.warn(e.toString(), e);
        } finally {
            if (channel != null) {
                try {
                    channel.close().syncUninterruptibly();
                } catch (Exception e) {
                    LOG.warn("Unable to close channel: " + e.toString(), e);
                }
            }

            LOG.info("Reconnection in 10 seconds...");

            try {
                Thread.sleep(10000L); // 10 seconds
            } catch (InterruptedException ignored) {
            }

            // reconnect
            start();
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
        new Thread(() -> {

            JSONObject jsonObject;
            try {
                jsonObject = (JSONObject) new JSONParser().parse(msg);
            } catch (ParseException ignored) {
                sendError(channel, UNSUPPORTED_FORMAT);
                return;
            }

            JSON json = new JSON(jsonObject);

            if (json.get("error") != null) {
                return;
            }

            UUID tag = json.getUUID("tag");
            JSONObject data = json.get("data", JSONObject.class);
            JSONArray endpointJSONArray = json.get("endpoint", JSONArray.class);

            if (tag == null || data == null) {
                sendError(channel, MISSING_PARAMETERS);
                return;
            }

            if (waitingForResponse.containsKey(tag) && endpointJSONArray == null) {
                waitingForResponse.replace(tag, data);
                return;
            }

            Object[] endpointArray = endpointJSONArray.toArray();
            List<String> endpoint = Arrays.asList(Arrays.copyOf(endpointArray, endpointArray.length, String[].class));

            UUID user = json.getUUID("user");
            String ms = json.get("ms");

            if (user != null) {
                JSONObject responseData = handleFromUser(endpoint, data, user);

                send(channel, JSONBuilder.anJSON()
                        .add("tag", tag.toString())
                        .add("data", responseData)
                        .build());
            } else if (ms != null && !waitingForResponse.containsKey(tag)) {
                sendToMicroService(ms, handleFromMicroService(endpoint, data, ms), tag);
            } else {
                waitingForResponse.replace(tag, data);
            }
        }).start();

    }

    public JSONObject handleFromUser(List<String> endpoint, JSONObject data, UUID user) {
        if (userEndpoints.containsKey(endpoint)) {
            Tuple<UserEndpoint, Method> tuple = userEndpoints.get(endpoint);

            UserEndpoint userEndpoint = tuple.getA();

            if (checkData(userEndpoint.keys(), userEndpoint.types(), data)) {
                try {
                    JSONObject result = (JSONObject) tuple.getB().invoke(new Object(), new JSON(data), user);

                    if (result == null) {
                        result = new JSONObject();
                    }

                    return result;
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    LOG.error("Error executing endpoint {}.", endpoint, e);
                    return INTERNAL_ERROR.getResponse();
                }
            } else {
                return MISSING_PARAMETERS.getResponse();
            }
        }

        return UNKNOWN_SERVICE.getResponse();
    }

    public JSONObject handleFromMicroService(List<String> endpoint, JSONObject data, String ms) {
        if (microServiceEndpoints.containsKey(endpoint)) {
            Tuple<MicroServiceEndpoint, Method> tuple = microServiceEndpoints.get(endpoint);

            MicroServiceEndpoint microServiceEndpoint = tuple.getA();

            if (checkData(microServiceEndpoint.keys(), microServiceEndpoint.types(), data)) {
                try {
                    JSONObject result = (JSONObject) tuple.getB().invoke(new Object(), new JSON(data), ms);

                    if (result == null) {
                        result = new JSONObject();
                    }

                    return result;
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    LOG.error("Error executing endpoint {}.", endpoint, e);
                    return INTERNAL_ERROR.getResponse();
                }
            } else {
                return MISSING_PARAMETERS.getResponse();
            }
        }

        return UNKNOWN_SERVICE.getResponse();
    }

    public void sendToUser(UUID user, JSONObject data) {
        send(channel, JSONBuilder.anJSON().add("action", "address").add("user", user.toString()).add("data", data).build());
    }

    public void sendToMicroService(String ms, JSONObject data, UUID tag) {
        send(channel, JSONBuilder.anJSON().add("ms", ms).add("data", data).add("tag", tag.toString()).build());
    }

    public JSONObject contactMicroService(String ms, String[] endpoint, JSONObject data) {
        return waitForResponse(JSONBuilder.anJSON()
                .add("ms", ms)
                .add("data", data)
                .add("endpoint", Arrays.asList(endpoint))
                .build());

    }

    public User getUser(UUID user) {
        JSON response = new JSON(waitForResponse(JSONBuilder.anJSON()
                .add("action", "user")
                .add("data", JSONBuilder.anJSON().add("user", user.toString()).build())
                .build()));

        Boolean valid = response.get("valid", Boolean.class);

        if (valid == null || !valid) {
            return null;
        }

        UUID uuid = response.getUUID("uuid");
        String name = response.get("name");
        Long createdTimestamp = response.get("created", Long.class);
        Long lastTimestamp = response.get("last", Long.class);

        if (uuid == null || name == null || createdTimestamp == null || lastTimestamp == null) {
            return null;
        }

        return new User(uuid, name, new Date(createdTimestamp), new Date(lastTimestamp));
    }

    public boolean isValidUser(UUID user) {
        return getUser(user) != null;
    }

    private JSONObject waitForResponse(JSONObject payload) {
        UUID tag = UUID.randomUUID();

        JSONBuilder jsonBuilder = JSONBuilder.anJSON();
        for (Object key : payload.keySet()) {
            jsonBuilder.add(key.toString(), payload.get(key));
        }
        jsonBuilder.add("tag", tag.toString());

        send(channel, jsonBuilder.build());

        waitingForResponse.put(tag, null);

        int counter = 0;
        while (waitingForResponse.get(tag) == null) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            counter++;

            if (counter > 100 * 30) {
                return null;
            }
        }

        return waitingForResponse.remove(tag);
    }

}
