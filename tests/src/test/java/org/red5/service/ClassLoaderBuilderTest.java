package org.red5.service;

import org.junit.Test;
import org.red5.classloading.ClassLoaderBuilder;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class ClassLoaderBuilderTest {




    @Test
    public void testScrubURLListWithVersions() throws Exception {

        List<URL> urls = new ArrayList<>();
        urls.add(new URL("file:/libs/spring-core-5.3.1.jar"));
        urls.add(new URL("file:/libs/spring-core-5.3.10.jar"));
        urls.add(new URL("file:/libs/log4j-1.2.jar"));
        urls.add(new URL("file:/libs/junit-4.12.jar"));


        ClassLoaderBuilder.scrubURLList(urls);


        assertTrue("La version 5.3.10 doit être conservée.",urls.stream().anyMatch(u -> u.getFile().contains("spring-core-5.3.10.jar")));
        assertFalse("La version 5.3.1 doit être supprimée.",urls.stream().anyMatch(u -> u.getFile().contains("spring-core-5.3.1.jar")));
        assertFalse("Les librairies junit doivent être filtrées.",urls.stream().anyMatch(u -> u.getFile().contains("junit")));
    }

    @Test
    public void testCompareVersionsLogic() {

        assertTrue("1.2.10 est supérieur à 1.2.3", ClassLoaderBuilder.compareVersions("1.2.10", "1.2.3")>0);
        assertEquals("Les versions sont équivalentes",0, ClassLoaderBuilder.compareVersions("2.0", "2.0.0"));
        assertTrue( "1.9 est inférieur à 2.0", ClassLoaderBuilder.compareVersions("1.9", "2.0")<0);
    }

    @Test
    public void testResolveRed5HomeWithSystemProperty() {
        String mockPath = "/tmp/red5_mock";
        System.setProperty("red5.root", mockPath);
        Path home = ClassLoaderBuilder.resolveRed5Home();
        assertEquals("Le chemin Red5 Home doit correspondre à la propriété système.",Paths.get(mockPath), home);
    }

}
