package me.javavirtualenv.ecology;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EcologyMerge {
	private EcologyMerge() {
	}

	public static Map<String, Object> merge(Map<String, Object> base, List<Map<String, Object>> overlays) {
		Map<String, Object> merged = deepCopy(base);
		for (Map<String, Object> overlay : overlays) {
			mergeInto(merged, overlay);
		}
		return merged;
	}

	public static Map<String, Object> deepCopy(Map<String, Object> source) {
		Map<String, Object> copy = new LinkedHashMap<>();
		for (Map.Entry<String, Object> entry : source.entrySet()) {
			copy.put(entry.getKey(), deepCopyValue(entry.getValue()));
		}
		return copy;
	}

	@SuppressWarnings("unchecked")
	private static void mergeInto(Map<String, Object> target, Map<String, Object> overlay) {
		for (Map.Entry<String, Object> entry : overlay.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			if (value == null) {
				continue;
			}
			if (value instanceof List<?> list) {
				if (list.isEmpty()) {
					continue;
				}
				target.put(key, deepCopyValue(list));
				continue;
			}
			Object existing = target.get(key);
			if (value instanceof Map<?, ?> mapValue && existing instanceof Map<?, ?> mapExisting) {
				mergeInto((Map<String, Object>) mapExisting, (Map<String, Object>) mapValue);
				continue;
			}
			target.put(key, deepCopyValue(value));
		}
	}

	private static Object deepCopyValue(Object value) {
		if (value instanceof Map<?, ?> map) {
			@SuppressWarnings("unchecked")
			Map<String, Object> cast = (Map<String, Object>) map;
			return deepCopy(cast);
		}
		if (value instanceof List<?> list) {
			List<Object> copy = new ArrayList<>(list.size());
			for (Object entry : list) {
				copy.add(deepCopyValue(entry));
			}
			return copy;
		}
		return value;
	}
}
