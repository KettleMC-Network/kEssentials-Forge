package net.kettlemc.kessentialsforge.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class TpaService {
    public static class Req {
        public UUID from;
        public UUID to;
        public long expiresAt;
        public boolean here;
        public String fromName;
    }

    private final Map<UUID, List<Req>> incoming = new ConcurrentHashMap<>();

    public void create(UUID from,
                       UUID to,
                       String fromName,
                       boolean here,
                       long nowMs,
                       long ttlMs) {
        Req req = new Req();
        req.from = from;
        req.to = to;
        req.fromName = fromName;
        req.here = here;
        req.expiresAt = nowMs + ttlMs;

        incoming.compute(to, (uuid, existing) -> {
            List<Req> list = existing == null ? new ArrayList<>() : new ArrayList<>(existing);
            removeExpired(list, nowMs);
            list.add(req);
            return list;
        });
    }

    public Req pull(UUID to) {
        return pull(to, null);
    }

    public Req pull(UUID to, UUID from) {
        long now = System.currentTimeMillis();
        AtomicReference<Req> found = new AtomicReference<>();

        incoming.computeIfPresent(to, (uuid, existing) -> {
            List<Req> list = new ArrayList<>();
            for (Req req : existing) {
                if (isExpired(req, now)) {
                    continue;
                }
                if (found.get() == null && (from == null || req.from.equals(from))) {
                    found.set(req);
                    continue;
                }
                list.add(req);
            }
            return list.isEmpty() ? null : list;
        });
        return found.get();
    }

    public Req peek(UUID to) {
        List<Req> list = listFor(to);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<Req> listFor(UUID to) {
        long now = System.currentTimeMillis();
        List<Req> result = new ArrayList<>();
        incoming.computeIfPresent(to, (uuid, existing) -> {
            List<Req> list = new ArrayList<>();
            for (Req req : existing) {
                if (!isExpired(req, now)) {
                    list.add(req);
                }
            }
            if (list.isEmpty()) {
                return null;
            }
            result.addAll(list);
            return list;
        });
        return List.copyOf(result);
    }

    public void tick() {
        long now = System.currentTimeMillis();
        for (UUID key : incoming.keySet()) {
            incoming.compute(key, (uuid, existing) -> {
                if (existing == null) {
                    return null;
                }
                List<Req> list = new ArrayList<>();
                for (Req req : existing) {
                    if (!isExpired(req, now)) {
                        list.add(req);
                    }
                }
                return list.isEmpty() ? null : list;
            });
        }
    }

    private static boolean isExpired(Req req, long now) {
        return req.expiresAt < now;
    }

    private static void removeExpired(List<Req> list, long now) {
        list.removeIf(req -> isExpired(req, now));
    }
}
