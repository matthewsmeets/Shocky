package pl.shockah.shocky;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.pircbotx.PircBotX;

public abstract class Module extends ListenerAdapter {
	private static final List<Module> modules = Collections.synchronizedList(new ArrayList<Module>()), modulesOn = Collections.synchronizedList(new ArrayList<Module>());
	private static final List<ModuleLoader> loaders = Collections.synchronizedList(new ArrayList<ModuleLoader>());
	
	public static void registerModuleLoader(ModuleLoader loader) {
		if (loaders.contains(loader)) loaders.remove(loader);
		loaders.add(loader);
	}
	public static void unregisterModuleLoader(ModuleLoader loader) {
		if (loaders.contains(loader)) {
			loader.unloadAllModules();
			loaders.remove(loader);
		}
	}
	
	private static void setup(Module module, ModuleLoader loader, ModuleSource source) {
		module.loader = loader;
		module.source = source;
	}
	public static Module load(ModuleSource source) {
		Module module = null;
		for (int i = 0; i < loaders.size(); i++) {
			if (loaders.get(i).accept(source)) module = loaders.get(i).loadModule(source);
			if (module != null) {
				setup(module,loaders.get(i),source);
				break;
			}
		}
		
		if (module != null) {
			for (int i = 0; i < modules.size(); i++) if (modules.get(i).name().equals(module.name())) {
				module.loader.unloadModule(module);
				return null;
			}
			
			modules.add(module);
			Data.config.setNotExists("module-"+module.name(),true);
			if (Data.config.getBoolean("module-"+module.name())) {
				module.onEnable();
				modulesOn.add(module);
			}
		}
		return module;
	}
	public static boolean unload(Module module) {
		if (module == null) return false;
		if (!modules.contains(module)) return false;
		if (modulesOn.contains(module)) {
			module.onDisable();
			modulesOn.remove(module);
		}
		modules.remove(module);
		module.loader.unloadModule(module);
		return true;
	}
	public static boolean reload(Module module) {
		if (module == null) return false;
		ModuleSource src = module.source;
		unload(module);
		return load(src) != null;
	}
	
	public static boolean enable(Module module) {
		if (module == null) return false;
		if (modulesOn.contains(module)) return false;
		module.onEnable();
		modulesOn.add(module);
		return true;
	}
	public static boolean disable(Module module) {
		if (!modulesOn.contains(module)) return false;
		module.onDisable();
		modulesOn.remove(module);
		return true;
	}
	
	public static ArrayList<Module> loadNewModules() {
		ArrayList<Module> ret = new ArrayList<Module>();
		File dir = new File("modules"); dir.mkdir();
		for (File f : dir.listFiles()) {
			if (f.isDirectory()) continue;
			if (f.getName().contains("$")) continue;
			if (!f.getName().endsWith(".class")) continue;
			if (!f.getName().startsWith("Module")) continue;
			Module m = load(new ModuleSource.File(f));
			if (m != null) ret.add(m);
		}
		return ret;
	}
	
	public static Module getModule(String name) {
		for (int i = 0; i < modules.size(); i++) if (modules.get(i).name().equals(name)) return modules.get(i);
		return null;
	}
	public static ArrayList<Module> getModules() {
		return new ArrayList<Module>(modules);
	}
	public static ArrayList<Module> getModules(boolean enabled) {
		ArrayList<Module> ret = getModules();
		for (int i = 0; i < ret.size(); i++) if (modulesOn.contains(ret.get(i)) != enabled) ret.remove(i--);
		return ret;
	}
	
	private ModuleLoader loader;
	private ModuleSource source;
	
	public abstract String name();
	
	public void onEnable() {}
	public void onDisable() {}
	public void onDie(PircBotX bot) {}
	public void onDataSave() {}
	
	public final boolean isEnabled() {
		return modulesOn.contains(this);
	}
}