package me.javavirtualenv.ecology;

import java.util.ArrayList;
import java.util.List;

public final class EcologyHandleRegistry {
	private static final List<EcologyHandle> HANDLES = new ArrayList<>();

	private EcologyHandleRegistry() {
	}

	public static void reset() {
		HANDLES.clear();
	}

	public static void register(EcologyHandle handle) {
		HANDLES.add(handle);
	}

	public static List<EcologyHandle> handles() {
		return List.copyOf(HANDLES);
	}

	public static List<EcologyHandle> resolve(EcologyProfile profile) {
		List<EcologyHandle> resolved = new ArrayList<>();
		for (EcologyHandle handle : HANDLES) {
			if (handle.supports(profile)) {
				resolved.add(handle);
			}
		}
		return resolved;
	}
}
