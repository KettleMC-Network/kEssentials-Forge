package net.kettlemc.kessentialsforge.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class HomeServiceLoadTest {
    @TempDir
    Path tempDir;

    @Test
    void loadMaterializesHomes() throws IOException {
        UUID id = UUID.randomUUID();
        String json = "{\"" + id + "\":{\"Main\":{\"dim\":\"minecraft:overworld\",\"x\":1.5,\"y\":70.0,\"z\":-20.25,\"yaw\":90.0,\"pitch\":-10.0}}}";
        Files.writeString(tempDir.resolve("homes.json"), json);

        HomeService service = new HomeService(tempDir);
        service.load();

        HomeService.Home home = service.getHome(id, "MAIN");
        assertNotNull(home);
        assertEquals("minecraft:overworld", home.dim);
        assertEquals(1.5, home.x);
        assertEquals(70.0, home.y, 1e-6);
        assertEquals(-20.25, home.z);
        assertEquals(90.0f, home.yaw);
        assertEquals(-10.0f, home.pitch);
        assertTrue(service.list(id).contains("main"));
    }
}
