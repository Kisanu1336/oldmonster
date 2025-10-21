package ru.onelove;

import com.google.common.eventbus.EventBus;
import com.jagrosh.discordipc.IPCClient;
import com.jagrosh.discordipc.IPCListener;
import com.jagrosh.discordipc.entities.Packet;
import com.jagrosh.discordipc.entities.RichPresence;
import com.jagrosh.discordipc.entities.pipe.PipeStatus;
import com.jagrosh.discordipc.exceptions.NoDiscordClientException;
import ru.onelove.command.*;
import ru.onelove.command.friends.FriendStorage;
import ru.onelove.command.impl.*;
import ru.onelove.command.impl.feature.*;
import ru.onelove.command.staffs.StaffStorage;
import ru.onelove.config.ConfigStorage;
import ru.onelove.events.EventKey;
import ru.onelove.functions.api.Function;
import ru.onelove.functions.api.FunctionRegistry;
import ru.onelove.scripts.client.ScriptManager;
import ru.onelove.ui.ab.factory.ItemFactory;
import ru.onelove.ui.ab.factory.ItemFactoryImpl;
import ru.onelove.ui.ab.logic.ActivationLogic;
import ru.onelove.ui.ab.model.IItem;
import ru.onelove.ui.ab.model.ItemStorage;
import ru.onelove.ui.ab.render.Window;
import ru.onelove.ui.autobuy.AutoBuyConfig;
import ru.onelove.ui.autobuy.AutoBuyHandler;
import ru.onelove.ui.dropdown.DropDown;
import ru.onelove.ui.mainmenu.AltConfig;
import ru.onelove.ui.mainmenu.AltWidget;
import ru.onelove.ui.styles.Style;
import ru.onelove.ui.styles.StyleFactory;
import ru.onelove.ui.styles.StyleFactoryImpl;
import ru.onelove.ui.styles.StyleManager;
import ru.onelove.utils.TPSCalc;
import ru.onelove.utils.client.ServerTPS;
import ru.onelove.utils.drag.DragManager;
import ru.onelove.utils.drag.Dragging;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.StringTextComponent;
import org.lwjgl.glfw.GLFW;
import via.ViaMCP;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;



