package com.nexora.client.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ModuleSettings {
    public enum Type { NUMBER, BOOLEAN, CHOICE }

    public static final class Setting {
        private final String id;
        private final String label;
        private final Type type;
        private double numberValue;
        private boolean booleanValue;
        private int choiceIndex;
        private final double min;
        private final double max;
        private final double step;
        private final String[] choices;

        private Setting(String id, String label, double value, double min, double max, double step) {
            this.id = id;
            this.label = label;
            this.type = Type.NUMBER;
            this.numberValue = value;
            this.min = min;
            this.max = max;
            this.step = step;
            this.choices = new String[0];
        }

        private Setting(String id, String label, boolean value) {
            this.id = id;
            this.label = label;
            this.type = Type.BOOLEAN;
            this.booleanValue = value;
            this.min = 0;
            this.max = 1;
            this.step = 1;
            this.choices = new String[0];
        }

        private Setting(String id, String label, String value, String[] choices) {
            this.id = id;
            this.label = label;
            this.type = Type.CHOICE;
            this.choices = choices.clone();
            int index = 0;
            for (int i = 0; i < choices.length; i++) {
                if (choices[i].equalsIgnoreCase(value)) {
                    index = i;
                    break;
                }
            }
            this.choiceIndex = index;
            this.min = 0;
            this.max = Math.max(0, choices.length - 1);
            this.step = 1;
        }

        public String id() { return id; }
        public String label() { return label; }
        public Type type() { return type; }
        public double number() { return numberValue; }
        public boolean bool() { return booleanValue; }
        public String choice() { return choices.length == 0 ? "" : choices[choiceIndex]; }
        public double min() { return min; }
        public double max() { return max; }
        public double step() { return step; }

        public void adjust(int direction) {
            if (type != Type.NUMBER) return;
            numberValue = clamp(round(numberValue + step * direction), min, max);
        }

        public void toggle() {
            if (type == Type.BOOLEAN) booleanValue = !booleanValue;
        }

        public void cycle(int direction) {
            if (type != Type.CHOICE || choices.length == 0) return;
            choiceIndex = Math.floorMod(choiceIndex + direction, choices.length);
        }

        private static double round(double value) {
            return Math.round(value * 1000.0) / 1000.0;
        }

        private static double clamp(double value, double min, double max) {
            return Math.max(min, Math.min(max, value));
        }
    }

    private final Map<String, List<Setting>> settings = new LinkedHashMap<>();

    public ModuleSettings() {
        defaults();
    }

    public List<Setting> forModule(String module) {
        return List.copyOf(settings.getOrDefault(key(module), List.of()));
    }

    public Setting get(String module, String id) {
        for (Setting setting : settings.getOrDefault(key(module), List.of())) {
            if (setting.id.equalsIgnoreCase(id)) return setting;
        }
        return null;
    }

    public double number(String module, String id, double fallback) {
        Setting setting = get(module, id);
        return setting != null && setting.type == Type.NUMBER ? setting.numberValue : fallback;
    }

    public boolean bool(String module, String id, boolean fallback) {
        Setting setting = get(module, id);
        return setting != null && setting.type == Type.BOOLEAN ? setting.booleanValue : fallback;
    }

    public String choice(String module, String id, String fallback) {
        Setting setting = get(module, id);
        return setting != null && setting.type == Type.CHOICE ? setting.choice() : fallback;
    }

    private void number(String module, String id, String label, double value, double min, double max, double step) {
        add(module, new Setting(id, label, value, min, max, step));
    }

    private void bool(String module, String id, String label, boolean value) {
        add(module, new Setting(id, label, value));
    }

    private void choice(String module, String id, String label, String value, String... options) {
        add(module, new Setting(id, label, value, options));
    }

    private void add(String module, Setting setting) {
        settings.computeIfAbsent(key(module), ignored -> new ArrayList<>()).add(setting);
    }

    private String key(String module) {
        return module.toLowerCase(Locale.ROOT);
    }

    private void defaults() {
        number("TriggerBot","cooldown","Cooldown",0.92,0.10,1.00,0.05);
        number("TriggerBot","range","Max Range",4.5,2.0,6.0,0.25);
        bool("TriggerBot","players","Players",true);
        bool("TriggerBot","mobs","Mobs",true);

        number("CrystalESP","range","Range",96,16,256,8);

        number("AimAssist","range","Range",8.0,2.0,12.0,0.5);
        number("AimAssist","strength","Turn Strength",0.35,0.05,1.0,0.05);
        bool("AimAssist","players","Players",true);
        bool("AimAssist","mobs","Mobs",true);

        number("AutoClicker","interval","Interval Ticks",4,1,20,1);
        number("AutoClicker","cooldown","Cooldown",0.80,0.10,1.0,0.05);
        number("AutoShield","range","Threat Range",6.0,2.0,10.0,0.5);
        number("CriticalJump","height","Jump Height",0.18,0.05,0.60,0.05);
        number("AutoSwing","interval","Interval Ticks",8,1,40,1);
        number("KillAura","range","Range",4.5,2.0,6.0,0.25);
        number("KillAura","cooldown","Cooldown",0.92,0.10,1.0,0.05);
        bool("KillAura","players","Players",true);
        bool("KillAura","mobs","Mobs",true);
        number("Reach","range","Reach",6.0,3.0,8.0,0.25);
        number("Reach","radius","Aim Radius",1.35,0.25,2.5,0.10);
        number("Reach","cooldown","Cooldown",0.80,0.10,1.0,0.05);
        number("Velocity","horizontal","Horizontal %",25,0,100,5);
        number("Velocity","vertical","Vertical %",35,0,100,5);

        number("Fly","speed","Fly Speed",0.10,0.05,1.0,0.05);
        bool("Sprint","forwardOnly","Forward Only",true);
        bool("AutoWalk","sprint","Sprint Too",false);
        bool("AutoSneak","pulse","Pulse",false);
        number("Speed","multiplier","Multiplier",1.45,1.0,3.0,0.10);
        number("BunnyHop","jump","Jump Power",0.42,0.30,0.80,0.02);
        number("Freecam","speed","Camera Speed",0.65,0.10,3.0,0.10);
        number("HighJump","power","Jump Power",0.72,0.42,1.50,0.05);
        number("AirJump","power","Jump Power",0.42,0.20,1.00,0.02);
        number("FastFall","speed","Fall Speed",0.30,0.10,1.0,0.05);
        number("Glide","fall","Fall Cap",0.08,0.01,0.30,0.01);
        number("Spider","speed","Climb Speed",0.25,0.05,0.80,0.05);
        number("WaterSpeed","multiplier","Multiplier",1.06,1.0,2.0,0.05);
        number("LongJump","boost","Forward Boost",0.72,0.20,1.50,0.05);
        number("LongJump","jump","Jump Power",0.42,0.20,0.80,0.02);
        number("Jetpack","thrust","Upward Thrust",0.28,0.05,0.80,0.05);
        number("SlowFall","fall","Fall Cap",0.03,0.01,0.20,0.01);
        number("ReverseStep","speed","Down Speed",0.70,0.20,2.0,0.10);
        number("StrafeBoost","speed","Boost Speed",0.31,0.10,1.0,0.05);
        number("NoSlow","multiplier","Movement Boost",1.35,1.0,2.0,0.05);
        number("SafeWalk","probe","Edge Probe",2.2,0.5,4.0,0.10);
        number("Jesus","lift","Surface Lift",0.08,0.02,0.30,0.02);
        number("Jesus","speed","Water Boost",1.08,1.0,1.8,0.05);
        number("Parkour","lookahead","Look Ahead",0.8,0.3,2.0,0.10);
        bool("Phase","noclip","NoClip",true);
        number("Step","height","Step Impulse",0.46,0.20,1.0,0.05);
        number("VehicleFly","horizontal","Horizontal",0.55,0.10,1.5,0.05);
        number("VehicleFly","vertical","Vertical",0.35,0.10,1.0,0.05);
        number("AntiVoid","threshold","Bottom Offset",5,1,20,1);
        number("AntiVoid","boost","Recovery Boost",1.0,0.2,2.0,0.10);

        number("AutoRespawn","delay","Delay Ticks",0,0,40,1);
        number("SpinBot","speed","Yaw / Tick",14,1,60,1);
        number("PitchLock","pitch","Pitch",0,-90,90,5);
        number("YawLock","step","Snap Degrees",45,15,90,15);
        number("AutoDrop","interval","Interval Ticks",20,1,100,1);
        bool("AutoDrop","fullStack","Drop Stack",false);
        number("HandSwing","interval","Interval Ticks",10,1,60,1);
        bool("KeepSprint","forwardOnly","Forward Only",true);
        number("NoFall","threshold","Fall Distance",2.5,1.0,6.0,0.25);
        number("FastPlace","interval","Interval Ticks",1,1,10,1);
        number("FastBreak","interval","Interval Ticks",1,1,10,1);
        number("Nuker","radius","Radius",2,1,5,1);
        number("Nuker","delay","Delay Ticks",1,1,20,1);
        choice("AutoTool","mode","Mode","Fastest","Fastest","Prefer Sword","Prefer Pickaxe");

        number("ESP","range","Range",160,32,256,16);
        bool("ESP","players","Players",true);
        bool("ESP","mobs","Mobs",true);
        bool("ESP","boxes","Boxes",true);
        bool("ESP","labels","Name + Distance",true);
        number("ItemESP","range","Range",96,16,256,8);
        number("Tracers","range","Range",160,32,256,16);
        number("Breadcrumbs","points","Trail Points",140,20,400,20);
        number("Fullbright","gamma","Gamma",1.0,0.5,1.0,0.05);
        number("XRay","range","Range",32,8,64,8);
        number("XRay","vertical","Vertical",32,8,48,4);
        number("Zoom","fov","FOV",30,10,70,5);
        number("PlayerESP","range","Range",160,32,256,16);
        number("MobESP","range","Range",160,32,256,16);
        number("BoxESP","range","Range",160,32,256,16);
        number("NameTags","range","Range",160,32,256,16);
        number("GlowESP","range","Range",160,32,256,16);

        number("Waypoints","range","Max Distance",5000,64,20000,64);

        bool("HUD","coordinates","Coordinates",true);
        bool("HUD","modules","Active Modules",true);
        number("HUD","opacity","GUI Opacity",175,90,235,10);
        number("Radar","rows","Rows",8,4,16,1);
        bool("AutoMine","onlyTarget","Only With Target",false);
        number("AutoUse","interval","Interval Ticks",1,1,10,1);
        number("AntiAFK","interval","Interval Ticks",100,20,600,20);
        number("AntiAFK","turn","Turn Degrees",3,1,45,1);

        number("SneakSpam","interval","Interval Ticks",5,1,20,1);
        number("JumpSpam","interval","Interval Ticks",10,1,40,1);
        number("UseSpam","interval","Interval Ticks",4,1,20,1);
        number("MineSpam","interval","Interval Ticks",4,1,20,1);
        number("QuickTurn","interval","Interval Ticks",60,10,300,10);
        number("QuickTurn","degrees","Turn Degrees",180,45,360,15);

        number("BaseFinder","range","Scan Range",32,8,64,8);
        number("BaseFinder","vertical","Vertical",32,8,48,4);
        number("BaseFinder","minCluster","Min Cluster",3,2,20,1);
        number("BaseFinder","clusterRadius","Cluster Radius",16,6,32,2);
        number("BaseFinder","delay","Update Delay",20,10,100,5);
        number("StorageESP","range","Range",32,8,64,8);
        number("BlockESP","range","Range",24,8,64,8);

        // Every module also gets common presentation/testing controls.
        for (String module : List.copyOf(settings.keySet())) {
            String original = module;
            bool(original,"showHud","Show in HUD",true);
            bool(original,"notify","Toggle Message",true);
        }
    }
}
