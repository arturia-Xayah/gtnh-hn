package dev.structureterminal.update;

import static org.junit.Assert.*;

import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class RenamedReleaseCatalogTest {

    private String renamed() {
        return ReleaseCatalogTest.release("0.4.0", "2.9.1", false)
            .replace("gtnh-modernization-upgrade-", "gtnh-hn-");
    }

    @Test
    public void acceptsNewAndHistoricalNamesOnTheCanonicalRepository() throws Exception {
        assertEquals("arturia-Xayah/gtnh-hn", ReleaseCatalog.REPOSITORY);
        for (String json : new String[] { renamed(), ReleaseCatalogTest.release("0.4.0", "2.9.1", false) }) {
            ReleaseCatalog.Release next = ReleaseCatalog.select(json, "0.3.8-alpha", "2.9.1", true, "0.4.0");
            assertNotNull(next);
            assertEquals("0.4.0", next.version);
            assertTrue(next.url.startsWith("https://github.com/arturia-Xayah/gtnh-hn/releases/download/"));
            assertNull(ReleaseCatalog.select(json, "0.3.8-alpha", "2.9.0", true, null));
        }
    }

    @Test
    public void prefersNewAssetRegardlessOfOrderAndFallsBackOnlyToValidLegacyAsset() throws Exception {
        JsonObject oldRelease = new JsonParser().parse(ReleaseCatalogTest.release("0.4.0", "2.9.1", false))
            .getAsJsonObject();
        JsonObject newRelease = new JsonParser().parse(renamed())
            .getAsJsonObject();
        JsonObject oldAsset = oldRelease.getAsJsonArray("assets")
            .get(0)
            .getAsJsonObject();
        JsonObject newAsset = newRelease.getAsJsonArray("assets")
            .get(0)
            .getAsJsonObject();
        for (boolean oldFirst : new boolean[] { true, false }) {
            JsonArray assets = new JsonArray();
            assets.add(oldFirst ? oldAsset : newAsset);
            assets.add(oldFirst ? newAsset : oldAsset);
            oldRelease.add("assets", assets);
            assertTrue(
                ReleaseCatalog.select(oldRelease.toString(), "0.3.8-alpha", "2.9.1", true, null).url
                    .endsWith("/gtnh-hn-0.4.0-GTNH-2.9.1.jar"));
        }
        newAsset.addProperty("digest", "invalid");
        assertTrue(
            ReleaseCatalog.select(oldRelease.toString(), "0.3.8-alpha", "2.9.1", true, null).url
                .endsWith("/gtnh-modernization-upgrade-0.4.0-GTNH-2.9.1.jar"));
    }

    @Test
    public void renamingDoesNotRelaxRepositoryOrExactFilenameChecks() throws Exception {
        for (String json : new String[] { renamed().replace("arturia-Xayah/gtnh-hn/", "arturia-Xayah/another-project/"),
            renamed().replace("gtnh-hn-0.4.0", "gtnh-hn-extra-0.4.0"),
            renamed().replace("https://github.com/", "https://github.com.evil.example/"),
            renamed().replace(".jar\"", ".jar?download=1\"") })
            assertNull(ReleaseCatalog.select(json, "0.3.8-alpha", "2.9.1", true, null));
    }
}
