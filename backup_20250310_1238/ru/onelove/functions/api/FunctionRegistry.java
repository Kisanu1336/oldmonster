package ru.onelove.functions.api;

import com.google.common.eventbus.Subscribe;
import ru.onelove.onelove;
import ru.onelove.events.EventKey;
import ru.onelove.functions.impl.combat.*;
import ru.onelove.functions.impl.misc.*;
import ru.onelove.functions.impl.movement.*;
import ru.onelove.functions.impl.player.*;
import ru.onelove.functions.impl.render.*;
import ru.onelove.utils.render.font.Font;
import lombok.Getter;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
public class FunctionRegistry {
    private final List<Function> functions = new CopyOnWriteArrayList<>();

    private SwingAnimation swingAnimation;
    private HUD hud;
    private AutoGapple autoGapple;
    private AutoSprint autoSprint;
    private Velocity velocity;
    private NoRender noRender;
    private Timer timer;
    private AutoTool autoTool;
    private xCarry xcarry;
    private ElytraHelper elytrahelper;
    private Phase phase;
    private AutoBuyUI autoBuyUI;
    private ItemSwapFix itemswapfix;
    private AutoPotion autopotion;
    private TriggerBot triggerbot;
    private NoJumpDelay nojumpdelay;
    private ClickFriend clickfriend;
    private InventoryMove inventoryMove;
    private ESP esp;
    private AutoTransfer autoTransfer;
    private GriefHelper griefHelper;
    private ItemCooldown itemCooldown;
    private ClickPearl clickPearl;
    private AutoSwap autoSwap;
    private AutoArmor autoArmor;
    private HitSound hitsound;
    private AntiPush antiPush;
    private FreeCam freeCam;
    private ChestStealer chestStealer;
    private AutoLeave autoLeave;
    private AutoAccept autoAccept;
    private NoEventDelay noEventDelay;
    private AutoRespawn autoRespawn;
    private Fly fly;
    private TargetStrafe targetStrafe;
    private ClientSounds clientSounds;
    private AutoTotem autoTotem;
    private NoSlow noSlow;
    private Pointers pointers;
    private AutoExplosion autoExplosion;
    private NoRotate noRotate;
    private KillAura killAura;
    private AntiBot antiBot;
    private Trails trails;
    private Crosshair crosshair;
    private DeathEffect deathEffect;
    private Strafe strafe;
    private World world;
    private ViewModel viewModel;
    private ElytraFly elytraFly;
    private ChinaHat chinaHat;
    private Snow snow;
    private Particles particles;
    private TargetESP targetESP;
    private JumpCircle jumpCircle;
    private ItemPhysic itemPhysic;
    private Predictions predictions;
    private NoClip noClip;
    private ItemScroller itemScroller;
    private AutoFish autoFish;
    private StorageESP storageESP;
    private Spider spider;
    private NameProtect nameProtect;
    private NoInteract noInteract;
    private GlassHand glassHand;
    private Tracers tracers;
    private SelfDestruct selfDestruct;
    private LeaveTracker leaveTracker;
    private BoatFly boatFly;
    private AntiAFK antiAFK;
    private PortalGodMode portalGodMode;
    private BetterMinecraft betterMinecraft;
    private Backtrack backtrack;
    private SeeInvisibles seeInvisibles;
    private TargetPearl targetPearl;
    private oldaura oldaura;
    private KillAura killaura;
    private CatFly catFly;
    private NoEntityTrace noEntityTrace;
    private Hitbox hitbox;
    private CatStrafe catStrafe;
    private CustomFog customFog;
    private testaura testaura;
    private AutoFirework strafee;
    private ScaffoldAI scaffoldAI;
    private AutoFarm autoFarm;
    private Xray xray;
    private ElytraTarget elytraTarget;

    public void init() {
        registerAll(hud = new HUD(), autoGapple = new AutoGapple(), autoSprint = new AutoSprint(), velocity = new Velocity(), noRender = new NoRender(), autoTool = new AutoTool(), xcarry = new xCarry(), seeInvisibles = new SeeInvisibles(), elytrahelper = new ElytraHelper(), phase = new Phase(), itemswapfix = new ItemSwapFix(), autopotion = new AutoPotion(), noClip = new NoClip(), triggerbot = new TriggerBot(), nojumpdelay = new NoJumpDelay(), clickfriend = new ClickFriend(), inventoryMove = new InventoryMove(), esp = new ESP(), autoTransfer = new AutoTransfer(), griefHelper = new GriefHelper(), autoArmor = new AutoArmor(), hitsound = new HitSound(), antiPush = new AntiPush(), autoBuyUI = new AutoBuyUI(), freeCam = new FreeCam(), chestStealer = new ChestStealer(), autoLeave = new AutoLeave(), autoAccept = new AutoAccept(), autoRespawn = new AutoRespawn(), fly = new Fly(), clientSounds = new ClientSounds(), noSlow = new NoSlow(), pointers = new Pointers(), autoExplosion = new AutoExplosion(), noRotate = new NoRotate(), antiBot = new AntiBot(), trails = new Trails(), crosshair = new Crosshair(), autoTotem = new AutoTotem(), itemCooldown = new ItemCooldown(), clickPearl = new ClickPearl(itemCooldown), autoSwap = new AutoSwap(autoTotem), targetStrafe = new TargetStrafe(killAura), strafe = new Strafe(targetStrafe, killAura), swingAnimation = new SwingAnimation(killAura), world = new World(), viewModel = new ViewModel(), elytraFly = new ElytraFly(), chinaHat = new ChinaHat(), snow = new Snow(), particles = new Particles(), jumpCircle = new JumpCircle(), itemPhysic = new ItemPhysic(), predictions = new Predictions(), itemScroller = new ItemScroller(), autoFish = new AutoFish(), storageESP = new StorageESP(), spider = new Spider(), timer = new Timer(), nameProtect = new NameProtect(), noInteract = new NoInteract(), glassHand = new GlassHand(), tracers = new Tracers(), selfDestruct = new SelfDestruct(), leaveTracker = new LeaveTracker(), antiAFK = new AntiAFK(), portalGodMode = new PortalGodMode(), betterMinecraft = new BetterMinecraft(), backtrack = new Backtrack(), new LongJump(), new XrayBypass(), new Parkour(), new RWHelper(), targetPearl = new TargetPearl(killAura), killAura = new KillAura(), hitbox = new  Hitbox(), noEntityTrace = new NoEntityTrace(), customFog = new CustomFog(), strafee = new AutoFirework(), scaffoldAI = new ScaffoldAI(), autoFarm = new AutoFarm(), xray = new Xray(), targetESP = new TargetESP(killAura), elytraTarget = new ElytraTarget());

        onelove.getInstance().getEventBus().register(this);
    }

    private void registerAll(Function... Functions) {
        Arrays.sort(Functions, Comparator.comparing(Function::getName));

        functions.addAll(List.of(Functions));
    }

    public List<Function> getSorted(Font font, float size) {
        return functions.stream().sorted((f1, f2) -> Float.compare(font.getWidth(f2.getName(), size), font.getWidth(f1.getName(), size))).toList();
    }

    @Subscribe
    private void onKey(EventKey e) {
        if (selfDestruct.unhooked) return;
        for (Function Function : functions) {
            if (Function.getBind() == e.getKey()) {
                Function.toggle();
            }
        }
    }
}
