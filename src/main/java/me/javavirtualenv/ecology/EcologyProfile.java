package me.javavirtualenv.ecology;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class EcologyProfile {
	private final ResourceLocation id;
	private final Map<String, Object> root;
	private final Map<String, Object> cache = new HashMap<>();
	private final Map<String, String[]> pathCache = new HashMap<>();

	public EcologyProfile(ResourceLocation id, Map<String, Object> root) {
		this.id = id;
		this.root = root;
	}

	public ResourceLocation id() {
		return id;
	}

	public Map<String, Object> root() {
		return root;
	}

	@Nullable
	public Object get(String path) {
		return resolvePath(path);
	}

	/**
	 * Fast path access for common two-level paths like "internal_state.hunger.max_value"
	 * Avoids array allocation by using direct map access pattern.
	 */
	@Nullable
	public Object getFast(String level1, String level2) {
		Object current = root.get(level1);
		if (!(current instanceof Map<?, ?> map)) {
			return null;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> cast = (Map<String, Object>) map;
		return cast.get(level2);
	}

	/**
	 * Fast path access for common three-level paths.
	 * Avoids array allocation and iteration overhead.
	 */
	@Nullable
	public Object getFast(String level1, String level2, String level3) {
		Object current = root.get(level1);
		if (!(current instanceof Map<?, ?> map1)) {
			return null;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> cast1 = (Map<String, Object>) map1;
		current = cast1.get(level2);
		if (!(current instanceof Map<?, ?> map2)) {
			return null;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> cast2 = (Map<String, Object>) map2;
		return cast2.get(level3);
	}

	@Nullable
	public Map<String, Object> getMap(String path) {
		Object value = resolvePath(path);
		if (value instanceof Map<?, ?> map) {
			@SuppressWarnings("unchecked")
			Map<String, Object> cast = (Map<String, Object>) map;
			return cast;
		}
		return null;
	}

	@Nullable
	public List<?> getList(String path) {
		Object value = resolvePath(path);
		if (value instanceof List<?> list) {
			return list;
		}
		return null;
	}

	public List<Map<String, Object>> getMapList(String path) {
		List<?> list = getList(path);
		if (list == null) {
			return List.of();
		}
		List<Map<String, Object>> out = new ArrayList<>();
		for (Object entry : list) {
			if (entry instanceof Map<?, ?> map) {
				@SuppressWarnings("unchecked")
				Map<String, Object> cast = (Map<String, Object>) map;
				out.add(cast);
			}
		}
		return out;
	}

	public List<String> getStringList(String path) {
		List<?> list = getList(path);
		if (list == null) {
			return List.of();
		}
		List<String> out = new ArrayList<>();
		for (Object entry : list) {
			if (entry instanceof String value) {
				out.add(value);
			}
		}
		return out;
	}

	public String getString(String path, String fallback) {
		Object value = resolvePath(path);
		if (value instanceof String string) {
			return string;
		}
		return fallback;
	}

	public int getInt(String path, int fallback) {
		Object value = resolvePath(path);
		if (value instanceof Number number) {
			return number.intValue();
		}
		if (value instanceof String string) {
			try {
				return Integer.parseInt(string);
			} catch (NumberFormatException ignored) {
			}
		}
		return fallback;
	}

	public double getDouble(String path, double fallback) {
		Object value = resolvePath(path);
		if (value instanceof Number number) {
			return number.doubleValue();
		}
		if (value instanceof String string) {
			try {
				return Double.parseDouble(string);
			} catch (NumberFormatException ignored) {
			}
		}
		return fallback;
	}

	public boolean getBool(String path, boolean fallback) {
		Object value = resolvePath(path);
		if (value instanceof Boolean bool) {
			return bool;
		}
		if (value instanceof String string) {
			return Boolean.parseBoolean(string);
		}
		return fallback;
	}

	/**
	 * Fast typed accessors for common internal_state paths.
	 * These avoid repeated path parsing and type checking.
	 */
	public int getIntFast(String section, String key, int fallback) {
		Object value = getFast("internal_state", section, key);
		if (value instanceof Number number) {
			return number.intValue();
		}
		return fallback;
	}

	public double getDoubleFast(String section, String key, double fallback) {
		Object value = getFast("internal_state", section, key);
		if (value instanceof Number number) {
			return number.doubleValue();
		}
		return fallback;
	}

	public boolean getBoolFast(String section, String key, boolean fallback) {
		Object value = getFast("internal_state", section, key);
		if (value instanceof Boolean bool) {
			return bool;
		}
		return fallback;
	}

	public <T> T cached(String key, Supplier<T> supplier) {
		@SuppressWarnings("unchecked")
		T cached = (T) cache.get(key);
		if (cached != null) {
			return cached;
		}
		T value = supplier.get();
		cache.put(key, value);
		return value;
	}

	@Nullable
	private Object resolvePath(String path) {
		// Use cached path segments to avoid repeated split() calls
		String[] parts = pathCache.computeIfAbsent(path, k -> k.split("\\."));
		Object current = root;
		for (String part : parts) {
			if (!(current instanceof Map<?, ?> map)) {
				return null;
			}
			@SuppressWarnings("unchecked")
			Map<String, Object> cast = (Map<String, Object>) map;
			current = cast.get(part);
			if (current == null) {
				return null;
			}
		}
		return current;
	}
}