@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class onelove {

    public static UserData userData;
    public boolean playerOnServer = false;
    public static final String CLIENT_NAME = "onelove solutions";

    // Экземпляр onelove
    @Getter
    private static onelove instance;

    // Менеджеры
    private FunctionRegistry functionRegistry;
    private ConfigStorage configStorage;
    private CommandDispatcher commandDispatcher;
    private ServerTPS serverTPS;
    private MacroManager macroManager;
    private StyleManager styleManager;

    // Менеджер событий и скриптов
    private final EventBus eventBus = new EventBus();
    private final ScriptManager scriptManager = new ScriptManager();

    // Директории
    private final File clientDir = new File(Minecraft.getInstance().gameDir + "\\onelove");
    private final File filesDir = new File(Minecraft.getInstance().gameDir + "\\onelove\\files");

    // Элементы интерфейса
    private AltWidget altWidget;
    private AltConfig altConfig;
    private DropDown dropDown;
    private Window autoBuyUI;

    // Конфигурация и обработчики
    private AutoBuyConfig autoBuyConfig = new AutoBuyConfig();
    private AutoBuyHandler autoBuyHandler;
    private ViaMCP viaMCP;
    private TPSCalc tpsCalc;
    private ActivationLogic activationLogic;
    private ItemStorage itemStorage;

    public onelove() {
        instance = this;

        if (!clientDir.exists()) {
            clientDir.mkdirs();
        }
        if (!filesDir.exists()) {
            filesDir.mkdirs();
        }

        clientLoad();
        FriendStorage.load();
        StaffStorage.load();
    }



    public Dragging createDrag(Function module, String name, float x, float y) {
        DragManager.draggables.put(name, new Dragging(module, name, x, y));
        return DragManager.draggables.get(name);
    }

    private void clientLoad() {
        viaMCP = new ViaMCP();
        serverTPS = new ServerTPS();
        functionRegistry = new FunctionRegistry();
        macroManager = new MacroManager();
        configStorage = new ConfigStorage();
        functionRegistry.init();
        initCommands();
        initStyles();
        altWidget = new AltWidget();
        altConfig = new AltConfig();
        tpsCalc = new TPSCalc();


        try {
            autoBuyConfig.init();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            altConfig.init();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            configStorage.init();
        } catch (IOException e) {
            System.out.println("Ошибка при подгрузке конфига.");
        }
        try {
            macroManager.init();
        } catch (IOException e) {
            System.out.println("Ошибка при подгрузке конфига макросов.");
        }
        DragManager.load();
        dropDown = new DropDown(new StringTextComponent(""));
        initAutoBuy();
        autoBuyUI = new Window(new StringTextComponent(""), itemStorage);
        //autoBuyUI = new AutoBuyUI(new StringTextComponent("A"));
        autoBuyHandler = new AutoBuyHandler();
        autoBuyConfig = new AutoBuyConfig();

        eventBus.register(this);
    }

    private final EventKey eventKey = new EventKey(-1);

    public void onKeyPressed(int key) {
        if (functionRegistry.getSelfDestruct().unhooked) return;
        eventKey.setKey(key);
        eventBus.post(eventKey);

        macroManager.onKeyPressed(key);

        if (key == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            Minecraft.getInstance().displayGuiScreen(dropDown);
        }
        if (this.functionRegistry.getAutoBuyUI().isState() && this.functionRegistry.getAutoBuyUI().setting.get() == key) {
            Minecraft.getInstance().displayGuiScreen(autoBuyUI);
        }


    }

    private void initAutoBuy() {
        ItemFactory itemFactory = new ItemFactoryImpl();
        CopyOnWriteArrayList<IItem> items = new CopyOnWriteArrayList<>();
        itemStorage = new ItemStorage(items, itemFactory);

        activationLogic = new ActivationLogic(itemStorage, eventBus);
    }

    private void initCommands() {
        Minecraft mc = Minecraft.getInstance();
        Logger logger = new MultiLogger(List.of(new ConsoleLogger(), new MinecraftLogger()));
        List<Command> commands = new ArrayList<>();
        Prefix prefix = new PrefixImpl();
        commands.add(new ListCommand(commands, logger));
        commands.add(new FriendCommand(prefix, logger, mc));
        commands.add(new BindCommand(prefix, logger));
        commands.add(new GPSCommand(prefix, logger));
        commands.add(new ConfigCommand(configStorage, prefix, logger));
        commands.add(new MacroCommand(macroManager, prefix, logger));
        commands.add(new VClipCommand(prefix, logger, mc));
        commands.add(new HClipCommand(prefix, logger, mc));
        commands.add(new StaffCommand(prefix, logger));
        commands.add(new MemoryCommand(logger));
        commands.add(new RCTCommand(logger, mc));

        AdviceCommandFactory adviceCommandFactory = new AdviceCommandFactoryImpl(logger);
        ParametersFactory parametersFactory = new ParametersFactoryImpl();

        commandDispatcher = new StandaloneCommandDispatcher(commands, adviceCommandFactory, prefix, parametersFactory, logger);
    }

    private void initStyles() {
        StyleFactory styleFactory = new StyleFactoryImpl();
        List<Style> styles = new ArrayList<>();

        styles.add(styleFactory.createStyle("Морской", new Color(5, 63, 111), new Color(133, 183, 246)));
        styles.add(styleFactory.createStyle("Малиновый", new Color(109, 10, 40), new Color(239, 96, 136)));
        styles.add(styleFactory.createStyle("Черничный", new Color(78, 5, 127), new Color(193, 140, 234)));
        styles.add(styleFactory.createStyle("Необычный", new Color(243, 160, 232), new Color(171, 250, 243)));
        styles.add(styleFactory.createStyle("Огненный", new Color(194, 21, 0), new Color(255, 197, 0)));
        styles.add(styleFactory.createStyle("Металлический", new Color(40, 39, 39), new Color(178, 178, 178)));
        styles.add(styleFactory.createStyle("Прикольный", new Color(82, 241, 171), new Color(66, 172, 245)));
        styles.add(styleFactory.createStyle("Новогодний", new Color(190, 5, 60), new Color(255, 255, 255)));

     /*   styles.add(styleFactory.createStyle("Mojito", "#1D976C", "#1D976C"));
        styles.add(styleFactory.createStyle("Rose Water", "#E55D87", "#5FC3E4"));
        styles.add(styleFactory.createStyle("Anamnisar", "#9796f0", "#fbc7d4"));
        styles.add(styleFactory.createStyle("Ultra Voilet", "#654ea3", "#eaafc8"));
        styles.add(styleFactory.createStyle("Quepal", "#11998e", "#38ef7d"));
        styles.add(styleFactory.createStyle("Intergalactic", "#5cb8f", "#c657f9"));
        styles.add(styleFactory.createStyle("Blush", "#B24592", "#F15F79"));
        styles.add(styleFactory.createStyle("Back to the Future", "#C02425", "#F0CB35"));
        styles.add(styleFactory.createStyle("Green and Blue", "#52f1ab", "#42acf5"));
        styles.add(styleFactory.createStyle("Sin City Red", "#ED213A", "#93291E"));
        styles.add(styleFactory.createStyle("Evening Night", "#005AA7", "#FFFDE4"));
        styles.add(styleFactory.createStyle("Compare Now", "#EF3B36", "#FFFFFF"));
        styles.add(styleFactory.createStyle("Netflix", "#8E0E00", "#1F1C18"));
        styles.add(styleFactory.createStyle("Passion", "#e53935", "#e35d5b"));*/

        styleManager = new StyleManager(styles, styles.get(0));
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class UserData {
        final String user;
        final int uid;
    }

}
