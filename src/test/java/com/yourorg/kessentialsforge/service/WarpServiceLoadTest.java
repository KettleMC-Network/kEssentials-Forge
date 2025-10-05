package net.kettlemc.kessentialsforge.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class WarpServiceLoadTest {
    @TempDir
    Path tempDir;

    @Test
    void loadMaterializesWarps() throws IOException {
        String json = "{\"Spawn\":{\"dim\":\"minecraft:the_nether\",\"x\":10.0,\"y\":65.0,\"z\":-5.5,\"yaw\":180.0,\"pitch\":0.0}}";
        Files.writeString(tempDir.resolve("warps.json"), json);

        WarpService service = new WarpService(tempDir);
        service.load();

        WarpService.Warp warp = service.get("SPAWN");
        assertNotNull(warp);
        assertEquals("minecraft:the_nether", warp.dim);
        assertEquals(10.0, warp.x);
        assertEquals(65.0, warp.y, 1e-6);
        assertEquals(-5.5, warp.z);
        assertEquals(180.0f, warp.yaw);
        assertEquals(0.0f, warp.pitch);
        assertTrue(service.list().contains("spawn"));
    }
}
